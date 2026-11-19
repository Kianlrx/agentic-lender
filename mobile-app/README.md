# Android Mobile App

Agentic Lender is a native Android prototype for retail borrowers applying for a Mortgage or Home Equity Line of Credit (HELOC). It is built with Kotlin, Jetpack Compose, Firebase Authentication, and Cloud Firestore.

The app lets borrowers create an account, complete a loan application, upload documents, chat with a broker and track their application status. 

## Key Features

- Email and password signup, login, password rest and email verification
- Borrower profile store in Firebase
- Mortgage or HELOC application
- One active application at a time
- Save application draft at any time
- Review screen before submission
- Estimated borrowign capacity and monthly repayments
- LTI,DTI and LTV calculations
- Prototype risk level - Low, Medium or High
- AI property valuation and price comparision
- Document status tracking
- Real-time chat with an assigned broker

## Application Stages
- Draft - Application is being completed
- Applied - Application has been submitted
- AI Deliberate - AI is reviewing the applicaiton
- Conditionally Approved - Approved pending more documents
- Underwriter Approved - Financial details have been verificed
- Final Approval - Ready for funding
- Closed - Mortgage process is complete
- Declined - Application was not approved
  
## Requirements

- Android Studio with the Android SDK and a compatible JDK (the project
  configures a Java 25 Gradle daemon toolchain). You do not need to install
  Gradle separately; the checked-in Gradle wrapper downloads and runs it.
- An Android emulator or device with API level 34 or later
- Firebase setup completed as described in [the Firebase README](../firebase/README.md)

The checked-in `app/google-services.json` targets project `benchodlender` and
package `com.example.agenticlender`.

## Open and run

Open the `mobile-app` folder in Android Studio, allow Gradle sync to complete,
then select the `app` run configuration and an emulator/device.

From PowerShell, the available build and test commands are:

```powershell
cd mobile-app
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

## Firebase data

- Profiles: `users/{uid}`
- Loan Application: `application/{uid}`
- Messages: `chats/{uid}/messages/{messageId}`
- Uploaded documents: `documents/{uid}/{docId}`

The mobile app currently does not submit loan applications to the web app's
`applications` collection. See [the root README](../README.md) for the current
integration boundaries.
