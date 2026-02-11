import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Picture
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.withus.app.debug
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.Locale
import kotlin.math.roundToInt

// --- 데이터 모델 ---
data class StickerData(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val icon: ImageVector? = null,
    val type: StickerType,
    var offsetX: Float = 0f,
    var offsetY: Float = 0f
)

enum class StickerType { TEXT, LOCATION, TAG }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(
    imageUri: Uri,
    isSent: Boolean,
    onClose: () -> Unit,
    onSendCompleteWithUri: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // [수정 1] Picture 제거하고 GraphicsLayer 생성
    val graphicsLayer = rememberGraphicsLayer()

    val stickers = remember { mutableStateListOf<StickerData>() }
    var isMenuVisible by remember { mutableStateOf(false) }
    var showTextInput by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var stickerToDelete by remember { mutableStateOf<StickerData?>(null) }

    val onTextComplete = { text: String ->
        if (text.isNotEmpty()) {
            stickers.add(StickerData(text = text, type = StickerType.TEXT))
        }
        showTextInput = false
    }

    // [수정 2] 저장 로직 변경 (Picture -> GraphicsLayer)
    fun saveCompositeImage() {
        if (isSent || isSaving) return

        isSaving = true
        scope.launch(Dispatchers.Default) {
            try {
                // Compose Bitmap -> Android Bitmap 변환
                // (import androidx.compose.ui.graphics.asAndroidBitmap 필요)
                val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()

                val savedUri = saveBitmapToGallery(context, bitmap)

                withContext(Dispatchers.Main) {
                    isSaving = false
                    if (savedUri != null) {
                        onSendCompleteWithUri(savedUri)
                    } else {
                        Toast.makeText(context, "이미지 생성 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isSaving = false
                    Toast.makeText(context, "에러 발생: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 상단 바
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
                IconButton(onClick = { saveCompositeImage() }) {
                    Icon(Icons.Default.Download, null, tint = Color.White)
                }
            }

            // --- [수정 3] 이미지 및 스티커 영역 (캡처 대상) ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    // [핵심] drawWithCache 제거 -> drawWithContent + record 사용
                    .drawWithContent {
                        graphicsLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(graphicsLayer)
                    }
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.DarkGray)
            ) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // 스티커 컨테이너
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 150.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    stickers.forEachIndexed { index, sticker ->
                        key(sticker.id) {
                            DraggableSticker(
                                data = sticker,
                                onDrag = { dragAmount ->
                                    val currentSticker = stickers[index]
                                    stickers[index] = currentSticker.copy(
                                        offsetX = currentSticker.offsetX + dragAmount.x,
                                        offsetY = currentSticker.offsetY + dragAmount.y
                                    )
                                },
                                onDeleteRequest = { stickerToDelete = sticker }
                            )
                        }
                    }
                }

                if (isSaving) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }

                if (isSent) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(80.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("전송 완료!", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 하단 컨트롤 바
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 30.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose, enabled = !isSaving) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
                Button(
                    onClick = { saveCompositeImage() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4B4B)),
                    shape = CircleShape,
                    modifier = Modifier.size(70.dp),
                    contentPadding = PaddingValues(0.dp),
                    enabled = !isSaving && !isSent
                ) {
                    Icon(
                        imageVector = if (isSent) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                IconButton(
                    onClick = { if (!isSent && !isSaving) isMenuVisible = true },
                    enabled = !isSaving
                ) {
                    Icon(Icons.Default.AutoFixHigh, null, tint = Color.White)
                }
            }
        }

        if (isMenuVisible && !isSaving) {

            // 꾸미기 메뉴 오버레이
            ModalBottomSheet(
                onDismissRequest = { isMenuVisible = false }, // 바깥 클릭이나 아래로 밀어서 닫을 때
                sheetState = rememberModalBottomSheetState(),
                containerColor = Color(0xFF222222), // 기존 배경색 유지
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                dragHandle = {
                    // 이미지처럼 상단 중앙에 회색 바 추가
                    BottomSheetDefaults.DragHandle(color = Color.Gray)
                }
            ) {
                EditorMenuBottomSheet(
                    onClose = { isMenuVisible = false },
                    onTextClick = { showTextInput = true; isMenuVisible = false },
                    onLocationClick = {
                        fetchLocation(context) { address ->
                            stickers.add(StickerData(text = address, type = StickerType.LOCATION))
                        }
                    },
                    onTagClick = { tag, icon ->
                        stickers.add(StickerData(text = tag, icon = icon, type = StickerType.TAG))
                    }
                )
            }
        }

        // 텍스트 입력 오버레이
        if (showTextInput) {
            TextEditorOverlay(
                onComplete = onTextComplete,
                onCancel = { showTextInput = false }
            )
        }

        // [추가 기능] 스티커 삭제 확인 다이얼로그
        if (stickerToDelete != null) {
            AlertDialog(
                onDismissRequest = { stickerToDelete = null },
                title = { Text("스티커 삭제") },
                text = { Text("이 스티커를 삭제하시겠습니까?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            stickers.remove(stickerToDelete)
                            stickerToDelete = null
                        }
                    ) { Text("삭제", color = Color.Red) }
                },
                dismissButton = {
                    TextButton(onClick = { stickerToDelete = null }) { Text("취소") }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraggableSticker(
    data: StickerData,
    onDrag: (Offset) -> Unit,
    onDeleteRequest: () -> Unit // [추가] 삭제 요청 콜백
) {
    // [지난번 수정 사항] 최신 상태 참조 보장
    val currentOnDrag by rememberUpdatedState(onDrag)

    Box(
        modifier = Modifier
            .offset { IntOffset(data.offsetX.roundToInt(), data.offsetY.roundToInt()) }
            // [중요] 드래그 감지
            .pointerInput(data.id) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    currentOnDrag(dragAmount)
                }
            }
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (data.type == StickerType.LOCATION) Color.Black.copy(alpha = 0.7f) else Color.White,
            contentColor = if (data.type == StickerType.LOCATION) Color.White else Color.Black,
            shadowElevation = 8.dp,
            modifier = Modifier
                // [추가 기능] 롱클릭 감지를 위해 combinedClickable 추가
                .combinedClickable(
                    onClick = {}, // 일반 클릭은 무시
                    onLongClick = { onDeleteRequest() } // 길게 누르면 삭제 요청
                )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (data.type == StickerType.LOCATION) {
                    Icon(
                        Icons.Default.Place,
                        null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.Red
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                } else if (data.icon != null) {
                    Icon(data.icon, null, modifier = Modifier.size(18.dp), tint = Color(0xFFFF9800))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(text = data.text, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
        }
    }
}

// --- [추가 유틸리티] 이미지 저장 관련 함수 ---

// Picture 객체에서 Bitmap 생성
fun createBitmapFromPicture(picture: Picture): Bitmap {
    val bitmap = Bitmap.createBitmap(
        picture.width,
        picture.height,
        Bitmap.Config.ARGB_8888
    )

    val canvas = Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.TRANSPARENT)
    canvas.drawPicture(picture)
    return bitmap
}

// 비트맵을 갤러리(MediaStore)에 저장
suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Uri? {
    val filename = "withus_photo_${System.currentTimeMillis()}.jpg"
    var fos: OutputStream? = null
    var imageUri: Uri? = null
    val contentResolver = context.contentResolver

    try {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/WithUs"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        imageUri =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        imageUri?.let { uri ->
            fos = contentResolver.openOutputStream(uri)
            fos?.let { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                contentResolver.update(uri, contentValues, null, null)
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
        imageUri = null
    } finally {
        fos?.close()
    }
    return imageUri
}


// --- 기존 코드 유지 ---
@Composable
fun TextEditorOverlay(onComplete: (String) -> Unit, onCancel: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable { onCancel() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable(enabled = false) { }
                .padding(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.Center) {
                            if (text.isEmpty()) {
                                Text("텍스트를 입력하세요", color = Color.Gray, fontSize = 24.sp)
                            }
                            innerTextField()
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { onComplete(text) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("완료", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun EditorMenuBottomSheet(
    onClose: () -> Unit,
    onTextClick: () -> Unit,
    onLocationClick: () -> Unit,
    onTagClick: (String, ImageVector) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFF222222),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            )
            .padding(bottom = 40.dp, top = 5.dp, start = 20.dp, end = 20.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            EditorMenuButton("Aa 텍스트", onTextClick)
            EditorMenuButton("📍 위치", onLocationClick)
            EditorMenuButton("🎵 음악") { }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            EditorMenuButton("😋 존맛탱") { onTagClick("존맛탱", Icons.Default.Face) }
            EditorMenuButton("👍 붐업") { onTagClick("붐업", Icons.Default.ThumbUp) }
            EditorMenuButton("👎 붐따") { onTagClick("붐따", Icons.Default.ThumbDown) }
        }
    }
}

@Composable
fun PhotoFlowScreen(
    onFinish: (Uri) -> Unit, // 최종 Uri를 전달할 콜백
    onCancel: () -> Unit     // 중간에 닫았을 때 처리
) {
    var capturedUri by remember { mutableStateOf<Uri?>(null) }
    // 편집 완료 후 저장된 Uri를 담을 상태
    var finalSavedUri by remember { mutableStateOf<Uri?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        if (capturedUri == null) {
            CameraCaptureScreen(
                onImageCaptured = { capturedUri = it },
                onClose = onCancel // 카메라 단계에서 닫기 버튼 대응
            )
        } else {
            PhotoEditorScreen(
                imageUri = capturedUri!!,
                isSent = false, // 초기값
                onClose = { capturedUri = null }, // 편집 취소 시 카메라로 이동
                onSendCompleteWithUri = { uri ->
                    debug("onSendCompleteWithUri : uri ")
                    // [핵심] 저장 완료 시 부모에게 Uri 전달
                    onFinish(uri)
                }
            )
        }
    }
}
@Composable
fun CameraCaptureScreen(onImageCaptured: (Uri) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraController = remember { LifecycleCameraController(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = {}) { Icon(Icons.Default.Close, null, tint = Color.White) }
            IconButton(onClick = {}) { Icon(Icons.Default.FlashOff, null, tint = Color.White) }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.DarkGray)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        controller = cameraController
                        cameraController.bindToLifecycle(lifecycleOwner)
                    }
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {}) {
                Icon(
                    Icons.Default.PhotoLibrary,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .border(4.dp, Color.White, CircleShape)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable {
                        val file = File.createTempFile("photo_", ".jpg", context.externalCacheDir)
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                        cameraController.takePicture(
                            outputOptions, ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    onImageCaptured(output.savedUri ?: Uri.fromFile(file))
                                }

                                override fun onError(exc: ImageCaptureException) {}
                            }
                        )
                    }
            )

            IconButton(onClick = {
                cameraController.cameraSelector =
                    if (cameraController.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
                        CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            }) {
                Icon(
                    Icons.Default.Cameraswitch,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
fun EditorMenuButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

fun fetchLocation(context: Context, onResult: (String) -> Unit) {
    try {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val geocoder = Geocoder(context, Locale.KOREA)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocation(
                            location.latitude,
                            location.longitude,
                            1
                        ) { addresses ->
                            val address = addresses.firstOrNull() ?: return@getFromLocation onResult("위치 정보 없음")
                            // 1. 전체 주소 (예: 서울특별시 강남구 역삼동 123-4)
                            val fullAddress = address.getAddressLine(0)

                            // 2. 구성 요소별 조합 (필요한 부분만 골라 쓰세요)
                            val city = address.locality ?: ""               // 시 (예: 서울특별시)
                            val district = address.subLocality ?: ""         // 구 (예: 강남구)
                            val dong = address.thoroughfare ?: ""            // 동/도로명 (예: 역삼동)
                            val feature = address.featureName ?: ""          // 건물 번호/지번 (예: 737)

                            // 결과 조합 예시: "강남구 역삼동"
                            val detailedLocation = listOf(district, dong).filter { it.isNotBlank() }.joinToString(" ")

                            onResult(detailedLocation.ifBlank { "알 수 없는 위치" })

                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val addresses =
                            geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        val address = addresses?.firstOrNull()
                        val city = address?.subLocality ?: address?.locality ?: "알 수 없는 곳"
                        onResult(city)
                    }
                } else {
                    onResult("위치 못찾음")
                }
            }
        } else {
            // 권한이 없는 경우 처리 (예: 기본값 전달 또는 권한 요청 유도)
            onResult("위치 권한 필요")
        }
    } catch (e: Exception) {
        onResult("위치 에러")
    }
}

@Composable
fun PhotoFlowDialog(
    onFinish: (Uri) -> Unit,
    onCancel: () -> Unit
) {
    // DialogProperties를 설정하여 시스템 기본 너비를 무시하고 전체 화면을 채웁니다.
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            usePlatformDefaultWidth = false, // 전체 화면 사용을 위해 필수
            dismissOnBackPress = true
        )
    ) {
        // 내부에서 PhotoFlowScreen을 호출
        Box(modifier = Modifier.fillMaxSize()) {
            PhotoFlowScreen(
                onFinish = onFinish,
                onCancel = onCancel
            )
        }
    }
}
