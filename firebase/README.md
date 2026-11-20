# Firebase setup

The web and Android clients are configured for the same Firebase project:
`benchodlender`. The Android `google-services.json` package name is
`com.example.agenticlender`. Firebase client configuration identifies a
project; it is not an Admin SDK credential.

## One-time console setup

1. Sign in to the Firebase Console for `benchodlender`.
2. Create/enable Cloud Firestore in Native mode.
3. Create/enable the Cloud Storage bucket used by the web application.
4. In Authentication, enable the Email/Password sign-in provider.
5. Ensure the Android app is registered as `com.example.agenticlender` and
   download its `google-services.json` to `mobile-app/app/`.
6. Install the Firebase CLI, sign in, and select the `benchodlender` project
   if it is not already selected:

   ```powershell
   npm.cmd install --global firebase-tools
   firebase login
   firebase use benchodlender
   ```

## Deploy Firebase rules

From the repository root:

```powershell
firebase deploy --only firestore:rules,firestore:indexes,storage
```

The root `firebase.json` points to the rules, index, and Storage rules files in
this folder. The current client queries do not require composite indexes.

## Data collections used today

- `users/{uid}`: a mobile user's profile; only that authenticated user can
  access their own document.
- `chats/{uid}/messages/{messageId}`: a mobile user's messages; only that
  authenticated user can read or add messages to their own chat.
- `applications/{applicationId}`: web application records. Temporarily, any
  signed-in user can read, update, and delete every record; creating a record
  still requires `createdBy` to match that user's UID. Mortgage applications
  store the step responses in `applicationData` and document metadata in
  `documents`.
- `applications/{applicationId}/aiAssessments/{assessmentId}`: any signed-in
  user can read the detailed agent outputs for the prototype application view.
  Client writes are denied; the AI engine writes with its Admin SDK.
- `mortgageApplications/{uid}/{applicationId}/{fileName}`: uploaded mortgage
  documents. Storage rules restrict uploads, reads, and deletes to the
  authenticated owner, and allow only PDF, JPEG, or PNG files up to 15 MB.
- `settings/riskThresholds`: the AI engine accesses this using the Firebase
  Admin SDK. The service account bypasses client rules. No client access is
  granted by these rules.

The mortgage form collects AI-analysis consent and income, debt, deposit,
property, and other details under `applicationData`. The AI engine maps these
(`ai-engine/al_ai/web_form.py`) and, through the Admin SDK, adds an
`aiAssessment` summary to the application. Under the temporary broad update
rule, any signed-in user can also change that field. The engine refuses to
analyze a record without
`consent.aiAnalysis == true`; see [the AI engine README](../ai-engine/README.md).

## Security limitations

Application access is deliberately broad for this prototype: any authenticated
account can read, edit, or delete every application, including sensitive
borrower data and AI assessments. This is not suitable for real applicant data.
Uploaded documents in Storage remain owner-restricted. Before using real data,
replace this temporary policy with trusted applicant, broker, and underwriter
roles and test the rules in the Firebase Emulator Suite. Never place a
service-account JSON file in the repository; the root `.gitignore` excludes
common service-account names.
