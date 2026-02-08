package com.widthus.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.widthus.app.MainActivity
import com.withus.app.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.withus.app.token.TokenManager
import javax.inject.Inject

@AndroidEntryPoint // Hilt를 사용한다면 필요합니다.
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var tokenManager: TokenManager // 필요 시 토큰 저장을 위해 주입

    // [중요] 새로운 토큰이 생성될 때마다 호출됨 (앱 설치 후 처음, 혹은 갱신 시)
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New Token: $token")

        CoroutineScope(Dispatchers.IO).launch {
            tokenManager.saveFcmToken(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // 메시지에 알림 데이터가 포함되어 있는지 확인
        remoteMessage.notification?.let {
            showNotification(it.title ?: "WithUs", it.body ?: "")
        }
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "withus_default_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android 8.0 이상을 위한 채널 설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "일반 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "WithUs 앱의 기본 알림입니다."
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 알림 클릭 시 앱으로 이동하도록 설정
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // 알림 디자인 (중후하고 깔끔한 스타일)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.image_login_background) // 앱 아이콘으로 교체 필요
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}