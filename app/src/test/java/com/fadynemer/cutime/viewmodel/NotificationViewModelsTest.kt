package com.fadynemer.cutime.viewmodel

import com.fadynemer.cutime.model.NotificationPreferences
import com.fadynemer.cutime.model.NotificationType
import com.fadynemer.cutime.model.UserNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun testNotification(
    id: String = "notification_1",
    isRead: Boolean = false,
    type: NotificationType =
        NotificationType.APPOINTMENT_BOOKED
) = UserNotification(
    id = id,
    userId = "user_1",
    type = type,
    title = "Appointment confirmed",
    message = "Your appointment is booked.",
    appointmentId = "appointment_1",
    barberId = "barber_1",
    isRead = isRead,
    createdAtMillis = 1_700_000_000_000L
)

class NotificationCenterViewModelTest {
    private val repository = FakeNotificationDataSource()
    private val viewModel =
        NotificationCenterViewModel(repository)

    @Test
    fun initializationObservesNotifications() {
        assertEquals(1, repository.notificationsObservationCount)
        assertTrue(viewModel.uiState.isLoading)
    }

    @Test
    fun successfulObservationCalculatesUnreadCount() {
        repository.emitNotifications(
            Result.success(
                listOf(
                    testNotification("one"),
                    testNotification("two", isRead = true),
                    testNotification("three")
                )
            )
        )

        assertFalse(viewModel.uiState.isLoading)
        assertEquals(3, viewModel.uiState.notifications.size)
        assertEquals(2, viewModel.uiState.unreadCount)
        assertFalse(viewModel.uiState.isEmpty)
    }

    @Test
    fun emptySuccessfulObservationMarksStateEmpty() {
        repository.emitNotifications(Result.success(emptyList()))

        assertTrue(viewModel.uiState.isEmpty)
        assertEquals(0, viewModel.uiState.unreadCount)
    }

    @Test
    fun failedObservationShowsRepositoryMessage() {
        repository.emitNotifications(
            Result.failure(IllegalStateException("No connection"))
        )

        assertFalse(viewModel.uiState.isLoading)
        assertEquals(
            "No connection",
            viewModel.uiState.errorMessage
        )
    }

    @Test
    fun retryStopsOldListenerAndStartsNewOne() {
        repository.emitNotifications(
            Result.failure(IllegalStateException("Failed"))
        )

        viewModel.retry()

        assertEquals(1, repository.notificationsStopCount)
        assertEquals(2, repository.notificationsObservationCount)
        assertTrue(viewModel.uiState.isLoading)
        assertNull(viewModel.uiState.errorMessage)
    }

    @Test
    fun unreadNotificationIsMarkedThroughRepository() {
        val notification = testNotification()
        repository.emitNotifications(
            Result.success(listOf(notification))
        )

        viewModel.markRead(notification)

        assertEquals(
            notification.id,
            repository.markedNotificationId
        )
        assertEquals(
            notification.id,
            viewModel.uiState.updatingNotificationId
        )
    }

    @Test
    fun markReadSuccessUpdatesLocalItemAndRunsCallback() {
        val notification = testNotification()
        repository.emitNotifications(
            Result.success(listOf(notification))
        )
        var finished = false
        viewModel.markRead(notification) {
            finished = true
        }

        repository.completeMarkRead(Result.success(Unit))

        assertTrue(viewModel.uiState.notifications.single().isRead)
        assertNull(viewModel.uiState.updatingNotificationId)
        assertTrue(finished)
    }

    @Test
    fun alreadyReadNotificationSkipsWriteAndRunsCallback() {
        val notification = testNotification(isRead = true)
        var finished = false

        viewModel.markRead(notification) {
            finished = true
        }

        assertNull(repository.markedNotificationId)
        assertTrue(finished)
    }

    @Test
    fun markReadFailureKeepsNotificationUnread() {
        val notification = testNotification()
        repository.emitNotifications(
            Result.success(listOf(notification))
        )
        viewModel.markRead(notification)

        repository.completeMarkRead(
            Result.failure(IllegalStateException("Write failed"))
        )

        assertFalse(viewModel.uiState.notifications.single().isRead)
        assertNull(viewModel.uiState.updatingNotificationId)
        assertEquals(
            "Write failed",
            viewModel.uiState.errorMessage
        )
    }

