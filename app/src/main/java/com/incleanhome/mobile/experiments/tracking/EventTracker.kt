package com.incleanhome.mobile.experiments.tracking

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.incleanhome.mobile.experiments.Assignment
import com.incleanhome.mobile.experiments.KeyValueStore
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.util.UUID

/**
 * Event envelope with the common properties of 8.2.8: eventId, capture date and time, platform,
 * user role and a pseudonymous user reference; experimentId and variant during an experiment.
 */
data class TrackingEvent(
    val eventId: String,
    val eventName: String,
    val occurredAt: String,
    val platform: String,
    val os: String,
    val appVersion: String,
    val environment: String,
    val userRole: String?,
    val userRef: String?,
    val sessionId: String,
    val experimentId: String?,
    val variant: String?,
    val featureActive: Boolean?,
    val properties: Map<String, Any?>
)

data class TrackingUser(val userId: Int, val role: String)

fun interface TrackingUploader {
    /** Returns true only when the API confirmed the whole batch. */
    suspend fun upload(events: List<TrackingEvent>): Boolean
}

fun interface TrackingLogger {
    fun log(line: String)
}

enum class TrackResult { RECORDED, DUPLICATE, REJECTED }

/**
 * Records plan events in a persistent outbox and sends them when an uploader is configured.
 * A repeated operation (same operation key) is never recorded twice (US42, Escenario 4); the
 * eventId is derived from that key so the API can also discard duplicates.
 */
class EventTracker(
    private val store: KeyValueStore,
    private val userProvider: () -> TrackingUser?,
    private val appVersion: String,
    private val environment: String,
    private val uploader: TrackingUploader? = null,
    private val logger: TrackingLogger? = null,
    private val clock: Clock = Clock.systemUTC(),
    private val sessionId: String = UUID.randomUUID().toString()
) {
    private val gson = Gson()
    private val lock = Any()

    fun track(
        eventName: String,
        properties: Map<String, Any?> = emptyMap(),
        operationKey: String? = null,
        assignment: Assignment? = null
    ): TrackResult {
        if (!TrackingPlan.isKnown(eventName)) return TrackResult.REJECTED
        val user = userProvider()
        val dedupeKey = operationKey?.let { "$eventName|${user?.userId ?: "anon"}|$it" }
        synchronized(lock) {
            val recordedKeys = readKeys()
            if (dedupeKey != null && dedupeKey in recordedKeys) return TrackResult.DUPLICATE

            val event = TrackingEvent(
                eventId = dedupeKey?.let { UUID.nameUUIDFromBytes(it.toByteArray()).toString() }
                    ?: UUID.randomUUID().toString(),
                eventName = eventName,
                occurredAt = Instant.now(clock).toString(),
                platform = PLATFORM,
                os = OS,
                appVersion = appVersion,
                environment = environment,
                userRole = user?.role?.lowercase(),
                userRef = user?.let { pseudonymize(it.userId) },
                sessionId = sessionId,
                experimentId = assignment?.takeIf { it.inExperiment }?.experiment?.id,
                variant = assignment?.takeIf { it.inExperiment }?.variant?.wire,
                featureActive = assignment?.takeIf { it.inExperiment }?.featureActive,
                properties = PrivacyGuard.sanitize(eventName, properties)
            )
            writeOutbox((readOutbox() + event).takeLast(MAX_OUTBOX))
            if (dedupeKey != null) writeKeys((recordedKeys + dedupeKey).toList().takeLast(MAX_KEYS))
            logger?.log(gson.toJson(event))
        }
        return TrackResult.RECORDED
    }

    fun pendingEvents(): List<TrackingEvent> = synchronized(lock) { readOutbox() }

    fun exportJson(): String = synchronized(lock) { gson.toJson(readOutbox()) }

    /**
     * Sends the outbox; events stay stored when there is no uploader or the API rejects them, and the
     * same eventId is sent again on the next attempt (the API ignores repeated ids).
     * Events without a user (for example the FAQ before signing in) stay on the device: the API
     * requires a session and would attribute them to the account that sends them.
     */
    suspend fun flush(): Int {
        val sender = uploader ?: return 0
        val batch = pendingEvents().filter { it.userRef != null }.take(MAX_BATCH)
        if (batch.isEmpty()) return 0
        val sent = runCatching { sender.upload(batch) }.getOrDefault(false)
        if (!sent) return 0
        synchronized(lock) {
            val sentIds = batch.map(TrackingEvent::eventId).toSet()
            writeOutbox(readOutbox().filterNot { it.eventId in sentIds })
        }
        return batch.size
    }

    // In-memory copies avoid parsing the stored JSON on every call (form edits track often).
    private var outboxCache: List<TrackingEvent>? = null
    private var keysCache: LinkedHashSet<String>? = null

    private fun readOutbox(): List<TrackingEvent> = outboxCache ?: runCatching {
        store.getString(OUTBOX_KEY)?.let<String, List<TrackingEvent>> {
            gson.fromJson(it, object : TypeToken<List<TrackingEvent>>() {}.type)
        }
    }.getOrNull().orEmpty().also { outboxCache = it }

    private fun writeOutbox(events: List<TrackingEvent>) {
        outboxCache = events
        store.putString(OUTBOX_KEY, gson.toJson(events))
    }

    private fun readKeys(): LinkedHashSet<String> = LinkedHashSet(keysCache ?: runCatching {
        store.getString(KEYS_KEY)?.let<String, List<String>> {
            gson.fromJson(it, object : TypeToken<List<String>>() {}.type)
        }
    }.getOrNull().orEmpty().toCollection(LinkedHashSet()).also { keysCache = it })

    private fun writeKeys(keys: List<String>) {
        keysCache = LinkedHashSet(keys)
        store.putString(KEYS_KEY, gson.toJson(keys))
    }

    companion object {
        const val PLATFORM = "mobile"
        const val OS = "android"
        private const val OUTBOX_KEY = "tracking.outbox"
        private const val KEYS_KEY = "tracking.operationKeys"
        private const val MAX_OUTBOX = 1000
        private const val MAX_KEYS = 2000
        /** POST tracking/events accepts up to 1000 events per batch. */
        private const val MAX_BATCH = 500

        /** Internal pseudonymous identifier: the analysis never needs the raw account id. */
        fun pseudonymize(userId: Int): String = MessageDigest.getInstance("SHA-256")
            .digest("incleanhome-user:$userId".toByteArray(Charsets.UTF_8))
            .take(8)
            .joinToString("") { "%02x".format(it) }
    }
}
