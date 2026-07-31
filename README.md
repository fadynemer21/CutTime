# CuTime

CuTime is a Kotlin and Jetpack Compose Android application for discovering
barbers, managing a real barber shop, and booking appointments. Firebase
Authentication provides account sessions and roles; Cloud Firestore stores
profiles, services, availability, appointments, ratings, and notification
records; Firebase Storage is prepared for barber galleries; Firebase Cloud
Messaging and WorkManager provide remote and local appointment updates.

## Product capabilities

### Customer

- Register, sign in, restore a session, reset a password, and sign out.
- Search a live catalog of complete barber shops.
- View barber details, hours, services, ratings, reviews, and gallery images.
- Select an availability-driven service, date, and unoccupied time.
- Review before submitting a transactionally protected appointment.
- View upcoming, completed, and cancelled appointments.
- Cancel or atomically reschedule an upcoming appointment.
- Long-press a cancelled appointment to remove it from customer history.
- Rate a completed appointment once.
- Edit the customer display name.
- View and manage in-app notification preferences.
- Receive two-hour and 30-minute local reminder work.

### Barber

- Create and edit a public shop profile.
- Add, update, and delete services.
- Configure up to six separate work periods per day, automatic break gaps,
  seven-day working hours, and blocked holiday dates.
- Automatically cancel affected upcoming appointments when saving holidays.
- View customer names and appointment details.
- Cancel appointments or complete them after their end time.
- Manage gallery images when Firebase Storage is enabled.
- View reviews and rating aggregates.
- Enter Customer Mode without changing the secure `BARBER` role.

## Technology

- Kotlin, Jetpack Compose, Material 3, Navigation Compose
- Firebase Authentication, Firestore, Storage, and Messaging
- WorkManager for on-device reminder scheduling
- Coil for gallery image loading
- TypeScript Firebase Functions for trusted notification delivery
- JUnit, Compose UI tests, Android Lint, R8, Firebase emulator rule tests
- GitHub Actions CI

## Local setup

1. Install Android Studio with its bundled JDK.
2. Open this repository and allow Gradle sync to finish.
3. Confirm `app/google-services.json` belongs to Firebase Android package
   `com.fadynemer.cutime`.
4. Follow [FIREBASE_SETUP.md](FIREBASE_SETUP.md) for Authentication,
   Firestore rules, and indexes.
5. Gallery and server notification setup is documented in
   [GALLERY_NOTIFICATIONS_SETUP.md](GALLERY_NOTIFICATIONS_SETUP.md).
6. Run the debug app from Android Studio.

Debug builds may show a clearly labelled, non-bookable sample catalog while Firestore is empty. Release builds never include this fallback: a shop appears only after a Barber account saves a valid profile, at least one service, and an availability week containing at least one open day.

## Local verification

From PowerShell:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:compileDebugAndroidTestKotlin
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease
```

Device tests require an emulator or physical Android device:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

Firebase emulator test source is in
[firebase-tests](firebase-tests/README.md). It is intentionally independent of
the live project.

## Release notes

Release APKs are minified and resource-shrunk with R8. The repository does not
contain a real signing key. `app:assembleRelease` therefore creates an unsigned
release artifact for shrinker verification only. Copy
`keystore.properties.example` to a local ignored file after a real upload key
has been created, then configure and verify signing before Play submission.

Android backup and cleartext HTTP are disabled to protect account-mode and
device-registration state. Remote notification routes are restricted to an
allow-list of authenticated in-app destinations.

## Documentation

- [What we changed](docs/WHAT_WE_CHANGED.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Privacy policy draft](docs/PRIVACY_POLICY.md)
- [Google Play Data Safety draft](docs/DATA_SAFETY.md)
- [Permissions explanation](docs/PERMISSIONS.md)
- [Account deletion runbook](docs/ACCOUNT_DELETION_RUNBOOK.md)
- [Account deletion web-page source](docs/account-deletion.html)
- [Manual QA plan](docs/QA_PLAN.md)
- [Device matrix](docs/DEVICE_MATRIX.md)
- [Screenshot plan](docs/SCREENSHOT_PLAN.md)
- [Demo script](docs/DEMO_SCRIPT.md)
- [Store listing draft](docs/STORE_LISTING.md)
- [Release checklist](docs/RELEASE_CHECKLIST.md)
- [Development progress](DEVELOPMENT_PROGRESS.md)

## Important constraints

- Do not deploy rules, indexes, Storage configuration, or Functions without
  reviewing the target project shown by `.firebaserc`.
- Do not commit `keystore.properties`, `*.jks`, `*.keystore`, local Firebase
  logs, or generated release bundles.
- Firebase Storage and scheduled Cloud Functions require the appropriate live
  Firebase billing configuration. The rest of the app remains locally
  buildable without deploying them.
- The privacy and store documents contain owner/contact placeholders that must
  be replaced before public submission.
