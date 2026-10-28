package com.jarvis.assistant.auth

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "guest_session")

class GuestSessionManager(private val context: Context) {
    companion object {
        private val IS_GUEST_MODE = booleanPreferencesKey("is_guest_mode")
    }

    val isGuestMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_GUEST_MODE] ?: false
    }

    suspend fun setGuestMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_GUEST_MODE] = enabled
        }
    }
}
