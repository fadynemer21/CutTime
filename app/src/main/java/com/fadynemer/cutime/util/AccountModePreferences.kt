package com.fadynemer.cutime.util

import android.content.Context
import androidx.core.content.edit

object AccountModePreferences {
    private const val PREFERENCES_NAME = "cutime_account_mode"

    fun isCustomerMode(
        context: Context,
        uid: String
    ): Boolean {
        return context
            .getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )
            .getBoolean(key(uid), false)
    }

    fun setCustomerMode(
        context: Context,
        uid: String,
        enabled: Boolean
    ) {
        context
            .getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )
            .edit { putBoolean(key(uid), enabled) }
    }

    private fun key(uid: String) =
        "customer_mode_$uid"
}
