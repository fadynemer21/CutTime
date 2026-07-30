package com.fadynemer.cutime.notifications

import com.fadynemer.cutime.model.AppointmentStatus
import com.fadynemer.cutime.model.NotificationPreferenceLimits
import com.fadynemer.cutime.viewmodel.testAppointment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ReminderPlannerTest {
    private val nowMillis = 2_000_000_000_000L
    private val clock = Clock.fixed(
        Instant.ofEpochMilli(nowMillis),
        ZoneOffset.UTC
    )

    @Test
    fun configuredScheduleContainsTwoHoursAndThirtyMinutes() {
        assertEquals(
            listOf(120, 30),
            NotificationPreferenceLimits.REMINDER_LEAD_MINUTES
        )
    }

    @Test
    fun sufficientlyFutureAppointmentProducesBothPlans() {
        val appointment = testAppointment(
            startAtMillis = nowMillis + 3 * 60 * 60_000L,
            endAtMillis =
                nowMillis + 3 * 60 * 60_000L + 30 * 60_000L
        )

        val plans = ReminderPlanner.plans(appointment, clock)

        assertEquals(2, plans.size)
        assertEquals(
            listOf(120, 30),
            plans.map { it.leadMinutes }
        )
        assertEquals(
            listOf(
                nowMillis + 60 * 60_000L,
                nowMillis + 150 * 60_000L
            ),
            plans.map { it.triggerAtMillis }
        )
        assertTrue(plans[0].message.contains("2 hours"))
        assertTrue(plans[1].message.contains("30 minutes"))
    }

    @Test
    fun appointmentInsideTwoHoursStillGetsThirtyMinutePlan() {
        val appointment = testAppointment(
            startAtMillis = nowMillis + 90 * 60_000L,
            endAtMillis =
                nowMillis + 120 * 60_000L
        )

        val plans = ReminderPlanner.plans(appointment, clock)

        assertEquals(1, plans.size)
        assertEquals(30, plans.single().leadMinutes)
        assertEquals(
            nowMillis + 60 * 60_000L,
            plans.single().triggerAtMillis
        )
        assertTrue(plans.single().message.contains("30 minutes"))
    }

    @Test
    fun completedAppointmentProducesNoConfiguredPlans() {
        val plans = ReminderPlanner.plans(
            testAppointment(
                status = AppointmentStatus.COMPLETED,
                startAtMillis = nowMillis + 3 * 60 * 60_000L
            ),
            clock
        )

        assertTrue(plans.isEmpty())
    }

    @Test
    fun upcomingFutureAppointmentProducesPlan() {
        val appointment = testAppointment(
            startAtMillis = nowMillis + 2 * 60 * 60_000L,
            endAtMillis =
                nowMillis + 2 * 60 * 60_000L + 30 * 60_000L
        )

        val plan = ReminderPlanner.plan(
            appointment = appointment,
            minutesBefore = 60,
            clock = clock
        )

        requireNotNull(plan)
        assertEquals(appointment.id, plan.appointmentId)
        assertEquals(
            nowMillis + 60 * 60_000L,
            plan.triggerAtMillis
        )
        assertEquals(60 * 60_000L, plan.delayMillis)
        assertEquals("Upcoming appointment", plan.title)
        assertTrue(plan.message.contains("Classic Haircut"))
        assertTrue(plan.message.contains("Urban Fade Studio"))
        assertTrue(plan.message.contains("1 hour"))
    }

    @Test
    fun completedAppointmentDoesNotProducePlan() {
        assertNull(
            ReminderPlanner.plan(
                testAppointment(
                    status = AppointmentStatus.COMPLETED,
                    startAtMillis = nowMillis + 10_000_000L
                ),
                minutesBefore = 60,
                clock = clock
            )
        )
    }

    @Test
    fun cancelledAppointmentDoesNotProducePlan() {
        assertNull(
            ReminderPlanner.plan(
                testAppointment(
                    status = AppointmentStatus.CANCELLED,
                    startAtMillis = nowMillis + 10_000_000L
                ),
                minutesBefore = 60,
                clock = clock
            )
        )
    }

    @Test
    fun zeroLeadTimeDoesNotProducePlan() {
        assertNull(
            ReminderPlanner.plan(
                testAppointment(
                    startAtMillis = nowMillis + 10_000_000L
                ),
                minutesBefore = 0,
                clock = clock
            )
        )
    }

    @Test
    fun negativeLeadTimeDoesNotProducePlan() {
        assertNull(
            ReminderPlanner.plan(
                testAppointment(
                    startAtMillis = nowMillis + 10_000_000L
                ),
                minutesBefore = -30,
                clock = clock
            )
        )
    }

    @Test
    fun reminderAtCurrentMomentIsNotScheduled() {
        val appointment = testAppointment(
            startAtMillis = nowMillis + 60 * 60_000L
        )

        assertNull(
            ReminderPlanner.plan(
                appointment,
                minutesBefore = 60,
                clock = clock
            )
        )
    }

    @Test
    fun reminderInPastIsNotScheduled() {
        val appointment = testAppointment(
            startAtMillis = nowMillis + 30 * 60_000L
        )

        assertNull(
            ReminderPlanner.plan(
                appointment,
                minutesBefore = 60,
                clock = clock
            )
        )
    }

    @Test
    fun halfHourLeadTimeIsFormatted() {
        assertEquals(
            "30 minutes",
            ReminderPlanner.formatLeadTime(30)
        )
    }

    @Test
    fun oneHourUsesSingularGrammar() {
        assertEquals(
            "1 hour",
            ReminderPlanner.formatLeadTime(60)
        )
    }

    @Test
    fun twoHoursUsesPluralGrammar() {
        assertEquals(
            "2 hours",
            ReminderPlanner.formatLeadTime(120)
        )
    }

    @Test
    fun fullDayIsExpressedInHours() {
        assertEquals(
            "24 hours",
            ReminderPlanner.formatLeadTime(1440)
        )
    }

    @Test
    fun mixedHoursAndMinutesAreFormattedCompactly() {
        assertEquals(
            "1h 30m",
            ReminderPlanner.formatLeadTime(90)
        )
    }
}
