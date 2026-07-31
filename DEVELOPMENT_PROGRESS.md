# CuTime development progress

This document records completed autonomous development batches and the remaining
roadmap. It is intentionally updated before new batch summaries are delivered.

## Current completion estimate

Estimated overall completion after the production-readiness batch:
**94-96%**.

The product implementation, cross-role lifecycle, real catalog, gallery,
notification/reminder architecture, local hardening, test source, release
configuration, CI, and submission documentation are implemented. The remaining
work is primarily live Firebase deployment/verification, device and emulator
execution, real release signing, final media assets, account-deletion policy,
and Play Console submission.

## Completion estimate after barber-management batch

Estimated overall completion: **58-62%**

Completed foundations include authentication, role routing, customer discovery
and barber profiles, booking selection, transactional appointment creation,
customer appointments, barber dashboard/navigation, profile management, service
management, weekly availability, local Firebase configuration, and automated
domain/ViewModel tests.

## Appointment and customer-navigation batch

- Added Firestore appointment and booking-slot models.
- Added transactional appointment creation.
- Added 15-minute slot locks covering the full service duration.
- Added overlapping and double-booking prevention.
- Added authentication, future-time, service, and price validation.
- Connected the booking review screen to real submission.
- Added loading, conflict, permission, network, and success states.
- Added customer My Appointments with upcoming, completed, and cancelled groups.
- Added live Firestore appointment observation.
- Added Home/Appointments customer navigation.
- Added local Firestore rules, indexes, Firebase project configuration, and
  deployment documentation.
- Corrected the instrumentation package assertion.
- Replaced template tests with appointment validation and ViewModel tests.

Verification at that checkpoint:

- 17 unit tests passed.
- Kotlin compilation passed.
- Android Lint reported 0 errors.
- Debug APK assembly passed.

## Barber-management batch

- Replaced the placeholder Barber Dashboard.
- Added today and upcoming appointment sections with live Firestore updates.
- Added Dashboard, Services, Availability, and Profile barber navigation.
- Added barber-owned public profile editing.
- Added service creation, editing, deletion, validation, and confirmation.
- Added weekly open/closed controls and opening/closing time editing.
- Added blocked dates for holidays and time off.
- Added validation for working hours and 15-minute service durations.
- Connected local booking-time checks to working-hour constraints.
- Added barber-owned Firestore profile, service, and availability repositories.
- Extended Firestore rules and indexes for barber ownership and queries.
- Removed empty Appointment/Profile placeholder classes and corrected naming.

Verification at that checkpoint:

- 25 unit tests passed with 0 failures or skipped tests.
- Kotlin compilation passed.
- Android Lint reported 0 errors.
- Debug APK assembly passed.
- Firebase JSON configuration and Git diff checks passed.

## Major remaining roadmap

- Connect real Firestore barber profiles/services/availability to customer Home.
- Appointment cancellation and rescheduling.
- Barber appointment completion/cancellation workflows and customer details.
- Customer Profile and appropriate logout placement.
- Persistent Customer Mode for Barber accounts without changing the stored role.
- Ratings after completed appointments.
- Firebase Storage gallery upload/view/delete.
- Notifications and appointment reminders.
- Expanded rule/emulator, UI, offline, accessibility, and device-size testing.
- Strings/resources cleanup, README, screenshots, demo video, and final submission.

No Firebase deployment, Git commit, or push is performed by autonomous batches.

## Cross-role appointment lifecycle and account-mode batch

- Added customer cancellation for upcoming appointments.
- Cancellation atomically changes appointment status and releases all slot locks.
- Added barber completion and cancellation actions from Dashboard.
- Extended Firestore rules so only the owning customer or barber can perform
  permitted status transitions.
- Added a Customer Profile screen and third customer navigation tab.
- Moved customer logout from Home into Profile.
- Added persistent, UID-scoped Customer Mode for Barber accounts.
- Added Return to Barber Mode without changing the stored `BARBER` role.
- Updated Splash session restoration to respect the saved interface mode.
- Stored the authenticated customer identifier on new appointments for the
  barber appointment display.
- Expanded appointment action tests and retained all prior validation coverage.

Estimated overall completion after this batch: **68-72%**.

## Real-data customer experience and lifecycle mega-batch

This batch moves the customer experience from sample-only discovery to a
Firestore-backed catalog and completes the core cross-role appointment
lifecycle. The sample catalog remains available only as an explicitly labelled
development fallback when Firestore has no complete barber data or a catalog
read fails.

### Real barber discovery

- Added a real-time Firestore catalog repository for `barberProfiles`.
- Aggregated each public profile with its `services` subcollection and matching
  `barberAvailability` document.
