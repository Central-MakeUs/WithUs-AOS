package com.widthus.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.messaging.FirebaseMessaging
import com.widthus.app.fragment.ComposeFragment
import com.withus.app.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.withus.app.token.TokenManager
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var tokenManager: TokenManager // 필요 시 토큰 저장을 위해 주입

    private val requestMultiplePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach { (permission, isGranted) ->
            when (permission) {
                Manifest.permission.ACCESS_FINE_LOCATION -> {
                    if (isGranted) Log.d("Permission", "위치 권한 허용")
                }
                Manifest.permission.CAMERA -> {
                    if (isGranted) Log.d("Permission", "카메라 권한 허용")
                }
                Manifest.permission.POST_NOTIFICATIONS -> {
                    if (isGranted) Log.d("Permission", "알림 권한 허용")
                }
                // 안드로이드 12 이하 저장소 권한
                Manifest.permission.READ_EXTERNAL_STORAGE -> {
                    if (isGranted) Log.d("Permission", "저장소 권한 허용")
                }
                Manifest.permission.READ_MEDIA_IMAGES -> {
                    if (isGranted) Log.d("Permission", "안드로이드 13+ 사진 권한 허용")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkAllPermissions()
        if (savedInstanceState == null) {
            navigateComposeFragment()
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // 현재 토큰 가져오기
            val token = task.result
            Log.d("FCM", "Current Token: $token")

            // 수동으로 저장소에 업데이트
            CoroutineScope(Dispatchers.IO).launch {
                tokenManager.saveFcmToken(token)
            }
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)

            val controller = window.insetsController
            if (controller != null) {
                controller.hide(WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }

        onBackPressedDispatcher.addCallback(this) {
            navigateBack()
        }
    }
    private fun checkAllPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA
        )

        // 안드로이드 13(TIRAMISU) 이상일 때 알림 및 미디어 권한 추가
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            // 12 이하일 때는 외부 저장소 읽기 권한
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        requestMultiplePermissions.launch(permissions.toTypedArray())
    }
    fun navigateComposeFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, ComposeFragment.newInstance())
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }

    fun navigateBack() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

