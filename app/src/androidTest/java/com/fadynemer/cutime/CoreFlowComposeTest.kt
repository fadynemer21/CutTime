package com.fadynemer.cutime

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.fadynemer.cutime.components.BarberBottomBar
import com.fadynemer.cutime.components.BarberDestination
import com.fadynemer.cutime.components.CustomerBottomBar
import com.fadynemer.cutime.components.CustomerDestination
import com.fadynemer.cutime.data.SampleBarberData
import com.fadynemer.cutime.model.BarberCatalog
import com.fadynemer.cutime.model.BarberShop
import com.fadynemer.cutime.repository.AppointmentBookingDataSource
import com.fadynemer.cutime.repository.AppointmentObservation
import com.fadynemer.cutime.repository.BarberCatalogDataSource
import com.fadynemer.cutime.screens.BookingScreen
import com.fadynemer.cutime.ui.theme.CutTimeTheme
import com.fadynemer.cutime.util.AvailabilitySlotGenerator
import com.fadynemer.cutime.util.UiTestTags
import com.fadynemer.cutime.viewmodel.BookingAvailabilityViewModel
import com.fadynemer.cutime.viewmodel.BookingViewModel
import com.fadynemer.cutime.model.BookingRequest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class CoreFlowComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun customerNavigationExposesReadableDestinations() {
        composeRule.setContent {
            CutTimeTheme(darkTheme = false) {
                CustomerBottomBar(
                    selectedDestination = CustomerDestination.HOME,
                    onHomeSelected = {},
                    onAppointmentsSelected = {},
                    onProfileSelected = {}
                )
            }
        }

        composeRule.onNodeWithText("Home").assertIsDisplayed()
        composeRule.onNodeWithText("Appointments").assertIsDisplayed()
        composeRule.onNodeWithText("Profile").assertIsDisplayed()
    }

    @Test
    fun barberNavigationUsesResponsiveHoursLabel() {
        composeRule.setContent {
            CutTimeTheme(darkTheme = false) {
                BarberBottomBar(
                    selectedDestination = BarberDestination.AVAILABILITY,
                    onDestinationSelected = {}
                )
            }
        }

        composeRule.onNodeWithText("Dashboard").assertIsDisplayed()
        composeRule.onNodeWithText("Services").assertIsDisplayed()
        composeRule.onNodeWithText("Hours").assertIsDisplayed()
        composeRule.onNodeWithText("Profile").assertIsDisplayed()
    }

    @Test
    fun bookingSummaryAppearsOnlyAfterCompleteSelectionAndReview() {
        val bookingRepository = RecordingBookingRepository()
        val bookingViewModel = BookingViewModel(bookingRepository)
        val availabilityViewModel =
            BookingAvailabilityViewModel(EmptyCatalogRepository())
        val barber = realBookableBarber()
        val service = barber.services.first()
        val date = firstOpenDate(barber)
        val time = AvailabilitySlotGenerator.availableTimes(
            availability = barber.availability,
            date = date,
            durationMinutes = service.durationMinutes,
            occupiedTimes = emptySet()
        ).first()

        composeRule.setContent {
            CutTimeTheme(darkTheme = false) {
                BookingScreen(
                    barberShop = barber,
                    onBack = {},
                    onViewAppointments = {},
                    bookingViewModel = bookingViewModel,
                    availabilityViewModel = availabilityViewModel
                )
            }
        }

        composeRule.onNodeWithTag(UiTestTags.BOOKING_FORM)
            .assertExists()
        composeRule.onNodeWithTag(UiTestTags.BOOKING_SUBMIT)
            .assertDoesNotExist()

        composeRule.onNodeWithTag(
            UiTestTags.SERVICE_OPTION_PREFIX + service.id
        ).performClick()
        composeRule.onNodeWithTag(
            UiTestTags.DATE_OPTION_PREFIX + date
        ).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(
            UiTestTags.TIME_OPTION_PREFIX + time
        ).performClick()
        composeRule.onNodeWithTag(UiTestTags.BOOKING_REVIEW)
            .performClick()

        composeRule.onNodeWithTag(UiTestTags.BOOKING_SUBMIT)
            .assertIsDisplayed()
        assertEquals(0, bookingRepository.submissionCount)
    }

    private fun realBookableBarber(): BarberShop =
        SampleBarberData.barberShops.first().copy(
            isDevelopmentFallback = false
        )

    private fun firstOpenDate(barber: BarberShop): LocalDate =
        (0L..6L)
            .map { offset -> LocalDate.now().plusDays(offset) }
            .first { date ->
                AvailabilitySlotGenerator.availableTimes(
                    availability = barber.availability,
                    date = date,
                    durationMinutes =
                        barber.services.first().durationMinutes,
                    occupiedTimes = emptySet()
                ).isNotEmpty()
            }

    private class RecordingBookingRepository :
        AppointmentBookingDataSource {
        var submissionCount = 0
            private set

        override fun createAppointment(
            request: BookingRequest,
            onResult: (Result<String>) -> Unit
        ) {
            submissionCount += 1
        }
    }

    private class EmptyCatalogRepository :
        BarberCatalogDataSource {
        override fun observeCatalog(
            onResult: (Result<BarberCatalog>) -> Unit
        ) = AppointmentObservation {}

        override fun loadBarber(
            barberId: String,
            onResult: (Result<BarberShop?>) -> Unit
        ) = onResult(Result.success(null))

        override fun observeOccupiedTimes(
            barberId: String,
            date: String,
            onResult: (Result<Set<String>>) -> Unit
        ): AppointmentObservation {
            onResult(Result.success(emptySet()))
            return AppointmentObservation {}
        }
    }
}
