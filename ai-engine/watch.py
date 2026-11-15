# Watches Firestore and runs the AI committee on every newly submitted application.
# python watch.py          -> keeps listening until Ctrl+C
# python watch.py --once   -> assesses waiting applications, then stops
# Run only one watcher at a time.

from __future__ import annotations

import argparse
import queue
import sys

from dotenv import load_dotenv
from google.cloud.firestore_v1.base_query import FieldFilter

from al_ai import firestore_adapter as fs
from al_ai.committee import run_committee
from al_ai.config import ROOT, load_config, with_threshold_overrides
from al_ai.llm_clients import LLMRouter

SUBMITTED = "Applied"   # status the web app sets on submit


# True if the document still needs an assessment
def is_waiting(doc: dict) -> bool:
    return doc.get("status") == SUBMITTED and "aiAssessment" not in doc


# assesses one application and saves the result; returns what happened
def assess(app_id: str, cfg: dict, llm: LLMRouter) -> str:
    coll = cfg["firestore"]["applications_collection"]
    snap = fs.db().collection(coll).document(app_id).get()
    if not snap.exists or not is_waiting(snap.to_dict() or {}):
        return "skipped"                                   # deleted, or already assessed
    try:
        app = fs.to_application(app_id, snap.to_dict(), cfg["pricing"]["credit_card_pct_of_limit"])
        if app.ai_consent is not True:
            fs.save_status(app_id, "not_assessed", "No consent to AI-assisted analysis", cfg)
            return "no consent"
        run_cfg = with_threshold_overrides(cfg, fs.load_threshold_overrides(cfg), "Firestore risk settings")
        result = run_committee(app, run_cfg, llm)
        fs.save_assessment(app_id, result, cfg)
        s = result.summary
        return f"{s['recommendation']}, risk {s['risk_rating']}, APR {s['apr_basis_points']} bps"
    except Exception as exc:   # one bad application doesn't stop the watcher
        fs.save_status(app_id, "error", f"{type(exc).__name__}: {exc}", cfg)
        return f"error: {exc}"


def waiting_query(cfg: dict):
    coll = fs.db().collection(cfg["firestore"]["applications_collection"])
    return coll.where(filter=FieldFilter("status", "==", SUBMITTED))


def main() -> int:
    sys.stdout.reconfigure(encoding="utf-8")
    load_dotenv(ROOT / ".env")
    p = argparse.ArgumentParser(description="Assess new Firestore applications automatically.")
    p.add_argument("--once", action="store_true", help="assess waiting applications, then stop")
    p.add_argument("--no-llm", action="store_true", help="numbers-only mode: skip all LLMs")
    args = p.parse_args()

    cfg = load_config()
    llm = LLMRouter.from_config(cfg, disabled=args.no_llm)
    print(f"LLMs: {', '.join(f'{p}/{m}' for p, m in llm.active) or 'none (numbers only)'}")

    if args.once:
        for snap in waiting_query(cfg).stream():
            if is_waiting(snap.to_dict() or {}):
                print(f"{snap.id}: {assess(snap.id, cfg, llm)}", flush=True)
        return 0

    # the listener runs on a background thread, so it only queues IDs; the work happens here
    todo: queue.Queue[str] = queue.Queue()

    def on_snapshot(col_snapshot, changes, read_time):
        for change in changes:
            if change.type.name in ("ADDED", "MODIFIED") and is_waiting(change.document.to_dict() or {}):
                todo.put(change.document.id)

    watch = waiting_query(cfg).on_snapshot(on_snapshot)
    print("Watching for submitted applications. Ctrl+C to stop.", flush=True)
    try:
        while True:
            try:
                app_id = todo.get(timeout=1)    # timeout so Ctrl+C works on Windows
            except queue.Empty:
                continue
            print(f"{app_id}: assessing...", flush=True)
            try:
                print(f"{app_id}: {assess(app_id, cfg, llm)}", flush=True)
            except Exception as exc:   # e.g. Firestore offline: keep watching
                print(f"{app_id}: could not save: {exc}", flush=True)
    except KeyboardInterrupt:
        print("Stopped.")
    finally:
        watch.unsubscribe()
    return 0


if __name__ == "__main__":
    sys.exit(main())
