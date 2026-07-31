# CuTime release checklist

## Code freeze

- [ ] Review working-tree diff and remove accidental/debug-only changes.
- [x] Confirm `applicationId` is `com.fadynemer.cutime`.
- [ ] Choose final `versionCode` and `versionName`.
- [ ] Confirm min/target/compile SDK choices against current Play requirements.
- [ ] Confirm no TODO, placeholder, owner contact, test credential, or secret is
      shipped.
- [x] Confirm the development fallback is disabled for release.

## Automated verification

- [x] `:app:compileDebugKotlin` passes.
- [x] `:app:testDebugUnitTest` passes: 306 tests.
- [x] `:app:connectedDebugAndroidTest` passes: 6 tests.
- [x] `:app:lintDebug` has 0 errors and 11 reviewed update notices.
- [x] `:app:assembleDebug` passes.
- [x] `:app:assembleRelease` passes with R8/resource shrinking.
- [x] `:app:bundleRelease` produces the expected unsigned AAB.
- [ ] Cloud Functions build/tests pass under Node 22. (29 tests passed locally under Node 24; CI is pinned to Node 22.)
- [x] Firestore and Storage emulator rule tests pass: 21 tests.
- [ ] GitHub Actions passes on the release commit.

## Firebase

- [ ] Verify `.firebaserc` target before any command.
- [ ] Authentication Email/Password is enabled.
- [ ] `google-services.json` matches package/project and contains no obsolete
      app registration.
- [ ] Deploy reviewed Firestore rules.
- [ ] Deploy all required Firestore composite indexes and wait for Enabled.
- [ ] Run live Customer and Barber read/write smoke tests.
- [ ] Decide Blaze billing and budget alerts.
- [ ] If enabled, create Storage bucket and deploy Storage rules.
- [ ] If enabled, deploy Functions to the intended region.
- [ ] Verify scheduled reminder function and FCM delivery logs.
- [ ] Configure App Check if selected, then test enforcement safely.
- [ ] Define monitoring, alerting, retention, and backup/export ownership.

## Signing

- [ ] Create a real upload key outside the repository.
- [ ] Back it up securely with recovery instructions.
- [ ] Copy `keystore.properties.example` to ignored `keystore.properties`.
- [ ] Add local/CI secret-based signing configuration.
- [ ] Never commit passwords or keystore files.
- [ ] Generate signed AAB.
- [ ] Verify signature and install via internal testing.
- [ ] Enroll in Play App Signing and store upload-key ownership details.

## Device QA

- [ ] Execute every P0 row in `DEVICE_MATRIX.md`.
- [ ] Complete authentication, booking, double-booking, rescheduling, holiday
      cancellation, rating, history, gallery, and notification cases.
- [ ] Test offline warm/cold cache and reconnect.
- [ ] Test TalkBack, font scale 1.3×/2.0×, display size, light/dark themes.
- [ ] Test foreground/background/terminated FCM.
- [ ] Test two-hour and 30-minute WorkManager reminders under battery saver.
- [ ] Test fresh install and upgrade from the previous internal build.
- [ ] Record all residual known issues and severity.

## Privacy and policy

- [ ] Replace all privacy policy placeholders.
- [ ] Publish privacy policy at a durable public HTTPS URL.
- [x] Implement an authenticated in-app account deletion request flow.
- [ ] Publish the public HTTPS account deletion information/request URL.
- [ ] Finalize Data Safety using the exact release and console setup.
- [ ] Confirm target audience, minimum age, content rating, and countries.
- [ ] Confirm no unreported analytics/diagnostic/advertising SDK.
- [ ] Confirm gallery image rights and review moderation/support process.
- [ ] Confirm notification copy and consent behavior.

## Store assets

- [ ] Final app icon and feature graphic.
- [ ] Capture approved phone screenshots.
- [ ] Capture tablet screenshots if required/used.
- [ ] Record and edit demo video.
- [ ] Finalize app name, short description, full description, and release notes.
- [ ] Add support email, website, privacy URL, and deletion URL.
- [ ] Proofread English copy and remove all fictional/test data disclosures.

## Play Console

- [ ] Create app and complete required declarations.
- [ ] Upload signed AAB to internal testing.
- [ ] Resolve pre-launch report crashes, ANRs, accessibility, and security
      findings.
- [ ] Invite testers and complete real multi-account live test.
- [ ] Promote only after exit criteria are met.
- [ ] Save the final version, Git commit, build fingerprint, and release notes.

## Post-release

- [ ] Monitor authentication, Firestore, Storage, Functions, FCM, crash, ANR,
      and review signals.
- [ ] Verify scheduled reminders after production release.
- [ ] Respond to deletion/support requests.
- [ ] Record hotfix criteria and rollback/contact plan.
