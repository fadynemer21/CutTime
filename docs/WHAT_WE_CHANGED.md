# CuTime — what we changed

This document is the consolidated change list for the current uncommitted
production-readiness and split-shift availability work. The chronological
history remains in `DEVELOPMENT_PROGRESS.md`.

Last updated: 31 July 2026

## Booking, appointment, and state hardening

- Required the explicit Review Booking step before an appointment can be
  submitted.
- Preserved duplicate-submit protection for rapid confirmation taps.
- Preserved already-loaded Home and appointment data when a listener reports a
  temporary offline or reconnect error.
- Added saved-data warnings and retry actions instead of replacing useful
  cached data with an empty error screen.
- Retained transactional slot locks, double-booking prevention, cancellation,
  rescheduling, holiday cancellation, completion, rating, and cancelled-history
  controls.
- Kept customer names as the barber-facing primary appointment identity.

## Split shifts and break hours

- Added up to six ordered working periods per weekday.
- Added visible controls to add, edit, and remove work periods from the Barber
  Hours screen.
- Defined gaps between periods as breaks; no separate break record is required.
- Updated slot generation so an appointment must fit completely inside one work
  period and cannot cross a break.
- Updated booking and rescheduling transactions to reread and validate the
  latest seven-day schedule before reserving a time.
- Rejected stale submissions into a break added after the customer opened the
  booking screen.
- Rejected invalid, overlapping, reversed, out-of-order, excessive, malformed,
  and midnight-wrapping periods.
- Updated public barber hours, next-available copy, and representative times to
  support multiple periods.
- Added the exact Wednesday example `09:00-12:00`, `14:00-16:00`, and
  `16:30-19:00` to automated and manual tests.
- Existing appointments are deliberately not silently cancelled by a new break;
  the barber can cancel a specific affected appointment explicitly.

## Availability data migration and Firebase configuration

- Added availability schema version 2 with nested `workingPeriods`.
- Added a pure, unit-tested Firestore document codec.
- Kept legacy single-range availability documents readable as one work period.
- Automatically upgrades an old document the next time the barber saves it.
- Retained legacy `startTime` and `endTime` as the first period so an older app
  build cannot expose a break as bookable time.
- Tightened Firestore availability writes to the expected version-2 top-level
  fields.
- Added Firestore emulator coverage for owner writes, customer denial,
  unsupported schema denial, and unknown-field denial.
- Added Auth, Firestore, Storage, and Emulator UI configuration to
  `firebase.json` without deploying it.

## Notifications and gallery hardening

- Kept the fixed two-hour and 30-minute appointment reminder schedule.
- Added an authenticated navigation allow-list for remote notification routes.
- Rejected malformed, external, oversized, authentication, and unknown routes.
- Restricted notification channel IDs to the declared CuTime channels.
- Serialized gallery caption saves and deletions.
- Disabled conflicting gallery actions while uploads, writes, deletes, or
  reorders are active.
- Added visible progress to caption and deletion confirmations.
- Preserved optimistic gallery-order rollback behavior.

## Accessibility, responsive UI, and text resources

- Added semantics and stable test tags to core Home, booking, appointment, and
  confirmation flows.
- Marked important section titles as accessibility headings.
- Added or retained descriptions for actionable icons and hid decorative icons
  from accessibility services.
- Extracted core customer Home, booking, appointments, navigation, availability,
  and notification copy into Android resources.
- Added quantity-aware duration and catalog-count plurals.
- Changed the Barber bottom label to the responsive `Hours` label without
  changing its route.
- Kept navigation labels to one line with ellipsis behavior.
- Allowed important booking actions to expand under large font settings.

## Offline and duplicate-action behavior

- Preserved useful listener data through reconnect failures.
- Added retry paths and explicit saved-data status.
- Prevented repeated booking, gallery caption, and gallery delete actions while
  a request is running.
- Retained transaction checks as the final app-side authority for current
  availability and occupied slots.

## Release and privacy hardening

- Disabled cleartext HTTP and Android backup/data transfer.
- Added API 26-30 and API 31+ backup exclusion resources.
- Enabled R8 code shrinking and resource shrinking for release builds.
- Added ProGuard rules for Firestore reflection models, FCM, WorkManager, and
  useful stack-trace metadata.
- Added ignored signing-secret and release-artifact patterns.
- Added a safe `keystore.properties.example` without creating a real key.
- Wired release signing to the ignored `keystore.properties` file when present;
  unsigned release verification continues to work when it is absent.