    @Test
    fun secondMarkReadIsBlockedWhileFirstIsPending() {
        val one = testNotification("one")
        val two = testNotification("two")
        repository.emitNotifications(Result.success(listOf(one, two)))

        viewModel.markRead(one)
        viewModel.markRead(two)

        assertEquals("one", repository.markedNotificationId)
    }

    @Test
    fun markAllDoesNothingWhenNoUnreadItems() {
        repository.emitNotifications(
            Result.success(
                listOf(testNotification(isRead = true))
            )
        )

        viewModel.markAllRead()

        assertEquals(0, repository.markAllCalls)
    }

    @Test
    fun markAllSuccessMarksEveryItemLocally() {
        repository.emitNotifications(
            Result.success(
                listOf(
                    testNotification("one"),
                    testNotification("two", isRead = true)
                )
            )
        )
        viewModel.markAllRead()

        assertTrue(viewModel.uiState.isMarkingAllRead)
        assertEquals(1, repository.markAllCalls)
        repository.completeMarkAll(Result.success(Unit))

        assertFalse(viewModel.uiState.isMarkingAllRead)
        assertTrue(
            viewModel.uiState.notifications.all { it.isRead }
        )
    }

    @Test
    fun markAllFailureRestoresIdleState() {
        repository.emitNotifications(
            Result.success(listOf(testNotification()))
        )
        viewModel.markAllRead()

        repository.completeMarkAll(
            Result.failure(IllegalStateException("Batch failed"))
        )

        assertFalse(viewModel.uiState.isMarkingAllRead)
        assertEquals(
            "Batch failed",
            viewModel.uiState.errorMessage
        )
    }

    @Test
    fun deleteSuccessRemovesOnlySelectedNotification() {
        repository.emitNotifications(
            Result.success(
                listOf(
                    testNotification("one"),
                    testNotification("two")
                )
            )
        )

        viewModel.delete("one")
        assertEquals("one", repository.deletedNotificationId)
        repository.completeDelete(Result.success(Unit))

        assertEquals(
            listOf("two"),
            viewModel.uiState.notifications.map { it.id }
        )
    }

    @Test
    fun deleteFailureKeepsNotification() {
        repository.emitNotifications(
            Result.success(listOf(testNotification("one")))
        )
        viewModel.delete("one")

        repository.completeDelete(
            Result.failure(IllegalStateException("Delete failed"))
        )

        assertEquals(1, viewModel.uiState.notifications.size)
        assertEquals(
            "Delete failed",
            viewModel.uiState.errorMessage
        )
    }

    @Test
    fun dismissErrorClearsVisibleError() {
        repository.emitNotifications(
            Result.failure(IllegalStateException("Failed"))
        )

        viewModel.dismissError()

        assertNull(viewModel.uiState.errorMessage)
    }
}

class NotificationPreferencesViewModelTest {
    private val repository = FakeNotificationDataSource()
    private val viewModel =
        NotificationPreferencesViewModel(repository)

    private val stored = NotificationPreferences(
        pushEnabled = true,
        remindersEnabled = true,
        appointmentUpdatesEnabled = true,
        reviewPromptsEnabled = true
    )

    private fun load() {
        repository.emitPreferences(Result.success(stored))
    }

    @Test
    fun initializationObservesPreferences() {
        assertEquals(1, repository.preferencesObservationCount)
        assertTrue(viewModel.uiState.isLoading)
    }

    @Test
    fun firstSnapshotSetsSavedAndDraft() {
        load()

        assertFalse(viewModel.uiState.isLoading)
        assertEquals(stored, viewModel.uiState.saved)
        assertEquals(stored, viewModel.uiState.draft)
        assertFalse(viewModel.uiState.hasChanges)
    }

