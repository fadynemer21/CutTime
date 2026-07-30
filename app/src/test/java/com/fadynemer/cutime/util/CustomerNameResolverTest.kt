package com.fadynemer.cutime.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CustomerNameResolverTest {

    @Test
    fun firestoreProfileName_isThePrimarySource() {
        assertEquals(
            "Fady Customer",
            CustomerNameResolver.resolve(
                firestoreFullName = "  Fady Customer  ",
                authenticationDisplayName = "Old name"
            )
        )
    }

    @Test
    fun authenticationName_isUsedForLegacyProfile() {
        assertEquals(
            "Fady Customer",
            CustomerNameResolver.resolve(
                firestoreFullName = null,
                authenticationDisplayName = "Fady Customer"
            )
        )
    }

    @Test
    fun missingNames_neverExposeTheEmailAddress() {
        assertEquals(
            "Customer",
            CustomerNameResolver.resolve(
                firestoreFullName = " ",
                authenticationDisplayName = null
            )
        )
    }
}
