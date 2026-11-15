# End-to-end tests against the local Firestore emulator (skipped when it isn't running).
# Start it from the repo root:
#   npx firebase-tools emulators:start --only firestore --project demo-agentic-lender
# then: $env:FIRESTORE_EMULATOR_HOST="127.0.0.1:8080"; python -m pytest tests/test_watch_emulator.py

import copy
import json
import os
import subprocess
import sys
import time
import uuid

import pytest

pytestmark = pytest.mark.skipif(not os.getenv("FIRESTORE_EMULATOR_HOST"), reason="Firestore emulator not running")

from al_ai import firestore_adapter as fs  # noqa: E402
from al_ai.committee import assessment_hash  # noqa: E402
from al_ai.config import ROOT, load_config  # noqa: E402
from al_ai.llm_clients import LLMRouter  # noqa: E402
from al_ai.models import CommitteeResult  # noqa: E402
import watch  # noqa: E402

SAMPLE = json.loads((ROOT / "samples" / "web_application.json").read_text(encoding="utf-8"))
CFG = load_config()


# writes a document like the web app's submit does, returns its ID
def submit(**changes) -> str:
    from google.cloud import firestore
    doc = {k: v for k, v in copy.deepcopy(SAMPLE).items() if not k.startswith("_") and k != "applicationId"}
    doc |= changes | {"timestamp": firestore.SERVER_TIMESTAMP}
    app_id = f"test-{uuid.uuid4().hex[:8]}"
    fs.db().collection("applications").document(app_id).set(doc)
    return app_id


def stored(app_id: str) -> dict:
    return fs.db().collection("applications").document(app_id).get().to_dict()


@pytest.fixture(scope="module")
def llm():
    return LLMRouter.from_config(CFG, disabled=True)


def test_assess_writes_summary_and_full_result(llm):
    app_id = submit()
    assert watch.assess(app_id, CFG, llm).startswith("APPROVE_WITH_CONDITIONS")
    ai = stored(app_id)["aiAssessment"]
    assert ai["status"] == "assessed" and ai["aprBasisPoints"] == 340 and ai["greenDiscountBps"] == 25
    assert ai["assessedAt"] is not None and len(ai["assessment_hash"]) == 64

    # the hash can be recomputed from the stored full result (ledger verifyAudit)
    full = (fs.db().collection("applications").document(app_id)
            .collection("aiAssessments").document(ai["assessmentId"]).get().to_dict())
    assert assessment_hash(CommitteeResult(**full)) == ai["assessment_hash"]

    # the application itself is untouched apart from aiAssessment
    assert stored(app_id)["status"] == "Applied" and stored(app_id)["borrower"] == "Niamh Example"


def test_already_assessed_is_skipped(llm):
    app_id = submit()
    watch.assess(app_id, CFG, llm)
    first = stored(app_id)["aiAssessment"]["assessmentId"]
    assert watch.assess(app_id, CFG, llm) == "skipped"
    assert stored(app_id)["aiAssessment"]["assessmentId"] == first


def test_no_consent_is_not_assessed(llm):
    app_id = submit(consent={"aiAnalysis": False})
    assert watch.assess(app_id, CFG, llm) == "no consent"
    ai = stored(app_id)["aiAssessment"]
    assert ai["status"] == "not_assessed" and "aprBasisPoints" not in ai
    assert not list(fs.db().collection("applications").document(app_id).collection("aiAssessments").stream())


def test_listener_assesses_new_submission():
    proc = subprocess.Popen([sys.executable, "watch.py", "--no-llm"], cwd=ROOT,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding="utf-8")
    try:
        time.sleep(3)                      # let the listener attach
        app_id = submit()
        for _ in range(60):
            if "aiAssessment" in stored(app_id):
                break
            time.sleep(0.5)
        assert stored(app_id)["aiAssessment"]["aprBasisPoints"] == 340
    finally:
        proc.terminate()
        output = proc.communicate(timeout=10)[0]
    assert f"{app_id}: APPROVE_WITH_CONDITIONS" in output
