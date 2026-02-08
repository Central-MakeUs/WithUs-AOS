package com.widthus.app.screen

// FourCutScreen.kt
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Picture
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage // Coil 라이브러리 필요
import com.withus.app.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class FourCutStep(val title: String, val progress: String) {
    FRAME("프레임을 선택해주세요", "1/4"),
    PHOTO("사진을 선택해주세요", "2/4"),
    FILTER("필터를 선택해 주세요", "3/4"),
    RESULT("원하는 문구를 작성할 수 있어요", "4/4")
}

@Composable
fun FourCutScreen(
    mediaManager: ImageMediaManager,
    onClose: () -> Unit,       // 닫기 콜백 추가
    onSaveComplete: (android.net.Uri) -> Unit // 저장 완료 콜백 추가
) {
    // 단계 상태
    var currentStep by remember { mutableStateOf(FourCutStep.FRAME) }

    val graphicsLayer = rememberGraphicsLayer()
    val coroutineScope = rememberCoroutineScope() // 코루틴 스코프 필요
    val context = LocalContext.current

    // 데이터 상태
    var isWideFrame by remember { mutableStateOf(false) }
    var selectedSlotIndex by remember { mutableIntStateOf(0) }
    val selectedImages = remember { mutableStateListOf<android.net.Uri?>(null, null, null, null) }
    var isBlackAndWhite by remember { mutableStateOf(false) }
    var userText by remember { mutableStateOf("2025.09.10") } // 날짜/문구

    // 저장 완료 화면 상태
    var isSaveComplete by remember { mutableStateOf(false) }
    var showToast by remember { mutableStateOf(false) }

    var showExitDialog by remember { mutableStateOf(false) }

    // 스낵바 타이머 로직
    LaunchedEffect(showToast) {
        if (showToast) {
            delay(2000) // 2초 뒤 사라짐
            showToast = false
        }
    }

    if (showExitDialog) {
        ExitConfirmDialog(
            onDismiss = { showExitDialog = false },
            onConfirmExit = {
                showExitDialog = false
                onClose() // 진짜 종료
            }
        )
    }

    val focusManager = LocalFocusManager.current


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
    ) {
        // =================================================================
        // 1. 상단 영역 (저장 완료 시에는 숨기거나 타이틀로 변경)
        // =================================================================
        if (!isSaveComplete) {
            // [편집 중] 커스텀 헤더
            FourCutHeader(
                currentStep = currentStep,
                onBackClick = {
                    if (currentStep == FourCutStep.FRAME) {
                        onClose()
                    } else {
                        showExitDialog = true
//                        currentStep = when (currentStep) {
//                            FourCutStep.PHOTO -> FourCutStep.FRAME
//                            FourCutStep.FILTER -> FourCutStep.PHOTO
//                            FourCutStep.RESULT -> FourCutStep.FILTER
//                            else -> FourCutStep.FRAME
//                        }
                    }
                },
                onNextClick = {
                    when (currentStep) {
                        FourCutStep.FRAME -> currentStep = FourCutStep.PHOTO
                        FourCutStep.PHOTO -> currentStep = FourCutStep.FILTER
                        FourCutStep.FILTER -> currentStep = FourCutStep.RESULT
                        else -> {}
                    }
                }
            )
            Spacer(modifier = Modifier.height(20.dp))
        } else {
            // [저장 완료] 타이틀 표시
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp), // 헤더보다 조금 더 높게
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "커플 네컷이 완성됐어요!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        // =================================================================
        // 2. 메인 컨텐츠 (프레임 + 하단 요소)
        // =================================================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            // --- 프레임 영역 (재사용 컴포저블) ---
            Box(contentAlignment = Alignment.Center) {
                // ✅ 2. Modifier에 drawWithContent 적용
                Box(
                    modifier = Modifier
                        .drawWithContent {
                            // 그래픽 레이어에 기록
                            graphicsLayer.record {
                                this@drawWithContent.drawContent()
                            }
                            // 화면에 실제 그리기
                            drawLayer(graphicsLayer)
                        }
                ) {
                    FourCutFrameDisplay(
                        // ... (기존 파라미터 그대로 유지) ...
                        isWideFrame = isWideFrame,
                        isBlackAndWhite = isBlackAndWhite,
                        selectedImages = selectedImages,
                        userText = userText,
                        onTextChange = { userText = it },
                        currentStep = if (isSaveComplete) FourCutStep.RESULT else currentStep,
                        selectedSlotIndex = selectedSlotIndex,
                        onSlotClick = { idx -> selectedSlotIndex = idx },
                        isEditable = !isSaveComplete
                    )
                }

                // 토스트는 캡처되면 안되므로 캡처 Box 바깥에 둠
                if (showToast) {
                    CustomSaveToast()
                }
            }
            Spacer(modifier = Modifier.weight(1f))

            // =================================================================
            // 3. 하단 컨트롤 영역 (저장 완료 여부에 따라 분기)
            // =================================================================
            if (!isSaveComplete) {
                // [편집 중 UI]
                when (currentStep) {
                    FourCutStep.FRAME -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "원하는 프레임 모양을 선택해주세요",
                                fontSize = 16.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                FrameSelectionButton(
                                    R.drawable.ic_frame_2x2,
                                    !isWideFrame
                                ) { isWideFrame = false }
                                FrameSelectionButton(
                                    R.drawable.ic_frame_4x1,
                                    isWideFrame
                                ) { isWideFrame = true }
                            }
                        }
                    }

                    FourCutStep.PHOTO -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                mediaManager.launchGallery {
                                    selectedImages[selectedSlotIndex] = it
                                }
                            }) {
                                Icon(Icons.Default.PhotoLibrary, "갤러리", tint = Color.Black)
                            }
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .border(4.dp, Color.Black, CircleShape)
                                    .padding(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                                    .clickable {
                                        mediaManager.launchCamera {
                                            selectedImages[selectedSlotIndex] = it
                                        }
                                    }
                            )
                            IconButton(onClick = {}) {
                                Icon(
                                    Icons.Default.Cameraswitch,
                                    "전환",
                                    tint = Color.Black
                                )
                            }
                        }
                    }

                    FourCutStep.FILTER -> {
                        Row(horizontalArrangement = Arrangement.Center) {
                            FilterButton("원본", !isBlackAndWhite) { isBlackAndWhite = false }
                            Spacer(modifier = Modifier.width(16.dp))
                            FilterButton("흑백", isBlackAndWhite) { isBlackAndWhite = true }
                        }
                    }

                    FourCutStep.RESULT -> {
                        // ✅ "저장하기" 버튼 (클릭 시 완료 화면으로 전환)
                        Button(
                            onClick = {
                                isSaveComplete = true // 상태 변경 -> UI 전환
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(50.dp)
                        ) {
                            Text("저장하기")
                        }
                    }
                }
            } else {
                // [저장 완료 UI] - 버튼 2개
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. 갤러리에 저장하기 (흰색 배경)
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                // GraphicsLayer를 비트맵으로 변환 (비동기)
                                val bitmap = graphicsLayer.toImageBitmap()

                                // 갤러리 저장 함수 호출 (ImageBitmap -> AndroidBitmap 변환 필요)
//                                saveImageBitmapToGallery(context, bitmap)?.let {
//                                    onSaveComplete.invoke(it)
//                                    showToast = true
//                                } ?: run {
//                                    Toast.makeText(context, "저장 실패:", Toast.LENGTH_SHORT).show()
//                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                    ) {
                        Text("갤러리에 저장하기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. 목록으로 가기 (검은 배경)
                    Button(
                        onClick = {
                            onClose()
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222))
                    ) {
                        Text("목록으로 가기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

/**
 * 네컷 프레임을 보여주는 전용 컴포저블 (편집/완료 화면 공용)
 */
@Composable
fun FourCutFrameDisplay(
    isWideFrame: Boolean,
    isBlackAndWhite: Boolean,
    selectedImages: List<android.net.Uri?>,
    userText: String,
    onTextChange: (String) -> Unit,
    currentStep: FourCutStep,
    selectedSlotIndex: Int,
    onSlotClick: (Int) -> Unit,
    isEditable: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(if (isWideFrame) 0.5f else 0.85f)
            .aspectRatio(if (isWideFrame) 1f / 2.5f else 3f / 4f)
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 상단 날짜/문구 (RESULT 단계에서만 보임)
            if (currentStep == FourCutStep.RESULT) {
                BasicTextField(
                    value = userText,
                    onValueChange = if (isEditable) onTextChange else {
                        {}
                    }, // 완료 시 수정 불가
                    enabled = isEditable, // 완료 시 입력 비활성화
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = if (isWideFrame) 10.sp else 12.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    ),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 8.dp)
                        .fillMaxWidth()
                )
            }

            // 사진 배치
            if (isWideFrame) {
                // [4x1]
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (i in 0 until 4) {
                        FourCutPhotoSlot(
                            uri = selectedImages[i],
                            isBlackAndWhite = isBlackAndWhite,
                            isFocused = (!isEditable) || (currentStep != FourCutStep.PHOTO) || (i == selectedSlotIndex),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            onClick = { if (isEditable) onSlotClick(i) }
                        )
                    }
                }
            } else {
                // [2x2]
                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.weight(1f)) {
                        FourCutPhotoSlot(
                            uri = selectedImages[0], isBlackAndWhite = isBlackAndWhite,
                            isFocused = (!isEditable) || (currentStep != FourCutStep.PHOTO) || (selectedSlotIndex == 0),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) { if (isEditable) onSlotClick(0) }
                        Spacer(modifier = Modifier.width(8.dp))
                        FourCutPhotoSlot(
                            uri = selectedImages[1], isBlackAndWhite = isBlackAndWhite,
                            isFocused = (!isEditable) || (currentStep != FourCutStep.PHOTO) || (selectedSlotIndex == 1),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) { if (isEditable) onSlotClick(1) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.weight(1f)) {
                        FourCutPhotoSlot(
                            uri = selectedImages[2], isBlackAndWhite = isBlackAndWhite,
                            isFocused = (!isEditable) || (currentStep != FourCutStep.PHOTO) || (selectedSlotIndex == 2),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) { if (isEditable) onSlotClick(2) }
                        Spacer(modifier = Modifier.width(8.dp))
                        FourCutPhotoSlot(
                            uri = selectedImages[3], isBlackAndWhite = isBlackAndWhite,
                            isFocused = (!isEditable) || (currentStep != FourCutStep.PHOTO) || (selectedSlotIndex == 3),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) { if (isEditable) onSlotClick(3) }
                    }
                }
            }
            // 로고
            Text(
                text = "WITHUS",
                color = Color.White,
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp)
            )
        }
    }
}

