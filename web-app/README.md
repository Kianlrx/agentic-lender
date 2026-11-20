# Web application

React 18 and Vite prototype for Firebase email/password authentication and a
shared loan-application pipeline.

## Requirements

- Node.js and npm
- Firebase project configured as described in [Firebase setup](../firebase/README.md)

## Run locally (PowerShell)

```powershell
cd web-app
npm.cmd install
npm.cmd run dev
```

Open the localhost URL printed by Vite (normally `http://localhost:5173/`).
`npm.cmd` invokes the Windows command shim and works when PowerShell script
execution policy blocks `npm.ps1`.

## Underwriter application review

Selecting an application from the pipeline or search opens its review page.
The page shows the submitted application, agent assessment details, and
underwriter approve/deny actions. **Start AI Analysis** calls the AI engine's
authenticated API; it does not depend on the Firestore listener in
`ai-engine/watch.py`.

Run the API separately as described in [the AI engine README](../ai-engine/README.md).
The local API URL defaults to `http://127.0.0.1:8000`; set `VITE_AI_API_URL`
at web-app build time when hosting it elsewhere. Deploy the updated Firestore
rules to let signed-in users read agent assessment documents.

Approve records `status: "Approved"` and deny records `status: "Declined"` in
Firestore. Neither action currently triggers the ledger. Account roles are
not implemented yet, so any signed-in user can currently access the review
page and trigger analysis.

## Build and preview

```powershell
npm.cmd run build
npm.cmd run preview
```

## Firebase and application behavior

The Firebase web configuration is in `src/firebase.js`; it targets the
`benchodlender` project shared with the Android app. Enable Email/Password
Authentication and deploy the repository Firestore rules before trying
sign-in or Firestore operations. Setup and the rules' security limitations are
documented in [the Firebase README](../firebase/README.md).

The application form writes records to `applications` with flat borrower and
loan fields plus the full form in `applicationData`. The web app shows the
entire collection to every signed-in user. Firestore rules temporarily allow
every signed-in user to read, edit, and delete any application; creating one
still requires `createdBy` to match the submitting user's UID. This broad
access is only for the prototype and is not suitable for real borrower data.
See [Firebase setup](../firebase/README.md) before deploying rules or using
real applicant data.

The form collects underwriting details and AI-analysis consent in
`applicationData` and `consent`. The AI engine can read these applications
from the shared Firestore collection when configured.