- Mapped aggregate rating count and average into customer barber cards.
- Derived opening-hour rows, starting prices, representative times, and next
  opening information from live barber data.
- Added a process cache so the selected real barber remains available across
  Home, profile, booking, and rescheduling screens.
- Added loading, retry, error, empty, and clearly identified development
  fallback states to Home.
- Kept the established barber-card and profile design instead of replacing it.

### Availability-aware booking

- Added a reusable slot-generation domain service.
- Generated appointment starts on 15-minute boundaries.
- Prevented services from extending beyond closing time.
- Excluded closed days and barber-blocked dates.
- Excluded past starts when booking on the current date.
- Observed occupied slot-lock documents for the selected barber and date.
- Removed any start whose complete service duration overlaps an occupied
  15-minute segment.
- Kept the Firestore transaction as the final concurrency authority so two
  customers racing for a newly freed time cannot double-book it.

### Appointment details and navigation

- Added typed route definitions and route builders for every customer and
  barber destination.
- Added customer appointment-detail navigation from all appointment groups.
- Added barber appointment/customer-detail navigation from Dashboard cards.
- Added detail views for service, barber/customer, date, time, duration, price,
  status, cancellation, completion, rescheduling, and rating entry.
- Added real-time detail observation so status changes are reflected without a
  manual refresh.
- Added optional appointment audit metadata for creation, last update,
  rescheduling, and submitted rating.

### Transactional rescheduling

- Added a customer rescheduling flow for upcoming appointments.
- Generated new choices from the same live barber hours and occupied slots used
  by first-time booking.
- Allowed movement that overlaps the appointment's own old locks while still
  excluding locks belonging to other appointments.
- Added a transaction that reads the appointment and every proposed new lock,
  rejects conflicts, deletes obsolete old locks, writes new locks, and updates
  the original appointment document atomically.
- Preserved the original appointment ID and immutable customer, barber, service,
  price, and duration data.
- Added clear success, conflict, permission, loading, and retry states.

### Ratings and reviews

- Added one rating document per completed appointment.
- Restricted submission to the customer who owns the completed appointment.
- Prevented duplicate reviews by using the appointment ID as the rating ID.
- Added 1-5 star selection and an optional review capped at 500 characters.
- Added a transaction that creates the rating, records it on the appointment,
  and updates barber rating count, sum, and average atomically.
- Added an aggregate audit pointer (`lastRatingId`) used by Firestore rules to
  prove that each aggregate increment belongs to a newly created valid rating.
- Added live written-review display to customer-facing barber profiles.
- Hid repeat rating submission after an appointment has a rating.

### Customer profile editing

- Added full-name editing from the customer Profile tab.
- Added length, blank-value, and one-line validation.
- Kept UID, role, and email immutable.
- Updated both Firebase Authentication display name and the Firestore user
  profile, including a best-effort Authentication rollback if Firestore fails.
- Added saving, validation, success, and failure UI states.

### Firestore configuration

- Expanded rules for safe customer profile-name updates.
- Added rules for customer rescheduling and atomic old-slot release.
- Allowed authenticated occupied-slot queries required by the booking UI.
- Added rules for completed-appointment rating creation and linked appointment
  updates.
- Added narrowly scoped rating-aggregate updates on barber profiles.
- Added composite indexes for occupied booking slots and barber reviews.
- Kept delete operations denied for users, appointments, barber profiles,
  availability, and ratings unless an explicit lifecycle transaction requires
  slot cleanup.

### Automated verification coverage

- Added deterministic tests for working hours, closing boundaries, blocked
  dates, past dates/times, duration rounding, and overlapping occupied slots.
- Added customer profile load/edit/save/validation/error tests.
- Added appointment detail observation and status-action tests.
- Added rescheduling state, request, success, conflict, and listener tests.
- Added rating entry, existing-rating, submission, error, and listener tests.
- Added barber review loading, filtering, retry, and listener tests.
- Retained the earlier booking, grouping, barber-management, and authentication
  routing coverage.

Final local verification results and actual line counts for this batch are
recorded at the end of this file after the last build, Lint, and APK run.

## Remaining roadmap after the mega-batch

- Firebase Storage gallery upload, viewing, replacement, and deletion.
- Push notifications, in-app notification state, and appointment reminders.
- Firestore emulator security-rule tests and multi-account live Firebase tests.
- Compose UI/instrumentation tests on an emulator or device.
- Accessibility audit, large-font testing, and compact/large device layouts.
- Offline-state and reconnect testing for listeners and transactions.
- String-resource extraction, localization readiness, and copy cleanup.
- Release signing, production build configuration, screenshots, demo video,
  README polish, privacy documentation, and final submission packaging.