/**
 * 저장 완료 알림 토스트 (스낵바)
 */
@Composable
fun CustomSaveToast(
    message: String = "사진 저장이 완료 되었어요!"
) {
    Box(
        modifier = Modifier
            .padding(bottom = 40.dp) // 위치 조정
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp)) // 그림자 (선택)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 체크 아이콘 (원형 배경)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color.Black, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

// 쉐도우 효과를 위한 확장 함수 (필요 시 없어도 됨)
fun Modifier.shadow(
    elevation: androidx.compose.ui.unit.Dp,
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.ui.graphics.RectangleShape,
    clip: Boolean = elevation > 0.dp
) = this.then(
    Modifier.drawBehind {
        drawIntoCanvas { canvas ->
            val paint = androidx.compose.ui.graphics.Paint()
            val frameworkPaint = paint.asFrameworkPaint()
            if (elevation > 0.dp) {
                frameworkPaint.setShadowLayer(
                    elevation.toPx(),
                    0f,
                    0f,
                    android.graphics.Color.BLACK
                )
            }
        }
    }
)

/**
 * 상단 헤더 컴포저블
 */
@Composable
fun FourCutHeader(
    currentStep: FourCutStep,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp)
    ) {
        IconButton(onClick = onBackClick, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(painterResource(id = R.drawable.ic_close), "닫기", tint = Color.Black)
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(currentStep.progress, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(currentStep.title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        if (currentStep != FourCutStep.RESULT) {
            IconButton(onClick = onNextClick, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(painterResource(id = R.drawable.ic_check), "다음", tint = Color.Black)
            }
        }
    }
}

@Composable
fun FrameSelectionButton(
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // 선택 여부에 따른 스타일 변수
    val borderColor = if (isSelected) Color.Black else Color.LightGray
    val borderWidth = if (isSelected) 3.dp else 1.dp
    val containerSize = 70.dp // 터치 영역 확보

    Box(
        modifier = Modifier
            .size(containerSize)
            // 1. 테두리 적용 (둥근 모서리)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            // 2. 배경색 (선택되면 아주 연한 회색 깔기 - 선택사항)
            .background(
                color = if (isSelected) Color.Black.copy(alpha = 0.05f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp)) // 클릭 효과도 둥글게
            .clickable { onClick() }
            .padding(12.dp), // 아이콘과 테두리 사이 여백
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.Unspecified, // 원본 색상 유지
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun FourCutPhotoSlot(
    uri: android.net.Uri?,
    isBlackAndWhite: Boolean,
    isFocused: Boolean = true, // ✅ 포커스 상태 추가 (기본값 true)
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colorFilter =
        if (isBlackAndWhite) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null

    Box(
        modifier = modifier
            .background(Color.White)
            .clickable { onClick() }
    ) {
        // 1. 이미지 표시
        if (uri != null) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter,
                modifier = Modifier.fillMaxSize()
            )
        } else {
//            // 빈 이미지일 때 (포커스 여부에 따라 아이콘 색상 변경 등 가능)
//            Icon(
//                painter = painterResource(R.drawable.ic_add), // + 아이콘이 있다고 가정
//                contentDescription = null,
//                tint = Color.LightGray,
//                modifier = Modifier.align(Alignment.Center).size(24.dp)
//            )
        }

        // 2. ✅ 포커스가 아닐 때 어두운 오버레이 씌우기 (Dimming Effect)
        if (!isFocused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)) // 투명도 60% 검은색
            )
        }

        // (선택사항) 포커스된 슬롯에 테두리를 주고 싶다면:
        /*
        if (isFocused && uri == null) { // 이미지가 없을 때만 테두리 표시 등
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, Color.Yellow)
            )
        }
        */
    }
}

