# CuTime Firebase setup

The Android app is connected to the existing Firebase project through
`app/google-services.json`. The repository now also contains version-controlled
Firestore rules and index configuration for booking, rescheduling, ratings,
real barber discovery, customer profiles, and barber management. The same
configuration protects barber-owned profiles, services, weekly availability,
cross-role appointment actions, and transaction-owned slot locks.

## Configuration files

- `firestore.rules` protects users, appointments, booking-slot locks, ratings,
  barber profiles, services, and availability.
- `firestore.indexes.json` defines appointment, occupied-slot, and review
  query indexes.
- `firebase.json` connects those files to the Firebase CLI.

## Deployment required before live booking tests

No Firebase configuration is deployed automatically by the Android build.
Install the Firebase CLI if it is not already available. From the project
root, sign in and deploy to the existing CuTime project configured in
`.firebaserc`:

```powershell
firebase login
firebase deploy --only firestore:rules,firestore:indexes
```

To confirm the selected project before deployment, use:

```powershell
firebase use
```

## Collections created by the app

No collections need to be created manually.

- `appointments/{appointmentId}` stores the customer-visible appointment.
- `bookingSlots/{barberId_date_time}` stores 15-minute collision locks.
- `barberProfiles/{barberId}` stores the public shop profile.
- `barberProfiles/{barberId}/services/{serviceId}` stores bookable services.
- `barberAvailability/{barberId}` stores weekly hours and blocked dates.
- `ratings/{appointmentId}` stores the single allowed rating for a completed
  appointment.

Appointment creation and all required slot locks are written in one Firestore
transaction. A service reserves every 15-minute segment covered by its duration,
so overlapping appointments fail even when their start times differ.

## Manual verification after deployment

1. Sign in to the Android app with a Customer account.
2. Open a barber, choose a service, date, and time, and confirm the booking.
3. Confirm that the success screen appears.
4. Open My Appointments and confirm the new booking is under Upcoming.
5. Attempt the same barber/time from a second Customer account.
6. Confirm that the second attempt reports that the time was already booked.

Home now reads real barber profiles, services, availability, and rating
aggregates from Firestore. If Firestore has no complete barber profile or the
catalog listener fails, the app shows the bundled development sample data and a
visible `Development data` notice. This fallback is for development continuity,
not production seeding. Its profile remains browsable, but booking is disabled
so preview IDs can never create live Firestore appointments.

## Barber management verification

1. Sign in with a Barber account and open Profile.
2. Save a shop name and description.
3. Add, edit, and remove a service.
4. Change weekly hours and add a blocked date.
5. Return to Dashboard and confirm all tabs remain accessible.
6. In Firestore, confirm every created document uses the signed-in Barber UID.

## Appointment lifecycle verification

1. As a Customer, cancel an upcoming appointment and confirm its slot documents
   are removed.
2. Book the released time again and confirm it succeeds.
3. As the owning Barber, mark an appointment completed.
4. Confirm another Customer and another Barber cannot change that appointment.
5. Switch the Barber account into Customer Mode, restart the app, and confirm
   it returns to the customer interface while the Firestore role stays `BARBER`.

## Real catalog verification

1. Sign in as a Barber and save a valid public profile.
2. Add at least one service and configure weekly availability.
3. Sign out, then sign in as a Customer.
4. Confirm the barber appears on Home without the development-data notice.
5. Open the barber and confirm live services, hours, price, and availability.
6. Change the profile from the Barber account and confirm a Customer Home
   listener receives the update.

The Barber Dashboard now contains a live Shop Setup card. It reports the shop
as bookable only when the following Firebase documents are complete and use the
same Barber Authentication UID:

```text
users/{barberUid}
barberProfiles/{barberUid}
barberProfiles/{barberUid}/services/{serviceId}
barberAvailability/{barberUid}
```

The profile requires a valid shop name and description. At least one service
must have a positive price and a positive 15-minute-multiple duration.
Availability must contain all seven days and at least one open day whose start
time is before its end time.

Gallery images are optional and do not block publication.

If the Dashboard reports `Your shop is live` but a Customer still sees
development previews, verify that:

1. The Customer and Barber use the same Firebase project configuration.
2. The deployed Firestore rules permit signed-in catalog reads.
3. The four document paths above contain matching Barber UIDs.
4. The Customer device has network connectivity.
5. Logcat does not contain a Firestore permission or missing-index error.

## Occupied-slot and rescheduling verification

1. Book a 30-minute service and inspect `bookingSlots`; two 15-minute locks
   should exist.
2. Return to booking for the same barber/date. Confirm all appointment starts
   that would overlap either lock are absent.
3. Open My Appointments, select the booking, and choose Reschedule.
4. Choose a free time and confirm the same appointment document ID is retained.
5. Confirm obsolete locks are removed and the new duration locks are present.
6. With a second Customer, race for the same free time. Exactly one transaction
   must succeed and the other must show a conflict.

## Rating verification

1. Sign in as the owning Barber after the appointment end time and mark the
   appointment completed.
2. Sign in as the booking Customer, open its details, and tap Rate This Barber.
3. Submit 1-5 stars and an optional written review.
4. Confirm `ratings/{appointmentId}` is created.
5. Confirm the appointment has `ratingId == appointmentId`.
6. Confirm the barber profile increments `ratingCount` and `ratingSum`, updates
   `ratingAverage`, and records `lastRatingId`.
7. Confirm the written review appears on the customer-facing barber profile.
8. Confirm a second submission for the same appointment is refused.

## Customer profile verification

1. Open the Customer Profile tab and tap Edit name.
2. Confirm blank, one-character, multiline, and over-60-character values are
   rejected.
3. Save a valid name.
4. Confirm `users/{uid}.fullName` and the Firebase Authentication display name
   both change while UID, email, and role remain unchanged.

## Firestore rules live-test matrix

After deploying the rules, verify with separate Customer and Barber accounts:

- A customer can read only appointments they own.
- A barber can read only appointments assigned to their UID.
- Neither role can change immutable appointment identity/service/price fields.
- Only the customer can reschedule their upcoming appointment.
- Only the owning customer or barber can cancel an upcoming appointment.
- Only the owning barber can complete an elapsed appointment.
- A rating is accepted only for its owning customer and a completed
  appointment.
- Rating updates/deletes and duplicate rating creates are denied.
- A customer cannot directly replace a barber's rating aggregate values.
- A barber cannot edit another barber's profile, services, or availability.
- User profile name updates cannot change UID, role, or email.

## Gallery, FCM, and Cloud Functions extension

The repository now also contains:

- `storage.rules` for barber-owned gallery objects.
- `functions/` with TypeScript appointment, rating, and reminder triggers.
- Firestore rules for `gallery`, `notifications`, `settings`, and `devices`.
- A composite `appointments(status, startAt)` index used by reminder scans.

See `GALLERY_NOTIFICATIONS_SETUP.md` for the complete enablement, deployment,
security, and live-test guide.

After Node.js 22, npm, and Firebase CLI are installed, the shortest safe
verification and deployment sequence is:

```powershell
cd C:\Users\Fady\Desktop\CutTime\functions
npm install
npm run lint
npm test
cd ..
firebase deploy --only firestore:rules,firestore:indexes,storage,functions
```

The autonomous batch did not run the deploy command and did not modify the live
Firebase project.
