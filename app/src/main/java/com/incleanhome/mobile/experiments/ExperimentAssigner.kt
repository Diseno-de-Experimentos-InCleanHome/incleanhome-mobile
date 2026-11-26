package com.incleanhome.mobile.experiments

import java.security.MessageDigest

/**
 * Deterministic assignment shared by every platform (8.2.4, 8.2.8 "Asignación persistente").
 *
 * bucket = first 4 bytes (big endian, unsigned) of SHA-256("<experimentId>:<userId>") mod 100.
 * The user is in treatment when bucket < treatmentPercent. Web and API can reproduce the same
 * bucket, so a user keeps the same group on every platform.
 */
object ExperimentAssigner {

    fun bucket(experimentId: String, userId: Int): Int {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$experimentId:$userId".toByteArray(Charsets.UTF_8))
        val value = ((digest[0].toLong() and 0xFF) shl 24) or
            ((digest[1].toLong() and 0xFF) shl 16) or
            ((digest[2].toLong() and 0xFF) shl 8) or
            (digest[3].toLong() and 0xFF)
        return (value % 100).toInt()
    }

    fun variantFor(experimentId: String, userId: Int, treatmentPercent: Int): Variant =
        if (bucket(experimentId, userId) < treatmentPercent) Variant.TREATMENT else Variant.CONTROL
}
