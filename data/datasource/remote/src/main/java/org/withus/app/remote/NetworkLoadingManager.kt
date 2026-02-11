package org.withus.app.remote

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkLoadingManager @Inject constructor() {
    private val _loadingCount = MutableStateFlow(0) // 현재 실행 중인 API 개수

    // 개수가 0보다 크면 true
    val isLoading: StateFlow<Boolean> = _loadingCount
        .map { it > 0 }
        .stateIn(CoroutineScope(
            Dispatchers.Main), SharingStarted.WhileSubscribed(5000), false)

    fun showLoading() {
        _loadingCount.value += 1
    }

    fun hideLoading() {
        _loadingCount.value = (_loadingCount.value - 1).coerceAtLeast(0)
    }
}