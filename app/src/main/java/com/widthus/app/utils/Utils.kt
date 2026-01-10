package com.widthus.app.utils

object Utils {
    fun calculateRemainingTime(targetTimeStr: String): String {
        try {
            // "08:00 PM" 형식을 24시간제로 변환하여 LocalTime 생성
            val formatter = java.time.format.DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.US)
            val targetTime = java.time.LocalTime.parse(targetTimeStr, formatter)
            val now = java.time.LocalTime.now()

            // 오늘 남은 시간 계산
            var duration = java.time.Duration.between(now, targetTime)

            // 만약 설정 시간이 이미 지났다면 내일 같은 시간으로 계산
            if (duration.isNegative) {
                duration = duration.plusDays(1)
            }

            val hours = duration.toHours()
            val minutes = duration.toMinutes() % 60

            return if (hours > 0) "${hours}시간 ${minutes}분 후" else "${minutes}분 후"
        } catch (e: Exception) {
            return "곧"
        }
    }


}