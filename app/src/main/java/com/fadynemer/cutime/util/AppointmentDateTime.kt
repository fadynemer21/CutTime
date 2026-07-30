package com.fadynemer.cutime.util

import com.fadynemer.cutime.model.BookingRequest
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

object AppointmentDateTime {

    private val isoDateFormatter =
        DateTimeFormatter.ISO_LOCAL_DATE

    private val timeFormatter =
        DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

    private val displayDateFormatter =
        DateTimeFormatter.ofPattern(
            "EEEE, d MMMM yyyy",
            Locale.ENGLISH
        )

    fun toStartMillis(
        date: String,
        time: String,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val localDate = LocalDate.parse(date, isoDateFormatter)
        val localTime = LocalTime.parse(time, timeFormatter)

        return LocalDateTime
            .of(localDate, localTime)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    fun formatDateForDisplay(date: String): String {
        return try {
            LocalDate
                .parse(date, isoDateFormatter)
                .format(displayDateFormatter)
        } catch (_: DateTimeParseException) {
            date
        }
    }

    fun isFuture(
        date: String,
        time: String,
        clock: Clock = Clock.systemDefaultZone()
    ): Boolean {
        return try {
            val startMillis =
                toStartMillis(date, time, clock.zone)

            startMillis > clock.millis()
        } catch (_: DateTimeParseException) {
            false
        }
    }

    fun validate(
        request: BookingRequest,
        clock: Clock = Clock.systemDefaultZone()
    ): String? {
        return when {
            request.barberId.isBlank() ->
                "The selected barber is invalid."

            request.serviceId.isBlank() ||
                request.serviceName.isBlank() ->
                "The selected service is invalid."

            request.price < 0 ->
                "The service price is invalid."

            request.durationMinutes <= 0 ->
                "The service duration is invalid."

            !isFuture(
                request.appointmentDate,
                request.appointmentTime,
                clock
            ) ->
                "Choose an appointment time in the future."

            else -> null
        }
    }

    fun slotDocumentId(
        barberId: String,
        date: String,
        time: String
    ): String {
        val cleanBarberId =
            barberId.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val cleanTime = time.replace(":", "-")

        return "${cleanBarberId}_${date}_$cleanTime"
    }

    fun slotDocumentIds(
        barberId: String,
        date: String,
        time: String,
        durationMinutes: Int,
        intervalMinutes: Int = 15
    ): List<String> {
        require(durationMinutes > 0)
        require(intervalMinutes > 0)

        return reservedTimes(
            time = time,
            durationMinutes = durationMinutes,
            intervalMinutes = intervalMinutes
        ).map { slotTime ->
            slotDocumentId(
                barberId = barberId,
                date = date,
                time = slotTime
            )
        }
    }

    fun reservedTimes(
        time: String,
        durationMinutes: Int,
        intervalMinutes: Int = 15
    ): List<String> {
        require(durationMinutes > 0)
        require(intervalMinutes > 0)

        val startTime = LocalTime.parse(time, timeFormatter)
        val slotCount =
            (durationMinutes + intervalMinutes - 1) / intervalMinutes

        return (0 until slotCount).map { slotIndex ->
            startTime
                .plusMinutes(
                    slotIndex.toLong() * intervalMinutes
                )
                .format(timeFormatter)
        }
    }
}
