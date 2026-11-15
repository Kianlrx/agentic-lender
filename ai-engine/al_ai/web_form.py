# Turns the web app's mortgage form (applications/{id}.applicationData) into an Application.
# ALL web form field names live in this file (see web-app/src/MortgageApplication.jsx).
# Personal details (PPSN, date of birth, email, phone, address) are never read.

from __future__ import annotations

import re

from al_ai.models import Application

COUNTIES = ["Carlow", "Cavan", "Clare", "Cork", "Donegal", "Dublin", "Galway", "Kerry", "Kildare", "Kilkenny",
            "Laois", "Leitrim", "Limerick", "Longford", "Louth", "Mayo", "Meath", "Monaghan", "Offaly",
            "Roscommon", "Sligo", "Tipperary", "Waterford", "Westmeath", "Wexford", "Wicklow"]
DUBLIN_EIRCODE = re.compile(r"^(D\d{2}|D6W)")             # D01-D24 and D6W routing keys are Dublin
# form property type -> valuation model type
PROPERTY_TYPES = {"House": "semi-detached", "Apartment": "apartment", "Duplex": "apartment"}
REPLACES_MORTGAGE = {"Switch mortgage", "Remortgage"}    # existing mortgage is paid off by the new one


# "250000" -> 250000.0, "" or bad text -> None
def num(value) -> float | None:
    try:
        return float(value) if value not in (None, "") else None
    except (TypeError, ValueError):
        return None


# finds the county in the address, or Dublin from the Eircode
def county_of(address: str, eircode: str) -> str | None:
    for county in COUNTIES:
        if re.search(rf"\b{county}\b", address or "", re.IGNORECASE):
            return county
    if DUBLIN_EIRCODE.match((eircode or "").replace(" ", "").upper()):
        return "Dublin"
    return None


# basic salary plus overtime/bonus/commission marked Regular, in EUR only
def yearly_income(applicant: dict, label: str, notes: list[str]) -> float | None:
    if applicant.get("incomeCurrency", "EUR") != "EUR":
        notes.append(f"{label} income is not in EUR: not converted, so not counted")
        return None
    salary = num(applicant.get("grossAnnualSalary"))
    if salary is None:
        return None
    variable = sum(num(applicant.get(k)) or 0 for k in ("overtime", "bonus", "commission"))
    if variable and applicant.get("variablePayRegularity") == "Regular":
        salary += variable
        notes.append(f"{label}: regular overtime/bonus/commission counted as yearly amounts")
    elif variable:
        notes.append(f"{label}: overtime/bonus/commission not counted (not marked Regular)")
    if any(num(item.get("amount")) for item in applicant.get("otherIncome", [])):
        notes.append(f"{label}: other/rental income not counted (form has no frequency for it)")
    return salary


def employment_type(applicant: dict) -> str | None:
    category = applicant.get("employmentCategory")
    if category == "Employed":
        return "salaried" if applicant.get("contractType") == "Permanent" else "temporary"
    return category.lower() if category else None


# monthly commitments that stay after the mortgage starts
def monthly_debts(form: dict, card_pct: float, notes: list[str]) -> list[dict]:
    debts = []
    for applicant in form.get("applicants", []):
        for loan in applicant.get("loans", []):
            if loan.get("refinance") == "Yes":
                notes.append("A loan marked 'to be refinanced' is not counted as a debt")
            elif num(loan.get("repayment")):
                debts.append({"type": "loan", "monthly_repayment": num(loan["repayment"])})
        for card in applicant.get("creditCards", []):
            if num(card.get("limit")):
                debts.append({"type": "credit card", "monthly_repayment": round(num(card["limit"]) * card_pct, 2)})
        if num(applicant.get("maintenanceAndChildcare")):
            debts.append({"type": "maintenance/childcare",
                          "monthly_repayment": num(applicant["maintenanceAndChildcare"])})
    if any(num(card.get("limit")) for a in form.get("applicants", []) for card in a.get("creditCards", [])):
        notes.append(f"Credit cards counted at {card_pct:.0%} of the limit per month")
    for prop in form.get("existingProperties", []):
        if form.get("applicationType") in REPLACES_MORTGAGE:
            notes.append("Existing mortgage repayment not counted: this application replaces it")
        elif num(prop.get("monthlyRepayment")):
            debts.append({"type": "existing mortgage", "monthly_repayment": num(prop["monthlyRepayment"])})
    return debts


