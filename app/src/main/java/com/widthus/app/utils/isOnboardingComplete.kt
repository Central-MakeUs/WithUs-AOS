package com.widthus.app.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context, // 필요 없으면 제거 가능
    private val dataStore: DataStore<Preferences>
) {
    private object PreferencesKeys {
        val IS_ONBOARDING_COMPLETE = booleanPreferencesKey("is_onboarding_complete")
    }

    val isOnboardingComplete: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.IS_ONBOARDING_COMPLETE] ?: false
        }

    suspend fun updateOnboardingStatus(isComplete: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_ONBOARDING_COMPLETE] = isComplete
        }
    }
}