# CuTime development progress

This document records completed autonomous development batches and the remaining
roadmap. It is intentionally updated before new batch summaries are delivered.

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
