package org.withus.app.model

// 추억 조회 응답 모델
data class MemoryResponse(
    val monthKey: Int,
    val weekMemorySummaries: List<WeekMemorySummary>
)

data class WeekMemorySummary(
    val memoryType: String, // WEEK_MEMORY, CUSTOM
    val title: String,
    val customMemoryId: Long?,
    val weekEndDate: String?,
    val status: MemoryStatus,
    val needCreateImageUrls: List<String>,
    val createdImageUrl: String?,
    val createdAt: String?
)

enum class MemoryStatus {
    UNAVAILABLE, NEED_CREATE, CREATED
}

data class CreateMemoryRequest(val imageKey: String, val title: String? = null)
