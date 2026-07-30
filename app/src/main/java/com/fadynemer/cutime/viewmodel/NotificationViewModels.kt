package com.fadynemer.cutime.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fadynemer.cutime.model.NotificationPreferences
import com.fadynemer.cutime.model.UserNotification
import com.fadynemer.cutime.repository.AppointmentObservation
import com.fadynemer.cutime.repository.NotificationDataSource
import com.fadynemer.cutime.repository.NotificationRepository

data class NotificationCenterUiState(
    val isLoading: Boolean = true,
    val notifications: List<UserNotification> = emptyList(),
    val updatingNotificationId: String? = null,
    val isMarkingAllRead: Boolean = false,
    val errorMessage: String? = null
) {
    val unreadCount: Int
        get() = notifications.count { !it.isRead }

    val isEmpty: Boolean
        get() = !isLoading && notifications.isEmpty()
}

class NotificationCenterViewModel(
    private val repository:
        NotificationDataSource = NotificationRepository()
) : ViewModel() {
    var uiState by mutableStateOf(NotificationCenterUiState())
        private set

    private var observation: AppointmentObservation? = null

    init {
        observe()
    }

    fun retry() {
        observation?.stop()
        observation = null
        uiState = NotificationCenterUiState()
        observe()
    }

    private fun observe() {
        observation =
            repository.observeNotifications { result ->
                result
                    .onSuccess { notifications ->
                        uiState = uiState.copy(
                            isLoading = false,
                            notifications = notifications,
                            errorMessage = null
                        )
                    }
                    .onFailure { error ->
                        uiState = uiState.copy(
                            isLoading = false,
                            errorMessage =
                                error.localizedMessage
                                    ?: "Notifications could not be loaded."
                        )
                    }
            }
    }

    fun markRead(
        notification: UserNotification,
        onFinished: () -> Unit = {}
    ) {
        if (notification.isRead) {
            onFinished()
            return
        }
        if (uiState.updatingNotificationId != null) {
            return
        }

        uiState = uiState.copy(
            updatingNotificationId = notification.id,
            errorMessage = null
        )
        repository.markRead(notification.id) { result ->
            result
                .onSuccess {
                    uiState = uiState.copy(
                        notifications =
                            uiState.notifications.map {
                                if (it.id == notification.id) {
                                    it.copy(isRead = true)
                                } else {
                                    it
                                }
                            },
                        updatingNotificationId = null
                    )
                    onFinished()
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        updatingNotificationId = null,
                        errorMessage =
                            error.localizedMessage
                                ?: "Notification could not be updated."
                    )
                }
        }
    }

    fun markAllRead() {
        if (
            uiState.isMarkingAllRead ||
            uiState.unreadCount == 0
        ) {
            return
        }

        uiState = uiState.copy(
            isMarkingAllRead = true,
            errorMessage = null
        )
        repository.markAllRead { result ->
            result
                .onSuccess {
                    uiState = uiState.copy(
                        isMarkingAllRead = false,
                        notifications =
                            uiState.notifications.map {
                                it.copy(isRead = true)
                            }
                    )
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        isMarkingAllRead = false,
                        errorMessage =
                            error.localizedMessage
                                ?: "Notifications could not be updated."
                    )
                }
        }
    }

    fun delete(notificationId: String) {
        if (uiState.updatingNotificationId != null) return

        uiState = uiState.copy(
            updatingNotificationId = notificationId,
            errorMessage = null
        )
        repository.deleteNotification(notificationId) { result ->
            result
                .onSuccess {
                    uiState = uiState.copy(
                        notifications =
                            uiState.notifications.filterNot {
                                it.id == notificationId
                            },
                        updatingNotificationId = null
                    )
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        updatingNotificationId = null,
                        errorMessage =
                            error.localizedMessage
                                ?: "Notification could not be deleted."
                    )
                }
        }
    }

    fun dismissError() {
        uiState = uiState.copy(errorMessage = null)
    }

    override fun onCleared() {
        observation?.stop()
        super.onCleared()
    }
}

data class NotificationPreferencesUiState(
    val isLoading: Boolean = true,
    val saved: NotificationPreferences =
        NotificationPreferences(),
    val draft: NotificationPreferences =
        NotificationPreferences(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val hasChanges: Boolean
        get() = draft != saved
}

class NotificationPreferencesViewModel(
    private val repository:
        NotificationDataSource = NotificationRepository()
) : ViewModel() {
    var uiState by mutableStateOf(
        NotificationPreferencesUiState()
    )
        private set

    private var observation: AppointmentObservation? = null

    init {
        observe()
    }

    private fun observe() {
        observation =
            repository.observePreferences { result ->
                result
                    .onSuccess { preferences ->
                        if (!uiState.hasChanges) {
                            uiState = uiState.copy(
                                isLoading = false,
                                saved = preferences,
                                draft = preferences,
                                errorMessage = null
                            )
                        } else {
                            uiState = uiState.copy(
                                isLoading = false,
                                saved = preferences,
                                errorMessage = null
                            )
                        }
                    }
                    .onFailure { error ->
                        uiState = uiState.copy(
                            isLoading = false,
                            errorMessage =
                                error.localizedMessage
                                    ?: "Notification settings could not be loaded."
                        )
                    }
            }
    }

    fun setPushEnabled(value: Boolean) {
        uiState = uiState.copy(
            draft = uiState.draft.copy(pushEnabled = value),
            successMessage = null
        )
    }

    fun setRemindersEnabled(value: Boolean) {
        uiState = uiState.copy(
            draft =
                uiState.draft.copy(remindersEnabled = value),
            successMessage = null
        )
    }

    fun setAppointmentUpdatesEnabled(value: Boolean) {
        uiState = uiState.copy(
            draft =
                uiState.draft.copy(
                    appointmentUpdatesEnabled = value
                ),
            successMessage = null
        )
    }

    fun setReviewPromptsEnabled(value: Boolean) {
        uiState = uiState.copy(
            draft =
                uiState.draft.copy(
                    reviewPromptsEnabled = value
                ),
            successMessage = null
        )
    }

    fun discardChanges() {
        uiState = uiState.copy(
            draft = uiState.saved,
            errorMessage = null,
            successMessage = null
        )
    }

    fun save() {
        if (uiState.isSaving || !uiState.hasChanges) return

        uiState = uiState.copy(
            isSaving = true,
            errorMessage = null,
            successMessage = null
        )
        val saving = uiState.draft
        repository.savePreferences(saving) { result ->
            result
                .onSuccess {
                    uiState = uiState.copy(
                        saved = saving,
                        draft = saving,
                        isSaving = false,
                        successMessage =
                            "Notification settings saved."
                    )
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        isSaving = false,
                        errorMessage =
                            error.localizedMessage
                                ?: "Notification settings could not be saved."
                    )
                }
        }
    }

    override fun onCleared() {
        observation?.stop()
        super.onCleared()
    }
}

data class NotificationBadgeUiState(
    val unreadCount: Int = 0
)

class NotificationBadgeViewModel(
    private val repository:
        NotificationDataSource = NotificationRepository()
) : ViewModel() {
    var uiState by mutableStateOf(NotificationBadgeUiState())
        private set

    private val observation =
        repository.observeNotifications { result ->
            result.onSuccess { notifications ->
                uiState = NotificationBadgeUiState(
                    unreadCount =
                        notifications.count { !it.isRead }
                )
            }
        }

    override fun onCleared() {
        observation?.stop()
        super.onCleared()
    }
}
