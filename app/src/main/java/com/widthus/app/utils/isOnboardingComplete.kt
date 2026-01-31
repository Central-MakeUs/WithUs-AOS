package com.widthus.app.utils

import android.content.Context
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

// DataStore 인스턴스 생성
private val Context.dataStore by preferencesDataStore(name = "user_preferences")

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // 저장할 키값들 정의 (나중에 여기에 계속 추가하면 됩니다)
    private object PreferencesKeys {
        val IS_ONBOARDING_COMPLETE = booleanPreferencesKey("is_onboarding_complete")
        // 예: val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
    }

    // --- 온보딩 완료 여부 로직 ---
    val isOnboardingComplete: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.IS_ONBOARDING_COMPLETE] ?: false
        }

    suspend fun updateOnboardingStatus(isComplete: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_ONBOARDING_COMPLETE] = isComplete
        }
    }

    // --- 새로운 항목이 추가될 때 아래에 비슷한 방식으로 작성 ---
    /*
    suspend fun updateDarkMode(isDark: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.IS_DARK_MODE] = isDark }
    }
    */
}