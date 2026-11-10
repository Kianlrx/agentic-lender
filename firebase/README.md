# Firebase setup

The web and Android clients are configured for the same Firebase project:
`benchodlender`. The Android `google-services.json` package name is
`com.example.agenticlender`. Firebase client configuration identifies a
project; it is not an Admin SDK credential.

## One-time console setup

1. Sign in to the Firebase Console for `benchodlender`.
2. Create/enable Cloud Firestore in Native mode.
3. In Authentication, enable the Email/Password sign-in provider.
4. Ensure the Android app is registered as `com.example.agenticlender` and
   download its `google-services.json` to `mobile-app/app/`.
5. Install the Firebase CLI, sign in, and select the `benchodlender` project
   if it is not already selected:

   ```powershell
   npm.cmd install --global firebase-tools
   firebase login
   firebase use benchodlender
   ```

## Deploy Firestore rules

From the repository root:

```powershell
firebase deploy --only firestore:rules,firestore:indexes
```

The root `firebase.json` points to the rules and index files in this folder.
There are no required composite indexes for the current client queries.

## Data collections used today

- `users/{uid}`: a mobile user's profile; only that authenticated user can
  access their own document.
- `chats/{uid}/messages/{messageId}`: a mobile user's messages; only that
  authenticated user can read or add messages to their own chat.
- `applications/{applicationId}`: web application records. The current web
  UI is a **shared staff prototype**: every authenticated account can read,
  update, and delete every record, while new records must have
  `createdBy == request.auth.uid`. The rules validate the web form's current
  flat document shape and prevent clients from changing creation metadata.
- `applications/{applicationId}/aiAssessments/{assessmentId}` and
  `settings/riskThresholds`: the AI engine accesses these using the Firebase
  Admin SDK. The service account bypasses client rules. No client access is
  granted by these rules.

The web form does not currently request AI-analysis consent or collect the
income, debts, deposit, property, and other fields required by the AI engine.
The AI engine intentionally refuses to analyze a record without
`consent.aiAnalysis == true`; see [the AI engine README](../ai-engine/README.md).

## Security limitations

These are demo rules, not a production access model. The shared `applications`
permissions expose sensitive borrower/contact information to every signed-in
account and allow any such account to alter or permanently delete records.
Before using real applicant data, add trusted role assignment (for example
custom claims set by a trusted server), separate applicant and staff access,
restrict application reads/updates/deletes to authorized staff, and test the
rules in the Firebase Emulator Suite. Never place a service-account JSON file
in the repository; the root `.gitignore` excludes common service-account names.
