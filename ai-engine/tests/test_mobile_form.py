# Tests for mobile_form.py (the Android app's flat application fields).

import copy
import json

import pytest

from al_ai.committee import run_committee
from al_ai.config import ROOT, load_config
from al_ai.firestore_adapter import to_application
from al_ai.llm_clients import LLMRouter
from al_ai.mobile_form import from_mobile, is_mobile, years

SAMPLE = json.loads((ROOT / "samples" / "mobile_application.json").read_text(encoding="utf-8"))


@pytest.fixture
def doc():
    return copy.deepcopy(SAMPLE)


def run(doc):
    cfg = load_config()
    return run_committee(from_mobile("test-id", doc), cfg, LLMRouter.from_config(cfg, disabled=True))


def test_detected_and_routed(doc):
    assert is_mobile(doc) and not is_mobile({"borrower": "x", "loanRequested": "1"})
    assert to_application("x", doc).income.yearly == 72_000


def test_maps_sample(doc):
    app = from_mobile("test-id", doc)
    assert app.borrower.first_name == "Sean" and app.borrower.first_time_buyer is True
    assert app.income.yearly == 72_000 and app.income.employment_type == "salaried"
    assert app.loan.amount == 240_000 and app.loan.term_years == 30
    assert app.deposit.amount == 60_000 and app.deposit.gifted_amount == 0
    assert app.property.asking_price == 300_000 and app.property.county == "Cork"
    assert app.property.property_type == "semi-detached" and app.ai_consent is True


def test_debts_leave_out_rent(doc):
    debts = {d.type: d.monthly_repayment for d in from_mobile("x", doc).debts}
    assert debts == {"credit card": 90, "loan": 200}


def test_never_reads_personal_details(doc):
    dumped = from_mobile("x", doc).model_dump_json()
    for secret in ("Example", "01/02/1992", "sean@example.com", "0870000002", "Sample Avenue"):
        assert secret not in dumped


def test_gift_deposit(doc):
    doc["depositSource"] = "Gift from Family"
    dep = from_mobile("x", doc).deposit
    assert dep.gifted_amount == 60_000 and dep.gift_source == "Family"


def test_first_time_buyer_from_purpose(doc):
    doc["loanPurpose"] = "Move / Next Home Purchase"
    assert from_mobile("x", doc).borrower.first_time_buyer is False
    doc["loanPurpose"] = ""
    assert from_mobile("x", doc).borrower.first_time_buyer is None


def test_years():
    assert years("10 Years") == 10 and years("") is None


def test_full_run_prices_on_purchase_price(doc):
    result = run(doc)
    pricing = result.agents[-1].result["pricing"]
    assert pricing["ltv_basis"] == "purchase price LTV (no valuation)"
    assert result.summary["apr_basis_points"] == 365                   # 80% LTV band, BER unknown
    assert result.summary["recommendation"] == "REFER"                 # no valuation possible


def test_heloc_priced_on_combined_ltv(doc):
    doc |= {"applicationType": "HELOC", "requestedCreditLimit": "50000", "existingFirstMortgageBalance": "150000",
            "estimatedHomeValue": "400000", "drawPeriodYears": "10 Years", "rentOrMortgage": "900"}
    app = from_mobile("x", doc)
    assert app.loan_type == "heloc" and app.loan.amount == 50_000 and app.loan.term_years == 10
    assert {"type": "existing mortgage", "monthly_repayment": 900} in [d.model_dump() for d in app.debts]
    pricing = run(doc).agents[-1].result["pricing"]
    assert pricing["ltv_used"] == 0.5 and pricing["apr_basis_points"] == 345   # (150k + 50k) / 400k


def test_buy_to_let_is_flagged(doc):
    doc["occupancyType"] = "Investment / Buy-to-Let"
    assert run(doc).summary["recommendation"] == "REFER"
