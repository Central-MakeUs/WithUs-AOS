package com.widthus.app.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
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

    fun shareImage(context: Context, imageBitmap: ImageBitmap) {
        try {
            // 1. Bitmap 변환 및 임시 파일 저장
            val bitmap = imageBitmap.asAndroidBitmap()
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs() // 폴더가 없으면 생성

            val file = File(cachePath, "shared_image_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            // 2. FileProvider를 통해 공유 가능한 URI 생성
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            // 3. 인텐트 생성 및 실행
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, contentUri)
                type = "image/png"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "이미지 공유하기"))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}

fun isUriEmpty(uri: Uri?): Boolean {
    return uri == null || uri == Uri.EMPTY || uri.toString().isBlank()
}
