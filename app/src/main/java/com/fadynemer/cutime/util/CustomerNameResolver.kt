package com.fadynemer.cutime.util

object CustomerNameResolver {

    fun resolve(
        firestoreFullName: String?,
        authenticationDisplayName: String?
    ): String {
        return firestoreFullName
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?: authenticationDisplayName
                ?.trim()
                ?.takeIf(String::isNotBlank)
            ?: "Customer"
    }
}
