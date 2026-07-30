# CuTime development progress

This document records completed autonomous development batches and the remaining
roadmap. It is intentionally updated before new batch summaries are delivered.

## Current completion estimate

Estimated overall completion after the gallery and notification mega-batch:
**90-93%**.

The core product, cross-role lifecycle, real catalog, gallery, in-app
notifications, push source, and local/server reminder architecture are now
implemented. The remaining work is primarily emulator/device validation,
production Firebase deployment, accessibility/offline hardening, release
configuration, and submission assets.

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
