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

The application form writes flat records to `applications` with `borrower`,
`applicantEmail`, `phone`, `location`, `loanRequested`, `status`, `timestamp`,
and `createdBy`. Every signed-in user can currently read, edit, and delete all
pipeline records. This is demo behavior, not suitable for real borrower data.

The form does not collect underwriting details or explicit AI-analysis
consent. It is not currently connected to the AI engine.