## Mega-batch final verification

Estimated overall project completion after this batch: **80-84%**.

The estimate reflects a complete local customer/barber core lifecycle, while
Firebase Storage gallery work, notifications, emulator/device validation,
accessibility/offline hardening, release configuration, and submission assets
remain.

Locally verified on 30 July 2026:

- Debug Kotlin compilation: passed.
- JVM unit tests: **130 passed**, 0 failures, 0 errors, 0 skipped.
- Android Lint: passed with **0 errors** and 27 existing dependency-catalog
  suggestions.
- Debug APK assembly: passed.
- APK output: `app/build/outputs/apk/debug/app-debug.apk`.
- Firebase JSON configuration: parsed successfully.
- Git whitespace/error check: passed.
- Firestore rule braces and local configuration shape: checked.
- Firebase CLI rule compilation and deployment: not run because the Firebase
  CLI is not installed locally and this batch explicitly forbids installing
  dependencies or deploying.

Meaningful nonblank line accounting immediately before this results block:

- Production Kotlin: 11,075 lines.
- JVM test Kotlin: 2,407 lines.
- Firestore rules and indexes: 411 lines.
- Progress and Firebase documentation: 295 lines.
- Counted scope total: 14,188 lines.
- Recorded pre-mega-batch baseline: 8,513 lines.
- Mega-batch increase at that checkpoint: **5,675 meaningful lines**.

No Firebase deployment, dependency installation, emulator/device use, Git
commit, push, or pull request was performed.

## Gallery, notifications, and reminders mega-batch

### Firebase Storage barber gallery

- Added a real Firebase Storage and Firestore-backed gallery repository.
- Added content selection through Android's system picker.
- Added MIME type, size, caption, and image-count validation.
- Added progress reporting for active uploads.
- Added best-effort binary cleanup when metadata persistence fails.
- Added caption editing, delete confirmation, and complete-order batch writes.
- Added optimistic reordering with rollback on failure.
- Added missing-object recovery so stale metadata can still be removed.
- Added a barber gallery management screen under Profile.
- Added Coil network image loading for management and customer views.
- Replaced customer profile placeholders with real gallery images when present.
- Preserved the clearly identified sample gallery only for development fallback
  catalog entries.
- Added full-size customer image previews without changing the established
  profile layout.

### Notification center and preferences

- Added typed notification and preference domain models.
- Added owner-scoped notification, settings, and device repositories.
- Added customer and barber notification inbox routes.
- Added unread badges on customer Home and Barber Dashboard.
- Added Today, Yesterday, This week, and Earlier inbox grouping.
- Added per-item read state, Mark all read, delete, retry, loading, error, and
  empty states.
- Added notification settings for master push, appointment updates, reminders,
  and review prompts.
- Each eligible appointment now schedules both required reminders: 2 hours and
  30 minutes before its start time.
- Added runtime `POST_NOTIFICATIONS` permission handling for Android 13+.
- Added target-aware navigation into customer or barber appointment details.
- Added payload fallback routing for FCM data messages.

### FCM and device lifecycle

- Added Firebase Messaging to the Android application.
- Added a manifest-declared `FirebaseMessagingService`.
- Added notification channels and a proper monochrome small icon.
- Added foreground/data-message publishing with stable notification IDs.
- Added FCM token hashing and per-user device registration.
- Added token refresh handling.
- Added authenticated launch-time token synchronization.
- Added current-device unregistration before customer and barber logout so a
  later account on the same phone cannot inherit the previous account's push
  destination.
- Added external intent route consumption after authenticated navigation is
  ready.

### Local appointment reminders

- Added a pure reminder planner for eligibility, trigger time, message content,
  and delay calculation.
- Added WorkManager scheduling keyed by appointment ID.
- Added independent work identities for the 2-hour and 30-minute reminders so
  neither reminder replaces the other.
- Added replacement when appointment time or preference changes.
- Added cancellation of obsolete appointment reminder work.
- Added preference-aware synchronization from the live upcoming appointment
  list.
- Added a Worker that publishes a routed appointment notification.

### Trusted Cloud Functions source

- Added a Node.js 22 TypeScript functions project.
- Added retry-safe appointment-created, appointment-updated, and rating-created
  Firestore triggers.
- Added customer and barber booking confirmations.
- Added cancellation, reschedule, completion, and review-request events.
- Added Barber notifications for new customer ratings.
- Added deterministic notification document IDs so trigger retries do not
  duplicate in-app records or push sends.
