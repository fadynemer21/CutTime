# CuTime manual QA plan

## Test data

Prepare:

- Customer A and Customer B with distinct names/emails.
- Barber A with a complete shop, two services, seven availability days, one
  blocked future date, and gallery images if Storage is enabled.
- Barber B with an incomplete setup to verify it is hidden from customers.
- At least one upcoming, cancelled, and completed appointment.
- A completed unrated appointment and a completed rated appointment.

Use fictional data. Record UIDs only in a private test worksheet.

## Authentication

- Register valid Customer and Barber accounts.
- Reject blank, invalid email, weak password, password mismatch, and missing
  role.
- Verify duplicate submit does not create duplicate users.
- Login with valid credentials and reject invalid credentials readably.
- Send password reset.
- Kill/relaunch and confirm role/session restoration.
- Logout and confirm protected navigation cannot be restored with Back.
- Login as another account on the same device and confirm no prior notification
  destination or Customer Mode leaks.

## Barber readiness

- New incomplete Barber account does not appear on customer Home.
- Save profile only: remains hidden.
- Add service only: remains hidden until availability is complete/open.
- Save complete week: shop appears live.
- Close every day: shop becomes non-bookable/hidden according to readiness.
- Edit shop name and confirm customer profile updates.
- Validate service name, positive price, and 15-minute duration intervals.

## Booking

- Development fallback card cannot book.
- Real shop shows selectable services and seven date choices.
- Closed days and blocked holidays show no times.
- Past times do not appear on today.
- Long service cannot start too close to closing.
- Occupied segments remove every overlapping start.
- Summary does not appear after only service/date selection.
- Review button stays disabled until service/date/time are selected.
- Review shows correct barber, service, duration, date, time, and total.
- Double-tap Confirm creates one request/appointment.
- Two customers race for the same time: exactly one succeeds.
- Offline submit shows a recoverable message; reconnect and retry succeeds once.

## Customer appointments

- New appointment appears in Upcoming without relaunch.
- Detail matches list and booking summary.
- Customer can cancel own upcoming appointment; slot becomes available again.
- Customer cannot cancel another customer's appointment.
- Reschedule reserves new slots and releases old slots atomically.
- Failed/conflicting reschedule leaves old appointment and locks unchanged.
- Listener failure after loaded data shows saved appointments and Retry.
- Long-press only a cancelled appointment opens Delete from History.
- History removal hides only the customer's copy; Barber still sees the record.

## Barber appointments and holidays

- Dashboard uses customer full name, not email, for new appointments.
- Legacy email-like appointment name is repaired when customer profile is
  readable.
- Barber sees only own appointments.
- Barber can cancel own upcoming appointment.
- Complete remains prohibited before end time and works after end time.
- Add a holiday containing upcoming appointments.
- Confirm the warning/count, save, and verify all affected appointments become
  cancelled and every lock is released.
- Unaffected dates remain upcoming.
- Partial batch/network failure produces a recoverable error and no silent
  success.

## Ratings

- Only owner of completed appointment can open/submit a rating.
- Zero stars cannot submit.
- Stars clamp to 1–5 and review stops at 500 characters.
- One appointment creates one immutable rating.
- Retry/double-tap cannot increment aggregate twice.
- Count, sum, and average match the rating set.
- Customer and Barber profile review displays update live.

## Gallery

- Pick JPEG, PNG, WebP, HEIC, and HEIF files under 8 MiB.
- Reject unsupported type, zero-byte, and over-limit files.
- Reject a 13th image.
- Show upload progress and recover from network loss.
- Metadata failure attempts binary cleanup.
- Caption length and update behavior are correct.
- Reorder succeeds and rolls back visibly on failure.
- Delete missing object still cleans stale metadata.
- Customer full-size preview opens/closes with meaningful controls.
- Customer/other Barber cannot write another shop's Storage path.

## Notifications

- Android 13+ grant and deny flows.
- In-app inbox works when system permission is denied.
- New booking notifies appropriate customer and Barber if enabled.
- Cancel, reschedule, complete, review request, and new review content/routes.
- Mark read, mark all read, and delete are owner-only.
- Badge counts unread items and updates live.
- Preferences persist and fixed lead schedule remains `[120, 30]`.
- Foreground, background, terminated, token refresh, and notification tap.
- Logout/login another account does not deliver to the old account.
- Two-hour and 30-minute local reminders both exist for a far-future booking.
- Appointment inside two hours receives only still-future reminders.
- Cancel/reschedule/preferences replace or cancel obsolete work.
- Test Doze/battery saver and document timing tolerance.

## Accessibility and layout

- TalkBack order is logical on every primary screen.
- All actionable icons have a readable label; decorative icons are silent.
- Buttons/cards expose click roles and selected state.
- Touch targets are at least 48 dp.
- Use 1.3× and 2.0× font scale on compact phone.
- No bottom-navigation label wraps; Barber uses “Hours”.
- No content is hidden behind system navigation/IME.
- Error messages are readable and paired with retry/action.
- Contrast is reviewed in light and dark theme.
- Screen rotation/process recreation does not submit actions twice.

## Offline/reconnect

- Launch authenticated while offline with warm Firestore cache.
- Catalog and appointments preserve cached content with a saved-data warning.
- Cold offline launch gives a clear error and Retry.
- Reconnect updates every active listener without duplicate cards.
- Offline profile/service/availability changes fail visibly.

## Split shifts and breaks

- Set Wednesday to `09:00-12:00`, `14:00-16:00`, and `16:30-19:00`; save,
  leave the screen, and confirm all three periods reload.
- As a customer, verify a 30-minute service offers `11:30`, not `11:45`, offers
  `14:00` and `15:30`, not `16:00`, then offers `16:30` through `18:30`.
- Start a booking screen before the barber adds a break. Add/save the break in
  the barber account, then try to submit the now-stale break time. Confirm the
  booking transaction rejects it as unavailable.
- Reschedule an appointment and confirm no break time appears. Confirm a stale
  reschedule attempt into a newly added break is also rejected.
- Enter overlapping or reversed periods and confirm Save shows a validation
  error. Remove an added period and confirm the remaining periods are intact.
- Repeated Retry does not leave multiple listeners.
- Network switch during transaction does not duplicate appointment/rating.

## Exit criteria

- All P0 device-matrix rows pass.
- No open Lint errors or release R8 errors.
- JVM, Compose compilation, Functions, and emulator rule tests pass.
- No security-rule bypass in the documented emulator matrix.
- No blocker/high-severity bug remains.
- Privacy/Data Safety answers match the final deployed build.
- Signed AAB installs through Play internal testing.
