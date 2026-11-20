# Tests for web_form.py and the APR in a full (numbers-only) committee run.

import copy
import json

import pytest

from al_ai.committee import run_committee
from al_ai.config import ROOT, load_config
from al_ai.llm_clients import LLMRouter
from al_ai.web_form import county_of, from_web_form

SAMPLE = json.loads((ROOT / "samples" / "web_application.json").read_text(encoding="utf-8"))


@pytest.fixture
def doc():
    return copy.deepcopy(SAMPLE)


def form(doc):
    return doc["applicationData"]


def run(doc):
    cfg = load_config()
    return run_committee(from_web_form("test-id", doc), cfg, LLMRouter.from_config(cfg, disabled=True))


# mapping

def test_maps_sample(doc):
    app = from_web_form("test-id", doc)
    assert app.borrower.first_name == "Niamh"
    assert app.borrower.joint_application and app.borrower.household_status == "couple"
    assert app.borrower.dependants == 1
    assert app.income.yearly == 58_000                 # 55k salary + 3k regular bonus
    assert app.income.co_applicant_yearly == 45_000    # occasional overtime not counted
    assert app.income.employment_type == "salaried"
    assert app.loan.amount == 300_000 and app.loan.term_years == 30
    assert app.deposit.amount == 80_000 and app.deposit.gifted_amount == 20_000
    assert app.deposit.gift_source == "Parents"
    assert app.property.county == "Galway" and app.property.property_type == "semi-detached"
    assert app.property.ber_rating == "B2" and app.property.bedrooms == 3
    assert app.ai_consent is True and app.flags == []


def test_debts_include_loans_cards_and_childcare(doc):
    debts = {d.type: d.monthly_repayment for d in from_web_form("x", doc).debts}
    assert debts == {"loan": 250, "credit card": 60, "maintenance/childcare": 400}


def test_never_reads_personal_details(doc):
    dumped = from_web_form("x", doc).model_dump_json()
    for secret in ("0000000TW", "1994-03-02", "niamh@example.com", "0870000000", "Example Park", "Example"):
        assert secret not in dumped


def test_refinanced_loan_not_counted(doc):
    form(doc)["applicants"][0]["loans"][0]["refinance"] = "Yes"
    assert "loan" not in {d.type for d in from_web_form("x", doc).debts}


def test_switch_does_not_count_the_mortgage_being_replaced(doc):
    form(doc)["existingProperties"] = [{"address": "a", "mortgageBalance": "1", "monthlyRepayment": "900",
                                        "forbearance": "No"}]
    assert "existing mortgage" in {d.type for d in from_web_form("x", doc).debts}
    form(doc)["applicationType"] = "Switch mortgage"
    app = from_web_form("x", doc)
    assert "existing mortgage" not in {d.type for d in app.debts}
    assert app.borrower.first_time_buyer is False


def test_non_euro_income_is_not_counted(doc):
    form(doc)["applicants"][0]["incomeCurrency"] = "GBP"
    app = from_web_form("x", doc)
    assert app.income.yearly is None
    assert "applicationData.applicants[0].grossAnnualSalary" in app.missing_fields


def test_consent_comes_from_the_document(doc):
    doc["consent"] = {"aiAnalysis": False}
    assert from_web_form("x", doc).ai_consent is False


@pytest.mark.parametrize("address, eircode, county", [
    ("1 Main St, Co. Cork", "T12 AB12", "Cork"),
    ("Flat 2, Ranelagh", "D06 XY12", "Dublin"),
    ("Apartment 4, Main Street", "D6W AB12", "Dublin"),
    ("Main Street", "H91 AB12", None),
])
def test_county_of(address, eircode, county):
    assert county_of(address, eircode) == county


# full run: APR and green discount

def test_green_apr_in_summary_and_hash(doc):
    result = run(doc)
    s = result.summary
    assert s["ltv"] == pytest.approx(300_000 / 380_000, abs=1e-4)  # estimate is above asking
    assert s["apr_basis_points"] == 340 and s["apr_pct"] == 3.40  # 365 band - 25 green
    assert s["green_discount_bps"] == 25 and s["green_mortgage_eligible"] is True
    assert s["recommendation"] == "APPROVE_WITH_CONDITIONS" and s["risk_rating"] == "LOW"
    assert len(result.assessment_hash) == 64                     # fits the ledger's bytes32


def test_no_green_discount_for_d1(doc):
    form(doc)["ber"] = "D1"
    s = run(doc).summary
    assert s["green_discount_bps"] == 0 and s["apr_basis_points"] in (365, 385)


def test_exempt_or_pending_ber_is_not_sent_to_valuation_model(doc):
    form(doc)["ber"] = "Exempt / Pending"
    result = run(doc)
    valuation = next(agent for agent in result.agents if agent.agent == "Valuation Agent")
    green = result.agents[-1].result["green_mortgage"]

    assert from_web_form("x", doc).property.ber_rating is None
    assert valuation.status == "UNAVAILABLE"
    assert any("ber_rating" in warning for warning in valuation.warnings)
    assert green["eligible"] is None


def test_red_flag_forces_refer(doc):
    form(doc)["insolvency"] = "Yes"
    s = run(doc).summary
    assert s["recommendation"] == "REFER"
    assert "Insolvency or judgements declared" in s["notes"]


def test_no_property_yet_priced_on_purchase_price(doc):
    form(doc)["propertyFound"] = "No"
    result = run(doc)
    pricing = result.agents[-1].result["pricing"]
    assert pricing["ltv_basis"] == "purchase price LTV (no valuation)"
    assert pricing["apr_basis_points"] == 365                    # 79% band, BER unknown so no discount
    assert result.summary["recommendation"] == "REFER"           # valuation unavailable


def test_ltv_above_rate_card_has_no_apr(doc):
    form(doc)["loanAmount"] = "360000"                             # 95% LTV
    result = run(doc)
    assert result.summary["apr_basis_points"] is None
    assert any("No APR" in w for w in result.agents[-1].warnings)
