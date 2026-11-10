# Android mobile app

Native Android prototype built with Kotlin, Jetpack Compose, Firebase
Authentication, and Cloud Firestore.

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
- Messages: `chats/{uid}/messages/{messageId}`

The mobile app currently does not submit loan applications to the web app's
`applications` collection. See [the root README](../README.md) for the current
integration boundaries.
