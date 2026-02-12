package org.withus.app.token

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.withus.app.token.TokenManager.Companion.ACCESS_TOKEN
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val FCM_TOKEN = stringPreferencesKey("fcm_token")
    }

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    private val _reRefreshToken = MutableStateFlow<String?>(null)
    val reRefreshToken: StateFlow<String?> = _token.asStateFlow()

    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            dataStore.data.map { it[ACCESS_TOKEN] }.collect { _token.value = it }
        }

        CoroutineScope(Dispatchers.IO).launch {
            dataStore.data.map { it[REFRESH_TOKEN] }.collect { _reRefreshToken.value = it }
        }

        CoroutineScope(Dispatchers.IO).launch {
            dataStore.data.map { it[FCM_TOKEN] }.collect { _fcmToken.value = it }
        }
    }

    fun getAccessTokenSync(): String? = _token.value

    fun getRefreshTokenSync(): String? = reRefreshToken.value

    suspend fun saveAccessToken(token: String, refreshToken: String) {
        dataStore.edit { it[ACCESS_TOKEN] = token }
        dataStore.edit { it[REFRESH_TOKEN] = refreshToken }
    }

    suspend fun deleteAccessToken() {
        dataStore.edit { it.remove(ACCESS_TOKEN) }
        _token.value = null // StateFlow도 즉시 업데이트
    }

    suspend fun saveFcmToken(token: String) {
        dataStore.edit { it[FCM_TOKEN] = token }
    }

    fun getFcmTokenSync(): String? = _fcmToken.value
}

