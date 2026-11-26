package com.incleanhome.mobile.experiments

import android.content.Context
import android.util.Log
import com.incleanhome.mobile.BuildConfig
import com.incleanhome.mobile.core.network.RetrofitClient
import com.incleanhome.mobile.experiments.tracking.EventTracker
import com.incleanhome.mobile.experiments.tracking.TrackingLogger
import com.incleanhome.mobile.experiments.tracking.TrackingUploader
import com.incleanhome.mobile.experiments.tracking.TrackingUser
import com.incleanhome.mobile.reviews.domain.ReviewReminderService
import kotlinx.coroutines.CancellationException

/**
 * Application-wide access to the experiment service and the event tracker, following the same
 * pattern as [RetrofitClient] (initialised once from MainActivity). Unit tests inject their own
 * instances through the constructors of the ViewModels instead of using this object.
 */
object Experimentation {
    @Volatile private var userProvider: () -> TrackingUser? = { null }
    @Volatile private var initialized: Pair<ExperimentService, EventTracker>? = null

    val experiments: ExperimentService get() = requireInitialized().first
    val tracker: EventTracker get() = requireInitialized().second

    fun isInitialized(): Boolean = initialized != null

    /** Null before initialisation (for example in JVM tests); callers then keep the As-Is flow. */
    fun experimentsOrNull(): ExperimentService? = initialized?.first
    fun trackerOrNull(): EventTracker? = initialized?.second
    fun storeOrNull(): KeyValueStore? = sharedStore

    @Volatile private var sharedStore: KeyValueStore? = null

    fun reminderServiceOrNull(): ReviewReminderService? {
        val (service, tracker) = initialized ?: return null
        val store = sharedStore ?: return null
        return ReviewReminderService(
            store = store,
            experiments = service,
            tracker = tracker,
            userId = { userProvider()?.userId },
            delayHours = BuildConfig.REVIEW_REMINDER_DELAY_HOURS.toLong()
        )
    }

    fun initialize(context: Context, currentUser: () -> TrackingUser?) {
        // Re-bind on every Activity creation: a recreated Activity owns a new SessionManager.
        userProvider = currentUser
        if (initialized != null) return
        val store = SharedPreferencesStore(
            context.applicationContext.getSharedPreferences("incleanhome_experiments", Context.MODE_PRIVATE)
        )
        val api by lazy { RetrofitClient.retrofit.create(ExperimentationApi::class.java) }
        val uploader = if (BuildConfig.TRACKING_UPLOAD_ENABLED) {
            TrackingUploader { events -> api.sendEvents(TrackingBatch(events)).isSuccessful }
        } else {
            null
        }
        val logger = if (BuildConfig.TRACKING_LOGCAT) TrackingLogger { Log.i(LOG_TAG, it) } else null
        val tracker = EventTracker(
            store = store,
            userProvider = { userProvider() },
            appVersion = BuildConfig.VERSION_NAME,
            environment = BuildConfig.ENVIRONMENT,
            uploader = uploader,
            logger = logger
        )
        val service = ExperimentService(
            store = store,
            userProvider = { userProvider() },
            baseConfig = { ExperimentConfig.parse(BuildConfig.EXPERIMENTS) },
            tracker = tracker,
            allowLocalOverrides = BuildConfig.DIAGNOSTICS_ENABLED
        )
        sharedStore = store
        initialized = service to tracker
    }

    /** Downloads the remote configuration when the API offers it; otherwise keeps the build default. */
    suspend fun refreshRemoteConfig(): Boolean {
        if (!BuildConfig.REMOTE_EXPERIMENTS_ENABLED || initialized == null) return false
        return try {
            val config = RetrofitClient.retrofit.create(ExperimentationApi::class.java).getConfig().toConfig()
            experiments.cacheRemoteConfig(config)
            true
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            false
        }
    }

    /** Lifecycle hook (MainActivity.onStart): refreshes the configuration and sends pending events. */
    suspend fun synchronize() {
        refreshRemoteConfig()
        trackerOrNull()?.flush()
    }

    private fun requireInitialized(): Pair<ExperimentService, EventTracker> =
        checkNotNull(initialized) { "Experimentation.initialize must be called from MainActivity" }

    const val LOG_TAG = "ICH-Tracking"
}