    @Test
    fun everyBooleanSettingCanBeChanged() {
        load()

        viewModel.setPushEnabled(false)
        viewModel.setRemindersEnabled(false)
        viewModel.setAppointmentUpdatesEnabled(false)
        viewModel.setReviewPromptsEnabled(false)

        assertFalse(viewModel.uiState.draft.pushEnabled)
        assertFalse(viewModel.uiState.draft.remindersEnabled)
        assertFalse(
            viewModel.uiState.draft.appointmentUpdatesEnabled
        )
        assertFalse(viewModel.uiState.draft.reviewPromptsEnabled)
        assertTrue(viewModel.uiState.hasChanges)
    }

    @Test
    fun incomingSnapshotDoesNotOverwriteDirtyDraft() {
        load()
        viewModel.setPushEnabled(false)
        val serverUpdate = stored.copy(
            remindersEnabled = false
        )

        repository.emitPreferences(Result.success(serverUpdate))

        assertFalse(viewModel.uiState.draft.pushEnabled)
        assertTrue(viewModel.uiState.draft.remindersEnabled)
        assertEquals(serverUpdate, viewModel.uiState.saved)
    }

    @Test
    fun discardRestoresLatestSavedValues() {
        load()
        viewModel.setPushEnabled(false)

        viewModel.discardChanges()

        assertEquals(stored, viewModel.uiState.draft)
        assertFalse(viewModel.uiState.hasChanges)
    }

    @Test
    fun saveDoesNothingWithoutChanges() {
        load()

        viewModel.save()

        assertNull(repository.savedPreferences)
        assertFalse(viewModel.uiState.isSaving)
    }

    @Test
    fun saveSendsCurrentDraft() {
        load()
        viewModel.setReviewPromptsEnabled(false)

        viewModel.save()

        assertTrue(viewModel.uiState.isSaving)
        assertEquals(
            viewModel.uiState.draft,
            repository.savedPreferences
        )
    }

    @Test
    fun saveSuccessMakesDraftTheNewBaseline() {
        load()
        viewModel.setPushEnabled(false)
        viewModel.save()

        repository.completeSave(Result.success(Unit))

        assertFalse(viewModel.uiState.isSaving)
        assertEquals(
            viewModel.uiState.saved,
            viewModel.uiState.draft
        )
        assertFalse(viewModel.uiState.hasChanges)
        assertEquals(
            "Notification settings saved.",
            viewModel.uiState.successMessage
        )
    }

    @Test
    fun saveFailureKeepsDirtyDraftForRetry() {
        load()
        viewModel.setPushEnabled(false)
        viewModel.save()

        repository.completeSave(
            Result.failure(IllegalStateException("Save failed"))
        )

        assertFalse(viewModel.uiState.isSaving)
        assertTrue(viewModel.uiState.hasChanges)
        assertEquals(
            "Save failed",
            viewModel.uiState.errorMessage
        )
    }

    @Test
    fun preferenceObservationFailureEndsLoading() {
        repository.emitPreferences(
            Result.failure(IllegalStateException("Read failed"))
        )

        assertFalse(viewModel.uiState.isLoading)
        assertEquals(
            "Read failed",
            viewModel.uiState.errorMessage
        )
    }
}

class NotificationBadgeViewModelTest {
    private val repository = FakeNotificationDataSource()
    private val viewModel =
        NotificationBadgeViewModel(repository)

    @Test
    fun unreadItemsAreCounted() {
        repository.emitNotifications(
            Result.success(
                listOf(
                    testNotification("one"),
                    testNotification("two", isRead = true),
                    testNotification("three")
                )
            )
        )

        assertEquals(2, viewModel.uiState.unreadCount)
    }

    @Test
    fun subsequentSnapshotsReplaceBadgeCount() {
        repository.emitNotifications(
            Result.success(listOf(testNotification()))
        )
        repository.emitNotifications(
            Result.success(
                listOf(testNotification(isRead = true))
            )
        )

        assertEquals(0, viewModel.uiState.unreadCount)
    }

    @Test
    fun failedSnapshotDoesNotInventUnreadItems() {
        repository.emitNotifications(
            Result.failure(IllegalStateException("Failed"))
        )

        assertEquals(0, viewModel.uiState.unreadCount)
    }
}