- Added per-user category and master push preference enforcement.
- Added FCM multicast messages with appointment routes and channel metadata.
- Added automatic cleanup of invalid/unregistered FCM tokens.
- Added a 15-minute scheduled reminder scan bounded to appointments in the next
  24 hours.
- Added per-recipient reminder timing so customer and barber preferences can
  differ.
- Added pure TypeScript tests for content, transitions, routing, message
  construction, retry IDs, preference categories, and reminder windows.

### Firebase security and configuration

- Added `storage.rules` with Barber-role ownership, path, MIME, size, and custom
  metadata enforcement.
- Denied client-side gallery object replacement and all unmatched Storage
  paths.
- Added Firestore gallery metadata create/update/delete validation.
- Allowed notification clients to read, acknowledge, or delete only their own
  records while denying client notification creation.
- Added owner-only preference and device rules with strict field allowlists.
- Added the scheduled reminder composite index.
- Registered Storage and Functions in `firebase.json`.
- Added a complete enablement, deployment, live-test, security, monitoring, and
  troubleshooting guide in `GALLERY_NOTIFICATIONS_SETUP.md`.

### Automated coverage added

- Gallery format, byte limit, caption, count limit, and progress tests.
- Public gallery observation, selection, refresh, retry, and listener tests.
- Gallery management upload, caption, delete, reordering, boundary, success,
  failure, optimistic update, and rollback tests.
- Notification routing and FCM payload routing tests.
- Notification time display and grouping tests.
- Stable device-token hashing tests.
- Notification center read, mark-all, delete, retry, concurrency, and error
  tests.
- Notification preference draft, incoming-update, validation, discard, save,
  and retry tests.
- Unread badge snapshot tests.
- Local reminder eligibility, timing, delay, and message-format tests.
- Cloud Functions pure lifecycle and delivery-helper tests (source added; Node
  test execution is pending because Node/npm is not installed locally).

### Remaining roadmap after this batch

- Deploy and live-test Firestore rules, indexes, Storage rules, and Functions.
- Run Firestore/Storage rules emulator tests after Firebase CLI is available.
- Run Compose UI/instrumentation tests on an emulator or physical device.
- Test image picking across Android API levels and common gallery providers.
- Test FCM delivery in foreground, background, terminated, token refresh, and
  multi-account logout/login scenarios.
- Test WorkManager timing under Doze and vendor-specific battery management.
- Perform TalkBack, large-font, contrast, touch-target, compact-phone, tablet,
  offline, and reconnect audits.
- Extract remaining UI strings and prepare localization.
- Add release signing, shrinking review, privacy documentation, screenshots,
  demo video, polished README, and final submission package.

No Firebase deployment, Git commit, push, or pull request was performed by this
batch. Final verification and line accounting for this batch are recorded after
the last Lint and APK run.

## Gallery/notification mega-batch final verification

Estimated overall project completion after this batch: **90-93%**.

Locally verified on 30 July 2026:

- Debug Kotlin compilation: passed.
- JVM unit tests: **244 passed**, 0 failures, 0 errors, 0 skipped.
- Android Lint: passed with **0 errors** and 22 non-blocking dependency,
  toolchain, KTX, icon-location, and unused/legacy resource suggestions.
- Debug APK assembly: passed.
- APK output: `app/build/outputs/apk/debug/app-debug.apk`.
- APK size: 29,113,039 bytes.
- Firebase JSON configuration files: parsed successfully.
- Git whitespace/error check: passed.
- Firestore and Storage rule files: locally reviewed and structurally checked.
- Cloud Functions package and TypeScript configuration JSON: parsed
  successfully.
- Cloud Functions TypeScript compilation/tests: not run because Node.js and npm
  are not installed on this machine. The source and tests are complete, and the
  exact later commands are documented in `GALLERY_NOTIFICATIONS_SETUP.md`.
- Firebase CLI rules compilation and all deployments: not run. Nothing was
  changed in the live Firebase project.

Meaningful nonblank line accounting after the final Android verification:

- Production Kotlin: 15,035 lines.
- JVM test Kotlin: 4,080 lines.
- Firebase rules, indexes, Functions, and configuration: 2,012 lines.
- Progress and Firebase documentation: 796 lines.
- Counted scope total: 21,923 lines.
- Recorded pre-batch checkpoint: 14,188 lines.
- This batch's increase: **7,735 meaningful nonblank lines**.

The count excludes generated build output, downloaded dependencies, APK
contents, blank lines, IDE files, and duplicated generated code. No filler was
added to reach a line target.

## Dual-reminder and real-barber clarification follow-up

- Replaced the selectable single reminder with a fixed pair: 2 hours and
  30 minutes before each eligible appointment.
