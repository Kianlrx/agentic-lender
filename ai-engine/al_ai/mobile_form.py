# Turns the mobile app's application (flat fields on applications/{uid}) into an Application.
# ALL mobile field names live in this file (see mobile-app Application.kt on the mobile-app-sprint2 branch).
# Personal details (last name, date of birth, email, phone, home address, signature) are never read.

from __future__ import annotations

from al_ai.models import Application
from al_ai.web_form import county_of, num

PROPERTY_TYPES = {"Detached House": "detached", "Semi-Detached": "semi-detached", "Terraced": "terraced",
                  "Apartment / Flat": "apartment", "Duplex": "apartment"}
EMPLOYMENT = {"Full-time Employed": "salaried", "Part-time Employed": "salaried", "Self-employed": "self-employed",
              "Contractor / Temporary": "temporary", "Retired": "retired", "Unemployed": "unemployed"}
NOT_FIRST_TIME = {"Move / Next Home Purchase", "Refinance / Switcher", "Equity Release"}


# True if the document looks like it came from the mobile app
def is_mobile(doc: dict) -> bool:
    return "consent_aiAnalysis" in doc or "grossAnnualIncome" in doc


# "10 Years" -> 10.0
def years(value) -> float | None:
    return num(str(value).split()[0]) if value not in (None, "") else None


def red_flags(doc: dict) -> list[str]:
    flags = []
    if doc.get("occupancyType") in ("Second / Holiday Home", "Investment / Buy-to-Let"):
        flags.append("Not a home to live in: buy-to-let/holiday-home rules are not modelled")
    if doc.get("propertyType") == "Land / Site":
        flags.append("Land / site: not a finished home, cannot be valued")
    return flags


# applications/{uid} document -> Application
def from_mobile(app_id: str, doc: dict, card_pct: float = 0.03) -> Application:
    heloc = doc.get("applicationType") == "HELOC"
    notes = ["Mobile app does not collect BER, floor area, bedrooms or year built: valuation and green "
             "discount cannot be checked"]
    missing: list[str] = []

    # first-time buyer: from the loan purpose, or "homeowner" in household status
    purpose = doc.get("loanPurpose") or ""
    owns = purpose in NOT_FIRST_TIME or heloc or "homeowner" in (doc.get("householdStatus") or "").lower()
    ftb = True if purpose == "First-Time Home Purchase" and not owns else False if owns else None

    if num(doc.get("additionalIncome")):
        notes.append("Additional bonus/overtime income not counted (regularity not collected)")
    if doc.get("isJointApplication"):
        notes.append("Joint application: co-applicant income is not collected, only applicant 1's income counted")

    debts = []
    if num(doc.get("creditCardLimit")):
        debts.append({"type": "credit card", "monthly_repayment": round(num(doc["creditCardLimit"]) * card_pct, 2)})
        notes.append(f"Credit cards counted at {card_pct:.0%} of the total limit per month")
    if num(doc.get("existingLoanRepayment")):
        debts.append({"type": "loan", "monthly_repayment": num(doc["existingLoanRepayment"])})
    if num(doc.get("otherMonthlyCommitments")):
        debts.append({"type": "other commitments", "monthly_repayment": num(doc["otherMonthlyCommitments"])})
    # rent stops when the new mortgage starts; a HELOC sits on top of the existing mortgage
    if num(doc.get("rentOrMortgage")):
        if heloc:
            debts.append({"type": "existing mortgage", "monthly_repayment": num(doc["rentOrMortgage"])})
        else:
            notes.append("Current rent/mortgage not counted: replaced by the new mortgage")

    loan = num(doc.get("requestedCreditLimit")) if heloc else num(doc.get("loanAmount"))
    term = years(doc.get("drawPeriodYears")) if heloc else num(doc.get("loanTermYears"))
    value = num(doc.get("estimatedHomeValue")) if heloc else num(doc.get("purchasePrice"))
    income = num(doc.get("grossAnnualIncome"))
    for name, v in (("grossAnnualIncome", income), ("requestedCreditLimit" if heloc else "loanAmount", loan),
                    ("estimatedHomeValue" if heloc else "purchasePrice", value)):
        if v is None:
            missing.append(name)

    gift = doc.get("depositSource") == "Gift from Family"
    deposit = num(doc.get("depositAmount"))
    if doc.get("propertyType") == "Duplex":
        notes.append("Duplex valued as an apartment")
    name = (doc.get("firstName") or "").split()

    app = Application(
        application_id=app_id,
        loan_type="heloc" if heloc else "mortgage",
        status=doc.get("status"),
        ai_consent=doc.get("consent_aiAnalysis"),
        borrower={"first_name": name[0] if name else None,
                  "household_status": "couple" if doc.get("isJointApplication") else "single",
                  "dependants": int(num(doc.get("dependants")) or 0),
                  "first_time_buyer": ftb,
                  "joint_application": bool(doc.get("isJointApplication"))},
        income={"employment_type": EMPLOYMENT.get(doc.get("employmentType"), (doc.get("employmentType") or None)),
                "yearly": income},
        debts=debts,
        no_existing_debts=not debts,
        deposit={"amount": deposit, "gifted_amount": (deposit or 0) if gift else 0.0,
                 "gift_source": "Family" if gift else None},
        loan={"amount": loan, "term_years": term, "rate_type": doc.get("rateType") or None,
              "existing_mortgage_balance": num(doc.get("existingFirstMortgageBalance")) if heloc else None},
        property={"asking_price": value, "property_type": PROPERTY_TYPES.get(doc.get("propertyType")),
                  "county": county_of(doc.get("propertyAddress", ""), "")},
    )
    app.missing_fields = missing
    app.notes = notes
    app.flags = red_flags(doc)
    return app
