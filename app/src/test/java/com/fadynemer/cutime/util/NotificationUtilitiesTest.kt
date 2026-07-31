package com.fadynemer.cutime.util

import com.fadynemer.cutime.model.NotificationType
import com.fadynemer.cutime.model.UserNotification
import com.fadynemer.cutime.navigation.AppRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class NotificationUtilitiesTest {
    private fun notification(
        type: NotificationType = NotificationType.GENERAL,
        appointmentId: String? = null,
        barberId: String? = null
    ) = UserNotification(
        id = "notification_1",
        userId = "user_1",
        type = type,
        title = "Title",
        message = "Message",
        appointmentId = appointmentId,
        barberId = barberId
    )

    @Test
    fun customerAppointmentNotificationUsesCustomerDetails() {
        assertEquals(
            AppRoute.CustomerAppointmentDetail.create("a 1"),
            NotificationRouter.destination(
                notification(appointmentId = "a 1"),
                isBarberMode = false
            )
        )
    }

    @Test
    fun barberAppointmentNotificationUsesBarberDetails() {
        assertEquals(
            AppRoute.BarberAppointmentDetail.create("a 1"),
            NotificationRouter.destination(
                notification(appointmentId = "a 1"),
                isBarberMode = true
            )
        )
    }

    @Test
    fun appointmentAlwaysTakesPriorityOverBarberProfile() {
        assertEquals(
            AppRoute.CustomerAppointmentDetail.create("appointment"),
            NotificationRouter.destination(
                notification(
                    appointmentId = "appointment",
                    barberId = "barber"
                ),
                isBarberMode = false
            )
        )
    }

    @Test
    fun generalBarberNotificationOpensPublicProfile() {
        assertEquals(
            AppRoute.BarberProfile.create("barber 1"),
            NotificationRouter.destination(
                notification(barberId = "barber 1"),
                isBarberMode = false
            )
        )
    }

    @Test
    fun nonGeneralNotificationWithoutTargetReturnsHome() {
        assertEquals(
            AppRoute.CustomerHome.pattern,
            NotificationRouter.destination(
                notification(
                    type = NotificationType.REVIEW_REQUEST
                ),
                isBarberMode = false
            )
        )
    }

    @Test
    fun missingBarberTargetReturnsDashboardInBarberMode() {
        assertEquals(
            AppRoute.BarberDashboard.pattern,
            NotificationRouter.destination(
                notification(),
                isBarberMode = true
            )
        )
    }

    @Test
    fun explicitPayloadRouteWins() {
        assertEquals(
            "appointment/explicit",
            NotificationRouter.destinationFromPayload(
                mapOf(
                    "route" to "appointment/explicit",
                    "appointmentId" to "ignored"
                )
            )
        )
    }

    @Test
    fun unsafeExplicitPayloadFallsBackToAppointmentTarget() {
        assertEquals(
            AppRoute.CustomerAppointmentDetail.create("safe"),
            NotificationRouter.destinationFromPayload(
                mapOf(
                    "route" to "login",
                    "appointmentId" to "safe"
                )
            )
        )
    }

    @Test
    fun unsafeExplicitPayloadWithoutTargetIsIgnored() {
        assertNull(
            NotificationRouter.destinationFromPayload(
                mapOf("route" to "https://example.com")
            )
        )
    }

    @Test
    fun customerPayloadBuildsCustomerRoute() {
        assertEquals(
            AppRoute.CustomerAppointmentDetail.create("one two"),
            NotificationRouter.destinationFromPayload(
                mapOf("appointmentId" to "one two")
            )
        )
    }

    @Test
    fun barberPayloadBuildsBarberRoute() {
        assertEquals(
            AppRoute.BarberAppointmentDetail.create("one two"),
            NotificationRouter.destinationFromPayload(
                mapOf(
                    "appointmentId" to "one two",
                    "audience" to "BARBER"
                )
            )
        )
    }

    @Test
    fun emptyPayloadHasNoDestination() {
        assertNull(
            NotificationRouter.destinationFromPayload(emptyMap())
        )
    }

    @Test
    fun blankPayloadValuesHaveNoDestination() {
        assertNull(
            NotificationRouter.destinationFromPayload(
                mapOf("route" to " ", "appointmentId" to "")
            )
        )
    }

    @Test
    fun tokenHashIsStableAndSha256Length() {
        val first = DeviceTokenHasher.documentId("token-value")
        val second = DeviceTokenHasher.documentId("token-value")

        assertEquals(first, second)
        assertEquals(64, first.length)
        assertTrue(first.matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun differentTokensProduceDifferentIds() {
        assertNotEquals(
            DeviceTokenHasher.documentId("token-one"),
            DeviceTokenHasher.documentId("token-two")
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankTokenIsRejected() {
        DeviceTokenHasher.documentId("  ")
    }

    @Test
    fun zeroTimestampFormatsAsJustNow() {
        assertEquals(
            "Just now",
            NotificationTimeFormatter.format(0)
        )
    }

    @Test
    fun todayTimestampGetsTodayLabel() {
        val clock = Clock.fixed(
            Instant.parse("2026-07-30T12:00:00Z"),
            ZoneOffset.UTC
        )
        val timestamp =
            Instant.parse("2026-07-30T08:15:00Z").toEpochMilli()

        assertEquals(
            "Today at 08:15",
            NotificationTimeFormatter.format(timestamp, clock)
        )
    }

    @Test
    fun yesterdayTimestampGetsYesterdayLabel() {
        val clock = Clock.fixed(
            Instant.parse("2026-07-30T12:00:00Z"),
            ZoneOffset.UTC
        )
        val timestamp =
            Instant.parse("2026-07-29T20:45:00Z").toEpochMilli()

        assertEquals(
            "Yesterday at 20:45",
            NotificationTimeFormatter.format(timestamp, clock)
        )
    }

    @Test
    fun olderTimestampGetsFullDate() {
        val clock = Clock.fixed(
            Instant.parse("2026-07-30T12:00:00Z"),
            ZoneOffset.UTC
        )
        val timestamp =
            Instant.parse("2026-07-10T09:05:00Z").toEpochMilli()

        assertEquals(
            "10 Jul 2026, 09:05",
            NotificationTimeFormatter.format(timestamp, clock)
        )
    }

    @Test
    fun dayGroupsCoverExpectedBuckets() {
        val zone = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 7, 30)

        fun millis(date: String) =
            Instant.parse("${date}T10:00:00Z").toEpochMilli()

        assertEquals(
            "Today",
            NotificationTimeFormatter.dayGroup(
                millis("2026-07-30"),
                zone,
                today
            )
        )
        assertEquals(
            "Yesterday",
            NotificationTimeFormatter.dayGroup(
                millis("2026-07-29"),
                zone,
                today
            )
        )
        assertEquals(
            "This week",
            NotificationTimeFormatter.dayGroup(
                millis("2026-07-25"),
                zone,
                today
            )
        )
        assertEquals(
            "Earlier",
            NotificationTimeFormatter.dayGroup(
                millis("2026-07-01"),
                zone,
                today
            )
        )
    }
}