- Gave each local WorkManager request a unique appointment-and-lead identity so
  both reminders remain scheduled independently.
- Gave each server reminder event a lead-specific deterministic identity so
  scheduler scans and retries cannot merge or duplicate the pair.
- Updated notification preferences, Firestore settings schema, rules, Cloud
  Functions source, Android UI, tests, and setup documentation together.
- Clarified throughout the customer UI that bundled generic barber shops are
  non-bookable development previews.
- Clarified that a bookable shop is created only when a real Barber account
  saves its profile, at least one service, and availability.

Verification after this follow-up:

- Debug Kotlin compilation: passed.
- JVM unit tests: **246 passed**, 0 failures, 0 errors, 0 skipped.
- Android Lint: passed with **0 errors** and 22 non-blocking warnings.
- Debug APK assembly: passed.
- APK output: `app/build/outputs/apk/debug/app-debug.apk`.
- Firebase and Functions JSON configuration parsing: passed.
- Git whitespace/error check: passed.
- No Firebase deployment, commit, or push was performed.

## Real Barber shop publication readiness

- Verified the complete live publication path from Barber registration through
  customer booking.
- Added a real-time Shop Setup card to the Barber Dashboard.
- The card checks the same three requirements used by Customer Home:
  - a valid public shop name and description
  - at least one valid service
  - a persisted seven-day availability schedule with an open working day
- Added direct setup actions for Profile, Services, and Hours.
- Added a green `Your shop is live` state when the shop qualifies for Customer
  Home and appointment booking.
- Kept gallery images optional; a Barber can publish and accept bookings before
  uploading gallery content.
- Hardened customer catalog aggregation so malformed legacy services,
  incomplete availability, all-closed schedules, and incomplete profiles do
  not appear as bookable shops.
- Added pure readiness validation and ViewModel listener/retry tests.
- Added clear customer-facing explanations that generic bundled shops are
  development previews rather than registered Barber businesses.

Publication remains automatic. Adding or changing a service or availability
touches the Barber profile timestamp, causing the Customer Home listener to
re-evaluate the complete shop without a manual publish button.

Verification after adding publication readiness:

- Debug Kotlin compilation: passed.
- JVM unit tests: **263 passed**, 0 failures, 0 errors, 0 skipped.
- Android Lint: passed with **0 errors** and 22 non-blocking warnings.
- Debug APK assembly: passed.
- Git whitespace/error check: passed.
- No Firebase deployment, commit, or push was performed.

## Live-test lifecycle and booking UX corrections

- Changed appointment creation to use the authenticated customer's Firestore
  `fullName` as the canonical barber-facing name instead of falling back to an
  email address.
- Added owner-scoped repair of legacy appointment records when a customer opens
  My Appointments, so existing bookings can adopt the current profile name.
- Removed customer email from the barber appointment-detail presentation.
- Added Firestore rule validation linking every new or repaired appointment
  name to the owning customer's profile.
- Changed blocked-date saves so the holiday is persisted first, preventing new
  bookings and reschedules, then all upcoming appointments on blocked dates are
  cancelled automatically.
- Released every cancelled appointment's booking-slot locks so those records do
  not remain falsely occupied.
- Split holiday cancellations into rule-safe batches and made retries heal a
  partially completed cancellation pass.
- Added a clear availability success message with the number of appointments
  cancelled by the holiday.
- Added transaction and Firestore-rule checks preventing booking or
  rescheduling onto a newly blocked date.
- Removed the premature booking summary from the service/date/time form. The
  complete summary now appears only after all three choices are valid and the
  customer presses `Review Booking`.
- Cleared a previously selected time when the service changes because service
  duration changes which starts are valid.
- Added customer-name, holiday-policy, availability-result, and blocked-booking
  unit coverage.

Live Firebase verification of these corrections requires republishing the
updated `firestore.rules`, installing the new debug build, and repeating the
customer/barber flow. No Firebase deployment, commit, or push was performed.

Local verification after these corrections:

- Debug Kotlin compilation: passed.
- JVM unit tests: **274 passed**, 0 failures, 0 errors, 0 skipped.
- Android Lint: passed with **0 errors** and 22 non-blocking warnings.
- Debug APK assembly: passed.
- APK output: `app/build/outputs/apk/debug/app-debug.apk`.
- Git whitespace/error check: passed; only existing Windows line-ending notices
  were reported.

## Cancelled appointment history removal

- Added a long-press action to cancelled customer appointment cards.
- Added a confirmation explaining that deletion removes the appointment only
  from the customer's history and preserves the barber's shared record.
