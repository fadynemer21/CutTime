package com.fadynemer.cutime

import android.content.pm.ApplicationInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductionReadinessInstrumentedTest {
    private val context
        get() = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

    @Test
    fun applicationIdMatchesFirebaseRegistration() {
        assertEquals("com.fadynemer.cutime", context.packageName)
    }

    @Test
    fun applicationLabelComesFromProductionResources() {
        assertEquals(
            "CutTime",
            context.applicationInfo.loadLabel(context.packageManager)
                .toString()
        )
    }

    @Test
    fun sensitiveAccountStateIsExcludedFromAndroidBackup() {
        val backupAllowed =
            context.applicationInfo.flags and
                ApplicationInfo.FLAG_ALLOW_BACKUP != 0

        assertFalse(backupAllowed)
    }
}
