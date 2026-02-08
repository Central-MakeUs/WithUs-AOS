package com.widthus.app.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.withus.app.model.CreateMemoryRequest
import org.withus.app.model.MemoryResponse
import org.withus.app.remote.ApiService
import org.withus.app.repository.ArchiveRepository
import org.withus.app.repository.ImageUploadManager
import saveBitmapToGallery
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

sealed class UploadState {
    object Idle : UploadState()
    object Loading : UploadState()
    object Success : UploadState()
    data class Error(val message: String) : UploadState()
}

@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val repository: ArchiveRepository,
    private val imageUploadManager: ImageUploadManager,
    private val apiService: ApiService,
    @ApplicationContext private val context: Context,
    ) : ViewModel() {

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState = _uploadState.asStateFlow()

    var memoryData by mutableStateOf<MemoryResponse?>(null)
        private set

    var currentMonth by mutableStateOf(LocalDate.now())
        private set

    // YYYYMM 형식의 키 생성
    private fun getMonthKey(date: LocalDate) = date.format(DateTimeFormatter.ofPattern("yyyyMM"))

    fun fetchMemories(date: LocalDate = currentMonth) {
        currentMonth = date
        viewModelScope.launch {
            repository.getMemories(getMonthKey(date))
                .onSuccess { memoryData = it }
        }
    }

    fun uploadCustomMemory(bitmap: Bitmap, targetWeekEndDate: String? = null) {
        viewModelScope.launch {
            _uploadState.value = UploadState.Loading
//            val targetWeekEndDate = LocalDate.now()
//                .with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
//                .format(DateTimeFormatter.ISO_LOCAL_DATE) // "2026-02-14"

            // 1. 이미지 업로드 (S3)
            val imageKey = imageUploadManager.uploadBitmap(bitmap, "MEMORY")
            saveBitmapToGallery(context, bitmap)

            if (imageKey != null) {
                // 2. 추억 생성 API 호출
                try {
                    val result = if (targetWeekEndDate == null) apiService.createMemoryNoDate(
                        CreateMemoryRequest(imageKey)
                    )
                    else apiService.createMemoryWithDate(
                        targetWeekEndDate,
                        CreateMemoryRequest(imageKey)
                    )

                    if (result.isSuccessful && result.body()?.success == true) {
                        _uploadState.value = UploadState.Success
                    } else {
                        val errorMsg = result.body()?.error?.message ?: "추억 생성에 실패했습니다."
                        _uploadState.value = UploadState.Error(errorMsg)
                    }
                } catch (e: Exception) {
                    _uploadState.value = UploadState.Error("네트워크 오류가 발생했습니다.")
                }
            } else {
                _uploadState.value = UploadState.Error("이미지 업로드에 실패했습니다.")
            }
        }
    }

    // 상태 초기화 (성공 후 화면 닫기 전 등)
    fun resetUploadState() {
        _uploadState.value = UploadState.Idle
    }
}