- Implemented owner-scoped soft deletion through `hiddenFromCustomer` rather
  than destroying appointment audit data.
- Restricted history removal to the authenticated customer who owns an already
  cancelled appointment.
- Filtered hidden records from subsequent customer appointment snapshots
  without requiring another composite Firestore index.
- Added Firestore rules that allow a one-way hide transition while continuing
  to deny appointment document deletion.
- Added ViewModel and domain-policy tests for successful removal, ownership,
  appointment status, and repeat-removal protection.

Live verification requires republishing `firestore.rules` and installing the
new build. No Firebase deployment, commit, or push was performed.

Verification after cancelled-history removal:

- Debug Kotlin compilation: passed.
- JVM unit tests: **278 passed**, 0 failures, 0 errors, 0 skipped.
- Android Lint: passed with **0 errors** and 22 non-blocking warnings.
- Debug APK assembly: passed.
- APK output: `app/build/outputs/apk/debug/app-debug.apk`.
- Git whitespace/error check: passed; only existing Windows line-ending notices
  were reported.

## Production-readiness, testing, accessibility, and submission batch

Completed locally on 31 July 2026.

### Architecture and lifecycle audit

- Inspected the full tracked/untracked project layout and Git history before
  editing.
- Audited every production Kotlin package, repository interface, ViewModel,
  navigation route, Firebase configuration file, test source set, release
  configuration, and existing documentation.
- Removed the remaining production non-null assertions from catalog
  aggregation and registration.
- Added an authenticated destination allow-list for notification and FCM routes.
- Rejected malformed, oversized, traversal-like, external, authentication, and
  unknown remote routes before Navigation Compose receives them.
- Added defense-in-depth route validation when a system notification intent is
  created.
- Restricted remote notification channel IDs to the two declared channels.

### Duplicate action, reconnect, and gallery hardening

- Required the customer to enter the explicit booking-review state before
  submission can start.
- Kept the existing running-request guard so rapid confirm taps create only one
  repository call.
- Preserved previously loaded customer catalog and appointment groups when a
  listener later reports a reconnect/offline error.
- Added saved-data warning cards and retry actions instead of replacing cached
  content with an empty error page.
- Serialized gallery caption saves and deletes.
- Disabled gallery mutation controls while upload, caption save, delete, or
  reorder work is active.
- Added visible progress inside caption/delete confirmations and prevented
  dismissal while their write is running.
- Kept optimistic gallery ordering rollback behavior.

### Privacy and release preparation

- Disabled Android backup and cleartext HTTP.
- Added API 26-30 and API 31+ backup/data-transfer exclusion rules as defense in
  depth for account-mode, session, and device registration state.
- Enabled R8 code shrinking and resource shrinking for release builds.
- Added project ProGuard rules for Firestore reflection models, FCM entry
  points, WorkManager entry points, and useful stack-trace metadata.
- Added ignored signing-secret and release-artifact patterns.
- Added `keystore.properties.example` without creating a real key.
- Moved the large logo to `drawable-nodpi` to prevent density inflation while
  keeping the visual resource unchanged.
- Cleaned unused template colors and modernized mirrored navigation icons.

### Accessibility, responsive UI, and localization preparation

- Added stable semantics/test tags for Home search/catalog/retry, booking form,
  service/date/time choices, review/submit/success, appointment content/cards,
  retry, and confirmation dialogs.
- Marked core section titles as accessibility headings.
- Extracted core customer Home, booking, appointment, bottom-navigation, and
  notification-channel copy into Android string/plural resources.
- Added quantity-aware minute and catalog-count plurals.
- Replaced the wrapping Barber `Availability` bottom label with the responsive
  `Hours` label while preserving route and visual styling.
- Forced bottom labels to one line with safe ellipsis behavior.
- Converted fixed action heights to minimum heights in the booking flow so
  larger font settings can expand controls.
- Added or retained content descriptions for actionable icons and silent
  semantics for decorative icons.

### Automated test expansion

- Added route-policy tests for every authenticated top-level destination,
  generated typed destinations, authentication-route rejection, malformed
  input, external input, and maximum length.
- Added notification payload tests proving unsafe explicit routes are ignored
  or safely fall back to a validated appointment target.
- Added booking tests for pre-review submission rejection and duplicate-submit
  suppression.
- Added catalog and appointment tests proving previously loaded data survives a
  listener failure.
- Added gallery tests for duplicate caption-save and duplicate-delete
  suppression, plus completion-state reset.
- Replaced the placeholder instrumentation test with application identity,
  label, and backup-policy checks.
- Added Compose device-test source for Customer bottom navigation, responsive
  Barber navigation, and the required booking form-to-review progression.

