package com.fadynemer.cutime.util

import java.security.MessageDigest

object DeviceTokenHasher {
    fun documentId(token: String): String {
        require(token.isNotBlank())
        val bytes =
            MessageDigest.getInstance("SHA-256")
                .digest(token.toByteArray(Charsets.UTF_8))

        return bytes.joinToString(separator = "") { byte ->
            "%02x".format(byte)
        }
    }
}