# serious declarations a human must look at
def red_flags(form: dict) -> list[str]:
    flags = []
    if form.get("insolvency") == "Yes":
        flags.append("Insolvency or judgements declared")
    if any(p.get("forbearance") == "Yes" for p in form.get("existingProperties", [])):
        flags.append("Forbearance arrangement on an existing mortgage")
    if form.get("propertyUse") in ("Buy-to-let", "Holiday home") or form.get("propertyStatus") == "Buy-to-let":
        flags.append("Not a home to live in: buy-to-let/holiday-home rules are not modelled")
    if form.get("propertyStatus") == "Self-build":
        flags.append("Self-build: stage payments are not modelled")
    return flags


# applications/{id} document -> Application
def from_web_form(app_id: str, doc: dict, card_pct: float = 0.03) -> Application:
    form = doc.get("applicationData") or {}
    applicants = form.get("applicants") or [{}]
    main, co = applicants[0], (applicants[1] if len(applicants) > 1 else None)
    notes: list[str] = []
    missing: list[str] = []

    main_income = yearly_income(main, "Applicant 1", notes)
    co_income = yearly_income(co, "Applicant 2", notes) if co else None
    if main_income is None:
        missing.append("applicationData.applicants[0].grossAnnualSalary")

    # first-time buyer isn't asked: only "no" can be told from the form
    owns_home = bool(form.get("existingProperties")) or any(a.get("accommodation") == "Owner-occupier"
                                                            for a in applicants)
    if not owns_home:
        notes.append("First-time buyer not asked on the form: stricter second-time-buyer LTI limit used")

    price, loan = num(form.get("purchasePrice")), num(form.get("loanAmount"))
    funds = form.get("funds", [])
    gifts = [f for f in funds if "gift" in (f.get("source") or "").lower()]
    joint = form.get("applicationKind") == "Joint applicants"

    prop: dict = {"asking_price": price}
    if form.get("propertyFound") == "Yes":
        county = county_of(form.get("propertyAddress", ""), form.get("eircode", ""))
        prop_type = PROPERTY_TYPES.get(form.get("propertyType"))
        if not county:
            missing.append("county (not found in propertyAddress or eircode)")
        if form.get("propertyType") and not prop_type:
            notes.append(f"Property type '{form['propertyType']}' cannot be valued by the model")
        if form.get("propertyType") == "House":
            notes.append("Property type 'House' valued as semi-detached (form has no house style)")
        prop |= {"property_type": prop_type, "floor_area_m2": num(form.get("floorArea")),
                 "bedrooms": num(form.get("bedrooms")), "year_built": num(form.get("yearBuilt")),
                 "ber_rating": (form.get("ber") or "").strip().upper() or None, "county": county}
    else:
        notes.append("No property found yet (Approval in Principle): valuation skipped")

    consent = (doc.get("consent") or {}).get("aiAnalysis", form.get("declarations", {}).get("aiAnalysis"))
    name = (main.get("legalName") or doc.get("borrower") or "").split()
    debts = monthly_debts(form, card_pct, notes)
    app = Application(
        application_id=app_id,
        loan_type="heloc" if form.get("applicationType") == "Mortgage top-up" else "mortgage",
        status=doc.get("status"),
        ai_consent=consent,
        borrower={"first_name": name[0] if name else None,
                  "household_status": "couple" if joint or main.get("maritalStatus") in
                  ("Married", "Civil partnership") else "single",
                  "dependants": int(max(num(a.get("dependantsCount")) or 0 for a in applicants)),
                  "first_time_buyer": False if owns_home else None,
                  "joint_application": joint},
        income={"employment_type": employment_type(main), "yearly": main_income,
                "co_applicant_yearly": co_income},
        debts=debts,
        no_existing_debts=not debts,
        deposit={"amount": max(price - loan, 0) if price is not None and loan is not None else None,
                 "gifted_amount": sum(num(f.get("amount")) or 0 for f in gifts),
                 "gift_source": ", ".join(f["donorRelationship"] for f in gifts if f.get("donorRelationship"))
                 or None},
        loan={"amount": loan, "term_years": num(form.get("loanTermYears")),
              "rate_type": form.get("rateType") or None, "repayment_type": form.get("repaymentType") or None},
        property={k: (int(v) if k in ("bedrooms", "year_built") and v is not None else v)
                  for k, v in prop.items()},
    )
    if loan is None:
        missing.append("applicationData.loanAmount")
    app.missing_fields = missing
    app.notes = notes
    app.flags = red_flags(form)
    return app