### Firebase emulator and CI preparation

- Added local Auth, Firestore, Storage, and Emulator UI ports to `firebase.json`.
- Added executable Node test source for Firestore user privacy, immutable role,
  barber ownership, appointment privacy, cancelled-history hiding,
  notification ownership/trust, fixed `[120, 30]` reminder settings, and query
  constraints.
- Added executable Storage rule source for ownership, allowed paths, MIME
  types, custom metadata, authenticated reads, and owner-only deletion.
- Added an isolated emulator test package and setup guide. Dependencies were not
  installed and live Firebase was not touched.
- Added GitHub Actions jobs for Android JVM tests, device-test compilation,
  Lint, debug/release APKs, Functions build/tests, Firebase emulator rule tests,
  and report/APK artifacts.

### Submission documentation

- Added a complete project README and architecture document.
- Added privacy policy and Data Safety drafts with explicit publisher,
  retention, and account-deletion placeholders.
- Added permissions explanation, manual QA plan, device matrix, screenshot
  plan, demo script, store-listing copy, and final release checklist.
- Documented the Storage download-token privacy limitation and the absence of
  broad media, camera, location, contacts, microphone, or exact-alarm
  permissions.

### Final local verification

- Debug and release Kotlin compilation: passed.
- JVM unit tests: **291 passed**, 0 failures, 0 errors, 0 skipped across 31
  suites.
- Device/instrumentation test source: **6 tests compiled**. Execution is pending
  because no emulator or physical device was used.
- Firebase emulator rule source: **15 tests prepared**. Execution is pending
  because Node.js/Firebase CLI dependencies were deliberately not installed.
- Cloud Functions source: **29 tests present**. Execution remains pending
  locally because Node.js/npm is unavailable.
- Android Lint: passed with **0 errors**. Remaining warnings are toolchain and
  dependency update notices intentionally not upgraded inside this batch.
- Debug APK: passed at
  `app/build/outputs/apk/debug/app-debug.apk`.
- Minified/resource-shrunk unsigned release APK: passed at
  `app/build/outputs/apk/release/app-release-unsigned.apk`.
- `firebase.json`, `firestore.indexes.json`, `.firebaserc`, and Firebase test
  package JSON parsing: passed.
- Git whitespace check: no whitespace errors; Git reports only Windows
  line-ending conversion notices.
- No Firebase deployment, billing change, dependency installation, emulator,
  device, real signing key, commit, push, PR, or Play Console mutation was
  performed.

### Remaining external work

- Deploy the reviewed Firestore rules and indexes, then repeat real
  Customer/Barber multi-account lifecycle tests.
- Decide Blaze billing, create/enable the Storage bucket, deploy Storage rules
  and Functions, and verify gallery/FCM/server reminders live.
- Install/run the isolated Firebase emulator tests and Functions tests in a
  suitable Node 22 environment or through CI.
- Execute the six Android tests and full manual QA/device matrix on real
  emulator/device profiles.
- Verify TalkBack, 2.0x fonts, compact/tablet/foldable layouts, dark theme,
  offline cold/warm cache, reconnect, FCM app states, and Doze timing.
- Create and secure a real upload key, configure signing secrets, build a signed
  AAB, and install it through Play internal testing.
- Replace privacy/support placeholders, define account deletion/retention,
  capture approved screenshots/video, complete live Data Safety answers, and
  finish Play Console submission.

## Split-shift and break-hours batch

Completed locally on 31 July 2026.

- Replaced the one-continuous-shift limitation with up to six ordered work
  periods per weekday; gaps are treated as barber breaks.
- Added barber Availability controls to add, edit, and remove work periods
  while preserving the established card design and 24-hour time input.
- Added schema-versioned Firestore serialization with a pure codec and automatic
  legacy migration. Existing `startTime`/`endTime` documents still load as one
  period, and the next save writes `schemaVersion: 2` plus `workingPeriods`.
- Kept legacy fields aligned to only the first period so older app builds do
  not incorrectly expose an entire break as bookable time.
- Updated customer-facing shop hours, next-available copy, and representative
  times to understand multiple daily periods.
- Updated slot generation so appointments are produced independently inside
  each period and never span a break.
- Added transaction-time schedule validation to both new bookings and
  rescheduling. A stale client is rejected if the barber added a break after
  that client loaded availability.
- Expanded readiness validation for malformed, reversed, overlapping,
  out-of-order, and excessive periods.
- Tightened Firestore availability writes to the version-2 top-level schema and
  added emulator tests for owner writes, customer denial, old schema denial,
  and unknown-field denial.