- Disabled the sample barber catalog in release builds while retaining the
  clearly labelled preview in debug builds.
- Added an authenticated, owner-only account deletion request flow and a manual
  processing runbook that works on the Spark plan.
- Moved the large logo to `drawable-nodpi` to avoid density inflation.
- Removed unused template colors and modernized mirrored navigation icons.

## Tests and continuous integration

- Expanded route, notification, booking, catalog reconnect, appointment
  reconnect, gallery duplicate-action, availability codec, overlap, break-slot,
  midnight-boundary, and ViewModel tests.
- Replaced the template instrumentation test with application, label, and backup
  policy checks.
- Added Compose test source for customer navigation, responsive barber
  navigation, and booking-form-to-review progression.
- Added 18 Firestore/Storage emulator rule tests.
- Retained 29 Cloud Functions tests.
- Added GitHub Actions jobs for Android tests, device-test compilation, Lint,
  debug/release builds, Functions checks, Firebase emulator checks, and build
  artifacts.

## Documentation and submission preparation

- Added the project README and architecture guide.
- Added Firebase setup, gallery/notification setup, privacy policy, Data Safety,
  permissions, QA, device matrix, screenshot plan, demo script, store listing,
  and release checklist documents.
- Documented Storage bearer-URL behavior, Firebase deployment boundaries,
  signing steps, data-handling decisions, and Play submission placeholders.
- Updated `DEVELOPMENT_PROGRESS.md` after each autonomous batch.

## Final local verification

- Debug and release Kotlin compilation passed.
- 300 JVM tests passed with 0 failures, 0 errors, and 0 skipped tests across 32
  suites.
- Six Android instrumentation/Compose tests compile; device execution remains
  pending.
- Android Lint passed with 0 errors and 11 reviewed toolchain/dependency update
  notices.
- Debug APK assembly passed.
- Minified and resource-shrunk unsigned release APK assembly passed.
- Firebase JSON files parse successfully.
- Git diff whitespace/error validation passed.
- No Firebase deployment, dependency installation, billing change, commit,
  push, pull request, signing key, or Play Console mutation was performed.

## Historical remaining work before final closeout (superseded below)

- Deploy the reviewed Firestore rules and indexes on the current Spark plan.
- Run the 18 Firebase emulator tests under Node.js 22; emulator execution does
  not require changing the live billing plan.
- Execute the six Android tests on an emulator or physical device.
- Complete manual Customer/Barber multi-account regression testing.
- Test minimum/current Android APIs, small phone, tablet, foldable, font scale,
  TalkBack, theme, rotation, process death, offline cache, and reconnect cases.
- Finish extracting remaining hard-coded user-facing strings and decide whether
  a second locale is required for submission.
- Decide whether the development fallback remains in release builds or is
  disabled for production.
- Publish the HTTPS deletion-information/request page and finalize the data
  retention/processing policy for the implemented in-app request flow.
- Replace publisher, privacy, support, website, and deletion URL placeholders.
- Create and securely back up a real upload key; configure ignored/CI signing.
- Produce and verify a signed Android App Bundle.
- Capture final screenshots, icon/feature graphic, and demo video.
- Finalize store copy, countries, minimum age, target audience, content rating,
  Data Safety, and privacy declarations.
- Upload to Play internal testing, resolve its pre-launch report, and complete
  the final device/user acceptance test.
- Review the complete working-tree diff, commit it, push it, and confirm GitHub
  Actions passes.

Firebase Storage gallery delivery and deployed Cloud Functions/server reminders
are intentionally excluded from this remaining-work section because they depend
on the upgraded live Firebase configuration.

## Saved handoff commands

Free Firestore deployment:

```powershell
cd C:\Users\Fady\Desktop\CutTime
firebase login
firebase use cuttime-b1fa1
firebase deploy --only firestore:rules,firestore:indexes
```

Local Firebase emulator tests after installing Node.js 22:

```powershell
cd C:\Users\Fady\Desktop\CutTime\firebase-tests
npm install
npm test
```

Review, commit, and push the complete working batch:

```powershell
cd C:\Users\Fady\Desktop\CutTime
git status
git diff --check
git add -A
git status
git commit -m "Add production hardening and split-shift availability"
git push origin main
```

## Final non-Blaze closeout batch (31 July 2026)

Saved changes from this closeout:

- Release builds no longer expose development barber previews; debug builds
  retain the labelled, non-bookable fallback.
