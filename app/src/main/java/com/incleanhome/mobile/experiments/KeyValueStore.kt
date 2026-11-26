package com.incleanhome.mobile.experiments

import android.content.SharedPreferences
import androidx.core.content.edit

/** Small persistence abstraction so the experimentation logic can be unit tested on the JVM. */
interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
}

class SharedPreferencesStore(private val preferences: SharedPreferences) : KeyValueStore {
    override fun getString(key: String): String? = preferences.getString(key, null)
    override fun putString(key: String, value: String) = preferences.edit { putString(key, value) }
    override fun remove(key: String) = preferences.edit { remove(key) }
}

class InMemoryStore : KeyValueStore {
    private val values = linkedMapOf<String, String>()
    override fun getString(key: String): String? = synchronized(values) { values[key] }
    override fun putString(key: String, value: String) { synchronized(values) { values[key] = value } }
    override fun remove(key: String) { synchronized(values) { values.remove(key) } }
}