@Composable
fun FilterButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color.Black else Color(0xFFF0F0F0),
            contentColor = if (isSelected) Color.White else Color.Black
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .width(100.dp)
            .height(48.dp)
    ) {
        Text(text)
    }
}


fun saveImageBitmapToGallery(context: Context, imageBitmap: ImageBitmap): Uri? {
    // 1. Picture -> Bitmap 변환
    val bitmap = imageBitmap.asAndroidBitmap()

    // 2. 갤러리에 저장 (MediaStore 사용)
    val filename = "WITHUS_${System.currentTimeMillis()}.jpg"
    var fos: java.io.OutputStream? = null
    var uri: android.net.Uri? = null

    try {
        val contentResolver = context.contentResolver
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

        uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            fos = contentResolver.openOutputStream(it)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos!!)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri != null) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            contentResolver.update(uri, contentValues, null, null)
        }

        return uri

    } catch (e: Exception) {
        e.printStackTrace()
        return null
    } finally {
        fos?.close()
    }
}


@Composable
fun ExitConfirmDialog(
    onDismiss: () -> Unit,
    onConfirmExit: () -> Unit
) {
    // Dialog 배경 투명처리 및 설정
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth(0.9f) // 좌우 여백 약간 줌
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. 타이틀 및 메시지 영역
                Column(
                    modifier = Modifier.padding(top = 32.dp, bottom = 24.dp, start = 16.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "수정을 종료하시겠어요?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "나가면 변경 내용이 사라질 수 있어요.\n저장이 되었는지 꼭 확인해 주세요.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = Color(0xFF888888), // 연한 회색
                        lineHeight = 20.sp
                    )
                }

                Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)

                // 2. 종료하기 버튼 (빨간색)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onConfirmExit() }
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "종료하기",
                        color = Color(0xFFFF3B30), // iOS 스타일 레드
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)

                // 3. 취소 버튼 (검은색/기본)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDismiss() }
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "취소",
                        color = Color.Black,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