- Added codec, validation, slot-generation, and ViewModel tests covering the
  exact `09:00-12:00`, `14:00-16:00`, `16:30-19:00` example.
- Updated Firebase setup and manual QA documentation with migration, deployment,
  visible testing, and stale-booking checks.

Local unit verification after this feature: **300 JVM tests passed**, with no
failures, errors, or skipped tests. Full Lint/APK/release verification is
recorded in the final handoff after the complete working tree is rebuilt.

The updated Firestore rules still require manual deployment. No Firebase
deployment, commit, or push was performed.

The consolidated current change list and the remaining work that does not
require upgraded Firebase are saved in `docs/WHAT_WE_CHANGED.md`.

## Autonomous non-Blaze closeout (31 July 2026)

This section supersedes the earlier pending local-verification items above.

### Completed locally

- Disabled the bundled sample barber catalog in release builds while retaining
  the clearly labelled debug-only preview. A release build now reports an empty
  real catalog or a real listener error instead of masking it with sample data.
- Added optional ignored release-signing configuration through
  `keystore.properties.example`; release builds remain safely unsigned until
  the publisher supplies a real upload key.
- Added a Spark-compatible in-app account-deletion request flow, protected
  `accountDeletionRequests/{uid}` rules, owner status observation, duplicate
  submission protection, five ViewModel tests, three rule tests, an operations
  runbook, and a ready-to-host deletion information page.
- Completed the Compose localization-preparation pass: direct hard-coded
  `Text` copy and hard-coded accessibility descriptions were moved to Android
  string resources. Corrected corrupted ellipsis, apostrophe, bullet, and
  shekel resource encoding.
- Fixed Firebase Functions compilation against the installed Firebase Admin
  typings by using the shared Firestore instance.
- Made Firebase emulator files run serially so isolated setup cannot clear data
  belonging to another test file. Read/delete rule tests now seed objects with
  rules disabled and test only the intended operation.
- Added reviewed pnpm lockfiles and deterministic pnpm-based GitHub Actions
  installation for Functions and Firebase rule-test jobs.
- Ignored the local pnpm store and Firebase emulator output.

### Final verified results

- Debug and release Kotlin compilation: passed.
- JVM unit tests: **306 passed**, 0 failures, 0 errors, 0 skipped across 33
  suites.
- Android instrumentation and Compose tests: **6 passed** on the headless
  `Pixel_10_Pro_XL` API 37 emulator, 0 failed and 0 skipped.
- Firestore and Storage emulator security tests: **21 passed**, 0 failed and 0
  skipped.
- Cloud Functions logic tests: **29 passed**, 0 failed and 0 skipped; TypeScript
  type-check/build passed. The locally bundled Node runtime was 24, while CI is
  pinned to the declared Node 22 runtime.
- Android Lint: passed with **0 errors** and 11 reviewed dependency/toolchain
  update notices.
- Debug APK: built successfully.
- Minified/resource-shrunk unsigned release APK and unsigned release AAB: built successfully.
- Generated BuildConfig check: development catalog is `true` for debug and
  `false` for release.
- No live Firebase deployment, billing change, commit, push, or Play Console
  mutation was performed.

### Remaining non-Blaze work

The code-complete local batch is finished. These remaining items need the
publisher, live Firebase console, real accounts, signing ownership, or Play
Console access rather than additional unattended implementation:

1. Deploy reviewed Firestore rules and indexes to `cuttime-b1fa1`, wait for
   every index to show **Enabled**, and run two-account Customer/Barber smoke
   tests.
2. Confirm the final account-deletion retention policy, replace
   `[SUPPORT_EMAIL]`, and publish `docs/account-deletion.html` plus the privacy
   policy on durable HTTPS URLs.
3. Create and securely back up a real upload key, create ignored
   `keystore.properties`, generate a signed AAB, and test it through Play
   internal testing.
4. Complete the documented physical-device/accessibility/device matrix,
   including TalkBack, 2.0x font, offline/reconnect, process death, battery
   saver, and real notification timing.
5. Capture final screenshots/demo video, replace publisher/support/privacy
   placeholders, complete Data Safety/content rating/target audience decisions,
   and finish Play Console submission.
6. Review the working-tree diff, commit, push, and verify the Node 22 GitHub
   Actions run.

Live Firebase Storage gallery hosting and deployed scheduled Cloud
Functions/server FCM reminders remain the separate Blaze-dependent workstream.

### Current completion estimate

- Application code and locally testable behavior: **about 94%**.
- Full release/submission readiness including Firebase deployment, signing,
  real-device QA, policy decisions, store assets, and Play Console: **about
  84%**.

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