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
    }

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            dataStore.data.map { it[ACCESS_TOKEN] }.collect { _token.value = it }
        }
    }

    fun getAccessTokenSync(): String? = _token.value

    suspend fun saveAccessToken(token: String) {
        dataStore.edit { it[ACCESS_TOKEN] = token }
    }
}

