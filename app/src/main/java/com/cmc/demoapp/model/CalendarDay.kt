package com.cmc.demoapp.model

data class CalendarDay(
    val date: Int,
    val dayOfWeek: String,
    val isSelected: Boolean = false
)