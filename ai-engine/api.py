"""Authenticated HTTP API for underwriter-triggered AI assessments."""

from __future__ import annotations

import logging
import os

import firebase_admin
from dotenv import load_dotenv
from fastapi import Depends, FastAPI, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from firebase_admin import auth as firebase_auth
from google.cloud import firestore

from al_ai import firestore_adapter as fs
from al_ai.committee import run_committee
from al_ai.config import ROOT, load_config, with_threshold_overrides
from al_ai.llm_clients import LLMRouter

load_dotenv(ROOT / ".env")
logger = logging.getLogger(__name__)

app = FastAPI(title="Agentic Lender AI")
default_origins = "http://localhost:5173,http://127.0.0.1:5173"
allowed_origins = [
    origin.strip()
    for origin in os.getenv("WEB_APP_ORIGINS", default_origins).split(",")
    if origin.strip()
]
app.add_middleware(
    CORSMiddleware,
    allow_origins=allowed_origins,
    allow_credentials=False,
    allow_methods=["POST"],
    allow_headers=["Authorization", "Content-Type"],
)


class AnalysisInProgress(Exception):
    pass


class AlreadyAssessed(Exception):
    pass


class MissingConsent(Exception):
    pass


def authenticated_user(authorization: str | None = Header(default=None)) -> dict:
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="A Firebase ID token is required.")

    try:
        firebase_admin.get_app()
    except ValueError:
        try:
            firebase_admin.initialize_app(options={
                "projectId": os.getenv("FIREBASE_PROJECT_ID") or "benchodlender"
            })
        except Exception as exc:
            logger.exception("Could not initialize Firebase Admin SDK")
            raise HTTPException(
                status_code=503,
                detail="Firebase Admin could not initialize. Check GOOGLE_APPLICATION_CREDENTIALS and FIREBASE_PROJECT_ID.",
            ) from exc

    try:
        return firebase_auth.verify_id_token(authorization.removeprefix("Bearer "))
    except Exception as exc:
        logger.warning("Firebase ID token verification failed (%s): %s", type(exc).__name__, exc)
        raise HTTPException(status_code=401, detail="The Firebase ID token could not be verified.") from exc


def claim_application_for_analysis(application_ref, started_by: str) -> dict:
    transaction = fs.db().transaction()

    @firestore.transactional
    def claim(transaction):
        snapshot = application_ref.get(transaction=transaction)
        if not snapshot.exists:
            raise HTTPException(status_code=404, detail="Application not found.")

        application = snapshot.to_dict() or {}
        consent = application.get("consent")
        if not isinstance(consent, dict) or consent.get("aiAnalysis") is not True:
            raise MissingConsent

        current_assessment = application.get("aiAssessment")
        if isinstance(current_assessment, dict):
            status = current_assessment.get("status")
            if status == "running":
                raise AnalysisInProgress
            if status == "assessed":
                raise AlreadyAssessed

        transaction.update(application_ref, {
            "aiAssessment": {
                "status": "running",
                "startedBy": started_by,
                "startedAt": firestore.SERVER_TIMESTAMP,
            }
        })
        return application

    return claim(transaction)


@app.post("/applications/{application_id}/analysis")
def start_analysis(application_id: str, user: dict = Depends(authenticated_user)) -> dict:
    application_ref = fs.db().collection("applications").document(application_id)
    try:
        application_data = claim_application_for_analysis(application_ref, user["uid"])
    except MissingConsent as exc:
        raise HTTPException(
            status_code=400,
            detail="The applicant has not consented to AI-assisted analysis.",
        ) from exc
    except AnalysisInProgress as exc:
        raise HTTPException(status_code=409, detail="An AI assessment is already running.") from exc
    except AlreadyAssessed as exc:
        raise HTTPException(status_code=409, detail="This application already has an AI assessment.") from exc

    config = load_config()
    try:
        application = fs.to_application(application_id, application_data)
        config = with_threshold_overrides(
            config,
            fs.load_threshold_overrides(config),
            "Firestore risk settings",
        )
        result = run_committee(application, config, LLMRouter.from_config(config))
        assessment_id = fs.save_assessment(application_id, result, config)
        return {"assessmentId": assessment_id, "summary": result.summary}
    except Exception as exc:
        logger.exception("AI assessment failed for application %s", application_id)
        try:
            fs.save_status(application_id, "error", f"{type(exc).__name__}: {exc}", config)
        except Exception:
            logger.exception("Could not save AI assessment failure for application %s", application_id)
        raise HTTPException(status_code=500, detail="The AI assessment failed. Check the AI engine logs.") from exc


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        app,
        host=os.getenv("AI_API_HOST", "127.0.0.1"),
        port=int(os.getenv("AI_API_PORT", "8000")),
    )
