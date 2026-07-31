# CuTime architecture

## Overview

CuTime uses a pragmatic presentation/domain/data split inside one Android
application module. Compose screens render immutable UI-state data classes and
send user intent to ViewModels. ViewModels enforce duplicate-action guards,
validation order, listener lifecycle, and readable error state. Repository
interfaces isolate Firebase callbacks so policy and ViewModel behavior can be
covered by local JVM tests.

```text
Compose screen
    -> ViewModel and UI state
        -> repository interface
            -> Firebase Authentication / Firestore / Storage / FCM
        -> pure policy and formatting utilities
    -> typed AppRoute builder
```

## Packages

- `screens`: customer, barber, authentication, booking, appointment, rating,
  gallery, and notification Compose screens.
- `components`: shared cards, bottom navigation, management scaffolds, and
  notification controls.
- `viewmodel`: observable UI states, listener ownership, validation, retries,
  and action serialization.
- `repository`: Firebase access, Firestore transactions, Storage operations,
  Authentication operations, and interfaces used by tests.
- `model`: typed app entities and lifecycle values.
- `util`: slot generation, grouping, validation, date/time conversion,
  notification routing, hashing, policies, and stable UI test tags.
- `navigation`: route definitions, encoded builders, remote-route allow-list,
  and the Compose `NavHost`.
- `notifications`: notification channels, FCM service, token registration,
  system publishing, WorkManager scheduling, worker, and reminder planning.
- `data`: development fallback catalog and selected-catalog process cache.

## Authentication and roles

Firebase Authentication owns credentials and persistent sessions. The Firestore
document `users/{uid}` stores the immutable role and customer-visible full name.
Firestore rules prevent a client from changing its UID, role, or email.

Barber Customer Mode is an interface preference stored per UID in private
Android preferences. It never rewrites the authoritative `BARBER` role.
Android backup is disabled so this local account-mode state is not restored onto
another installation.

## Real barber readiness

A customer-visible Firestore shop is an aggregate of:

1. `barberProfiles/{barberId}` with valid shop name and description.
2. At least one valid `barberProfiles/{barberId}/services/{serviceId}`.
3. `barberAvailability/{barberId}` with seven days and at least one open day.

Incomplete profiles stay out of the customer catalog. The clearly labelled
sample catalog is a safe development fallback and cannot submit bookings.

## Booking concurrency

The visible time list is generated from weekly working hours, blocked dates,
service duration, current time, and occupied 15-minute lock segments.
Availability filtering improves the experience but is not the authority.

Appointment creation runs in a Firestore transaction:

1. Resolve authenticated customer identity and profile name.
2. Read the selected service, barber profile, availability, and proposed slot
   locks.
3. Revalidate shop identity, service price/duration, future time, holiday
   status, and lock availability.
4. Create the appointment and every required `bookingSlots` document.

Concurrent attempts cannot both create the same lock. Rescheduling follows the
same principle, releasing old locks and reserving new locks in one transaction.

## Appointment lifecycle

The valid status flow is:

```text
UPCOMING -> CANCELLED
UPCOMING -> COMPLETED (owning barber, after appointment end)
```

Completed and cancelled records are retained. A customer history removal is a
soft-hide field available only on that customer's cancelled record. It does not
delete the barber audit record.

Saving newly blocked barber dates finds affected upcoming appointments, changes
them to cancelled in rule-safe batches, and releases their locks. Chunk sizing
accounts for both the appointment update and each lock deletion.

## Ratings

Ratings use the appointment ID as the rating ID, enforcing one rating per
appointment. A transaction verifies that the authenticated customer owns a
completed appointment, creates the rating, links the appointment, and updates
the barber count/sum/average. Security rules cross-check the linked writes.

## Offline and retry behavior

Firestore provides its local client cache. Listener-backed customer catalog,
appointments, galleries, notifications, and management screens expose explicit
retry paths. When a catalog or appointment listener fails after data was
loaded, the ViewModel retains that data and the UI marks it as saved/stale
instead of presenting an empty screen.

Transactions and file uploads require connectivity to complete. Buttons are
guarded while actions are running, failures leave the user on a recoverable
screen, and transaction-side validation remains authoritative after reconnect.

## Notifications

In-app notifications are trusted records created by Cloud Functions. Clients
may read, acknowledge, or delete only their own records. Device tokens are
stored under their owning user and removed on logout when possible.

Two reminder paths coexist:

- WorkManager schedules on-device two-hour and 30-minute reminders from the
  live upcoming list.
- A scheduled trusted Function can deliver cross-device reminders using each
  user's notification preferences.

Unique work names and deterministic server notification IDs make replacement
and function retries idempotent. Remote routes are validated by
`AppRoutePolicy` before they reach Navigation Compose.

## Gallery

The system document picker supplies a content URI. The app validates type,
size, caption, and image-count limits, uploads to
`barberGalleries/{barberId}/{imageId}`, then persists Firestore metadata. If
metadata creation fails, binary cleanup is attempted. Reordering is optimistic
and rolls back in UI state after a failed batch.

Storage download-token URLs should be treated as bearer URLs. The UI shares
them only inside authenticated app experiences, but a recipient could copy a
URL. This limitation must be considered in the final privacy review.

## Security boundaries

- Mobile input is untrusted; Firestore and Storage rules enforce ownership and
  immutable fields.
- Cloud Functions use Admin SDK privileges and therefore validate documents
  before constructing content.
- Notification deep links are untrusted remote input and are allow-listed.
- No cleartext HTTP is permitted.
- Android application backup is disabled.
- Signing secrets and Firebase emulator output are ignored by Git.

## Testing strategy

- Pure JVM tests cover domain policies, validators, route construction,
  reminders, grouping, and ViewModel state machines.
- Compose device-test source verifies core navigation labels and the required
  booking-review progression.
- Instrumented tests verify application identity, label, and backup policy.
- Firebase emulator source validates Firestore and Storage ownership rules.
- Cloud Functions pure TypeScript tests validate notification content and
  transitions.
- CI compiles device tests and builds debug and unsigned minified release APKs.

