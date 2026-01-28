package com.widthus.app.screen

// ImageMediaManager.kt
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.FileProvider
import com.google.accompanist.permissions.*
import java.io.File

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

/**
 * 카메라와 갤러리 실행을 담당하는 매니저 클래스
 */
class ImageMediaManager(
    private val launchGalleryAction: () -> Unit,
    private val launchCameraAction: () -> Unit,
    private val updateCallbackAction: ((Uri) -> Unit) -> Unit
) {
    fun launchGallery(onResult: (Uri) -> Unit) {
        updateCallbackAction(onResult) // 1. 결과 받을 콜백 교체
        launchGalleryAction()          // 2. 갤러리 실행
    }

    fun launchCamera(onResult: (Uri) -> Unit) {
        updateCallbackAction(onResult) // 1. 결과 받을 콜백 교체
        launchCameraAction()           // 2. 카메라 실행 (권한 체크 포함)
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberImageMediaManager(): ImageMediaManager {
    val context = LocalContext.current

    // 1. 현재 선택된 작업의 콜백을 저장할 상태 변수
    // (초기값은 아무것도 안 하는 빈 함수)
    var currentCallback by remember { mutableStateOf<(Uri) -> Unit>({}) }

    // 2. 임시 파일 URI 생성 (카메라 촬영용)
    val tempImageUri = remember {
        val file = File.createTempFile("temp_image_${System.currentTimeMillis()}", ".jpg", context.externalCacheDir)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    // 3. 갤러리 런처
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        // 결과가 오면 현재 설정된 콜백 실행
        if (uri != null) {
            currentCallback(uri)
        }
    }

    // 4. 카메라 런처
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentCallback(tempImageUri)
        }
    }

    // 5. 권한 상태 관리
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    // 6. 매니저 객체 생성 및 반환
    return remember(context, cameraPermissionState) {
        ImageMediaManager(
            launchGalleryAction = {
                galleryLauncher.launch("image/*")
            },
            launchCameraAction = {
                if (cameraPermissionState.status.isGranted) {
                    cameraLauncher.launch(tempImageUri)
                } else {
                    cameraPermissionState.launchPermissionRequest()
                }
            },
            updateCallbackAction = { newCallback ->
                currentCallback = newCallback
            }
        )
    }
}