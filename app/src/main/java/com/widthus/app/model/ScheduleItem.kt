package com.widthus.app.model

import androidx.annotation.DrawableRes

// 1. 일정 데이터 모델
data class ScheduleItem(
    val id: Int,
    val title: String,
    val time: String,
    val isDone: Boolean = false
)

// 2. 추억(사진) 데이터 모델
data class MemoryItem(
    val id: Int,
    @DrawableRes val imageResId: Int
)