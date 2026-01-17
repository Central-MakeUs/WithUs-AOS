package com.widthus.app.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

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

    @RequiresApi(Build.VERSION_CODES.O)
    fun checkIsTimePassed(notificationTime: String): Boolean {
        return try {
            // 1. "08:00 PM" 형식을 해석하기 위한 포맷터 설정
            val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)

            // 2. 문자열을 LocalTime 객체로 변환
            val targetTime = LocalTime.parse(notificationTime, formatter)

            // 3. 현재 시간 가져오기
            val currentTime = LocalTime.now()

            // 4. 현재 시간이 목표 시간보다 이후인지 확인
            currentTime.isAfter(targetTime)
        } catch (e: Exception) {
            // 파싱 에러 발생 시 기본값 반환
            false
        }
    }


}