- Optional ignored release signing is wired through
  `keystore.properties.example` without creating or committing a key.
- Customers and Customer Mode barbers can submit a protected in-app account
  deletion request. Firestore validates identity, email, role, status, and
  server timestamp, and mobile clients cannot impersonate, mutate processing
  status, or delete a request.
- Added the account deletion repository, ViewModel, Profile UI, confirmation
  flow, five JVM tests, three emulator tests, manual processing runbook, and
  `docs/account-deletion.html` ready for publisher details and HTTPS hosting.
- Extracted remaining direct Compose labels and accessibility descriptions to
  Android resources and fixed mojibake in punctuation/currency resources.
- Corrected a Firebase Admin typing regression in the rating notification
  trigger.
- Fixed Firebase rule-test isolation and made the complete suite deterministic.
- Added reviewed pnpm lockfiles and deterministic Functions/rules CI setup.

Final results:

- **306/306 JVM tests passed**.
- **6/6 Android instrumentation/Compose tests passed** on a headless API 37
  emulator.
- **21/21 Firestore/Storage emulator tests passed**.
- **29/29 Functions tests passed**, with TypeScript type-check/build passing.
- Android Lint passed with **0 errors** and 11 reviewed update notices.
- Debug APK, minified/resource-shrunk unsigned release APK, and unsigned release AAB all built.
- Release `ENABLE_DEVELOPMENT_CATALOG` is verified `false`.

Earlier entries saying emulator/device/Functions execution or localization were
pending are now historical and superseded by the results above.

## Current non-Blaze handoff

### Firebase console/CLI

These actions require the project owner but do not require Blaze:

```powershell
cd C:\Users\Fady\Desktop\CutTime
firebase login
firebase use cuttime-b1fa1
firebase deploy --only firestore:rules,firestore:indexes
```

Then wait for every composite index to show **Enabled**, relaunch both test
accounts, and execute the live Customer/Barber smoke checklist in
`docs/QA_PLAN.md`. The `accountDeletionRequests` collection is created
automatically by the first valid in-app request; do not create it manually.

### Review, commit, and push after live verification

```powershell
cd C:\Users\Fady\Desktop\CutTime
git status
git diff --check
git add -A
git status
git commit -m "Complete CuTime non-Blaze production readiness"
git push origin main
```

### Still requires the publisher, not unattended code work

- Replace support/publisher/privacy placeholders and finalize retention.
- Publish the privacy and deletion pages on durable HTTPS URLs.
- Create/back up the upload key and build a signed AAB.
- Finish physical-device, accessibility, offline, and real-account QA.
- Capture store assets and complete Play Console declarations/internal test.
- Confirm the pushed Node 22 GitHub Actions run passes.

Blaze-only work remains Firebase Storage-backed gallery delivery and deployed
Cloud Functions/server FCM reminders. Local two-hour and 30-minute WorkManager
reminders are already implemented and tested independently of Blaze.

## Responsive scheduling and flexible barber actions (31 July 2026)

- Fixed public opening-hours rows for split shifts. Multi-period schedules now
  keep the weekday on one line and render the periods below it without crushing
  either column.
- Fixed Review Booking summary rows so Date and other labels remain horizontal
  while long values wrap and align cleanly.
- Reworked Barber Dashboard appointment metadata into separate date and
  time/duration rows, preventing narrow one-character wrapping.
- Fixed Barber Appointment Details layout direction, Date/Time/Status/Price
  label widths, long value wrapping, pluralized duration, and localized status
  and currency output.
- Barbers can now mark any upcoming appointment completed, including one whose
  scheduled end time is still in the future. Completion atomically releases
  its booking-slot locks so the original future time can become available.
- Barber accounts in Customer Mode can now book their own real shop. This uses
  the barber account identity and the normal service, availability,
  conflict-prevention, and appointment lifecycle.
- Added Firestore emulator coverage proving own-shop Customer Mode booking,
  booking-slot creation, future completion, slot release, and customer denial.

Verification for this increment:

- Kotlin compilation and **306/306 JVM tests passed**.
- Android Lint passed with **0 errors** (11 existing reviewed update notices).
- Debug APK assembled successfully.
- **23/23 Firestore and Storage emulator tests passed**.

The updated firestore.rules must be deployed again before early completion
and own-shop Customer Mode booking work against live Firebase. No new index is
required for this increment:

    cd C:\Users\Fady\Desktop\CutTime
    firebase.cmd deploy --only firestore:rules