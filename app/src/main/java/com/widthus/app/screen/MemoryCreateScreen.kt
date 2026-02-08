package com.widthus.app.screen

import AddTextBottomSheet
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.widthus.app.viewmodel.MainViewModel
import com.widthus.app.viewmodel.MemoryViewModel
import com.widthus.app.viewmodel.UploadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.withus.app.R
import kotlinx.coroutines.delay
import org.withus.app.debug
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

data class GalleryImage(
    val id: Long,
    val uri: Uri,
)


suspend fun fetchAllPhotos(context: Context): List<GalleryImage> = withContext(Dispatchers.IO) {
    val photoList = mutableListOf<GalleryImage>()
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATE_TAKEN
    )

    // 최신순 정렬
    val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        sortOrder
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val contentUri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id
            )
            photoList.add(GalleryImage(id, contentUri))
        }
    }
    photoList
}

suspend fun fetchGalleryImages(context: Context): List<GalleryImage> = withContext(Dispatchers.IO) {
    val images = mutableListOf<GalleryImage>()
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
    val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    context.contentResolver.query(queryUri, projection, null, null, sortOrder)?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val contentUri = ContentUris.withAppendedId(queryUri, id)
            images.add(GalleryImage(id, contentUri))
        }
    }
    images
}


enum class ManualCreateStep(val title: String, val progress: String) {
    PHOTO("12장의 사진을 선택해주세요", "1/3"),
    FRAME("프레임 색상을 선택해주세요", "2/3"),
    TEXT("문구를 작성해 주세요", "3/3"),
    RESULT("완성된 추억을 확인하세요", "완료");

    // 다음 단계로 이동하는 함수
    fun next(): ManualCreateStep {
        val nextIndex = this.ordinal + 1
        return if (nextIndex < entries.size) entries[nextIndex] else this
    }

    // 이전 단계로 이동하는 함수
    fun previous(): ManualCreateStep {
        val prevIndex = this.ordinal - 1
        return if (prevIndex >= 0) entries[prevIndex] else this
    }

}

@Composable
fun ManualCreateScreen(
    memoryViewModel: MemoryViewModel,
    onClose: () -> Unit,
    onSaveComplete: (Uri) -> Unit
) {

    var showExitDialog by remember { mutableStateOf(false) }

    val todayPlaceholder = remember {
        java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd"))
    }

    BackHandler(enabled = true) {
        showExitDialog = true
    }

    val uploadState by memoryViewModel.uploadState.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

    // 1. 상태 관리
    var currentStep by remember { mutableStateOf(ManualCreateStep.PHOTO) }
    var allPhotos by remember { mutableStateOf<List<GalleryImage>>(emptyList()) }
    val selectedImages = remember { mutableStateListOf<Uri>() }

    var frameColor by remember { mutableStateOf(Color.White) }
    var titleText by remember { mutableStateOf(todayPlaceholder) }

    if (showExitDialog) {
        CommonConfirmDialog(
            title = "수정을 종료하시겠어요?",
            content = "나가면 변경 내용이 사라질 수 있어요.\n저장이 되었는지 꼭 확인해 주세요.",
            confirmText = "종료하기",
            onDismiss = { showExitDialog = false }
        ) {
            showExitDialog = false
            onClose() // 다이얼로그에서 '종료하기' 누르면 실제로 화면 닫기
        }
    }

    // 업로드 상태 관찰
    LaunchedEffect(uploadState) {
        when (uploadState) {
            is UploadState.Success -> {
                Toast.makeText(context, "추억이 서버에 저장되었습니다.", Toast.LENGTH_SHORT).show()
                memoryViewModel.resetUploadState() // 상태 초기화
                onClose() // 화면 닫기
            }
            is UploadState.Error -> {
                Toast.makeText(context, (uploadState as UploadState.Error).message, Toast.LENGTH_SHORT).show()
                memoryViewModel.resetUploadState()
            }
            else -> {}
        }
    }

    // 2. 화면 진입 시 즉시 사진 로드 (내 앱 내부 로드)
    LaunchedEffect(Unit) {
        selectedImages.clear()
        // 권한이 이미 허용되어 있다는 가정하에 바로 로드합니다.
        // (실제 기기에서는 권한 체크 로직이 선행되어야 사진이 보입니다.)
        allPhotos = fetchAllPhotos(context)
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color.White)) {
        // 상단 헤더
        ManualHeader(
            title = when(currentStep) {
                ManualCreateStep.PHOTO -> "앨범"
                ManualCreateStep.FRAME -> "색상 선택"
                ManualCreateStep.TEXT,
                ManualCreateStep.RESULT -> "문구 작성"
            },
            onBack = {
                if (currentStep == ManualCreateStep.PHOTO) onClose()
                else currentStep = ManualCreateStep.PHOTO
            },
            onClose = {
                showExitDialog = true
            }
        )

        Box(modifier = Modifier.weight(1f)) {
            if (currentStep == ManualCreateStep.PHOTO) {
                // [내 앱 내부 갤러리 그리드]
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(1.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(allPhotos) { photo ->
                        val isSelected = selectedImages.contains(photo.uri)
                        val selectIndex =
                            if (isSelected) selectedImages.indexOf(photo.uri) + 1 else 0

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable {
                                    if (isSelected) selectedImages.remove(photo.uri)
                                    else if (selectedImages.size < 12) selectedImages.add(photo.uri)
                                }
                        ) {
                            AsyncImage(
                                model = photo.uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // 선택 시 주황색 번호 배지
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.3f))
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(24.dp)
                                        .background(Color.White, CircleShape)
                                        .border(1.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "$selectIndex",
                                        color = Color.Black,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // 프레임 미리보기 (편집 단계)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .aspectRatio(0.6f)
                        .drawWithContent {
                            graphicsLayer.record { this@drawWithContent.drawContent() }
                            drawLayer(graphicsLayer)
                        }) {
                        TwelveCutFrame(
                            images = List(12) { selectedImages.getOrNull(it) },
                            backgroundColor = frameColor,
                            contentColor = if (frameColor == Color.Black) Color.White else Color.Black,
                            title = titleText,
                            myProfileUrl = "", // todo - 프로필
                            partnerProfileUrl = "",
                            onSlotClick = {},
                            currentStep = currentStep
                        )
                    }
                }
            }
        }

        // 3. 하단 컨트롤러 (사진 선택 단계에서는 선택 목록 표시)
        if (currentStep == ManualCreateStep.PHOTO) {
            SelectedPhotosBar(
                selectedImages = selectedImages,
                onNext = {
                    val nextStep = ManualCreateStep.entries.getOrNull(currentStep.ordinal + 1)
                    if (nextStep != null) {
                        currentStep = nextStep
                    }
                }
            )
        } else {
            BottomEditController(
                currentStep = currentStep,
                frameColor = frameColor,
                onFrameColorChange = { frameColor = it },
                titleText = titleText,
                onTitleChange = { titleText = it },
                onNext = {
                    val nextStep = ManualCreateStep.entries.getOrNull(currentStep.ordinal + 1)
                    if (nextStep != null) {
                        currentStep = nextStep
                    }
                },
                onSave = {
                    scope.launch {
                        val bitmap = graphicsLayer.toImageBitmap()

                        memoryViewModel.uploadCustomMemory(bitmap.asAndroidBitmap(),)
                        saveImageBitmapToGallery(context, bitmap)?.let {
                            onSaveComplete(it)
                            onClose()
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ManualHeader(title: String, onClose: () -> Unit, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.Default.ArrowBack, contentDescription = null)
        }
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterEnd)) {
            Icon(Icons.Default.Close, contentDescription = null)
        }
    }
}

@Composable
fun ColorBtn(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF212121) else Color(0xFFD5D5D5 ),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}


@Composable
fun TwelveCutFrame(
    images: List<android.net.Uri?>,
    backgroundColor: Color,
    contentColor: Color,
    title: String,
    myProfileUrl: Any?,
    partnerProfileUrl: Any?,
    onSlotClick: (Int) -> Unit,
    currentStep: ManualCreateStep,
    onImageLoaded: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp) // 프레임 전체 패딩
    ) {
        // [1] 사진 영역 (3열 x 4행)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 4개의 행 생성
            for (rowIndex in 0 until 4) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 각 행마다 3개의 열 생성
                    for (colIndex in 0 until 3) {
                        val flatIndex = (rowIndex * 3) + colIndex

                        // 개별 사진 슬롯
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(Color(0xFFEEEEEE)) // 빈 공간 회색
                                .clickable { onSlotClick(flatIndex) }
                        ) {
                            if (images[flatIndex] != null) {
                                AsyncImage(
                                    model = images[flatIndex],
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    onSuccess = { onImageLoaded() }, // 성공 시 콜백
                                    onError = { onImageLoaded() }   // 실패 시에도 카운트는 올려야 캡처 진행됨
                                )
                            } else {
                                // 사진 없을 때 + 아이콘 표시
                                if (currentStep == ManualCreateStep.PHOTO) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // [2] 하단 Footer 영역 (타이틀 + 프로필)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // 왼쪽: 타이틀 (입력값이 없으면 기본 텍스트)
            Column {
                Text(
                    text = title.ifEmpty { "제목을 입력해주세요" },
                    color = contentColor,
                    fontSize = 20.sp, // 스크린샷처럼 크게
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif // 약간 감성적인 폰트
                )
            }

            // 오른쪽: 프로필 2개 (겹치게 또는 나란히)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "by",
                    color = contentColor,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(end = 8.dp)
                )

                // 프로필 이미지 2개
                Row(horizontalArrangement = Arrangement.spacedBy((2).dp)) {
                    ProfileCircle(
                        model = myProfileUrl,
                        borderColor = contentColor,
                        onSuccess = onImageLoaded // 여기 추가
                    )
                    ProfileCircle(
                        model = partnerProfileUrl,
                        borderColor = contentColor,
                        onSuccess = onImageLoaded // 여기 추가
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileCircle(
    model: Any?,
    borderColor: Color,
    onSuccess: () -> Unit = {}
) {
    var hasCalledSuccess by remember { mutableStateOf(false) }

    // [중요] 타임아웃 혹은 초기 실행 보장
    LaunchedEffect(model) {
        delay(2000) // 2초 이상 소요되면 그냥 완료된 걸로 간주 (캡처 진행을 위해)
        if (!hasCalledSuccess) {
            debug("ProfileCircle: 로딩 지연으로 인한 강제 완료 처리")
            hasCalledSuccess = true
            onSuccess()
        }
    }

    Box(
        modifier = Modifier
            .size(28.dp)
            .border(1.dp, borderColor, CircleShape)
            .padding(1.dp)
            .clip(CircleShape)
            .background(Color.LightGray)
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            onState = { state ->
                when (state) {
                    is AsyncImagePainter.State.Success, is AsyncImagePainter.State.Error -> {
                        if (!hasCalledSuccess) {
                            debug("ProfileCircle: 로드 완료 상태 진입 (${state::class.java.simpleName})")
                            hasCalledSuccess = true
                            onSuccess()
                        }
                    }
                    is AsyncImagePainter.State.Loading -> {
                        debug("ProfileCircle: 현재 로딩 중 (painter=${state.painter})")
                    }
                    else -> {
                        debug("ProfileCircle: 현재 예외 발생 (painter=${state.painter})")
                    }
                }
            }
        )
    }
}

// 색상 선택 버튼 (FourCutScreen의 FilterButton 등 재활용 가능)
@Composable
fun ColorSelectionButton(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color(0xFFFF5E00) else Color.Gray, // 선택 시 주황색 테두리
                shape = CircleShape
            )
            .padding(4.dp) // 테두리 간격
            .clip(CircleShape)
            .background(color)
            .clickable { onClick() }
    ) {
        // 검은색 버튼일 때 내부가 잘 안보이므로 체크 아이콘 추가 가능
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = if (color == Color.White) Color.Black else Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

// 헤더 (재사용성을 위해 수정)
@Composable
fun FourCutHeader(
    currentStepTitle: String,
    progress: String,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
    isNextVisible: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp)
    ) {
        IconButton(onClick = onBackClick, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.Default.Close, contentDescription = "닫기")
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(progress, fontSize = 12.sp, color = Color.Gray)
            Text(currentStepTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        if (isNextVisible) {
            TextButton(onClick = onNextClick, modifier = Modifier.align(Alignment.CenterEnd)) {
                Text("다음", fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}


// [2] 하단 선택 바 (스크린샷 하단부)
@Composable
fun SelectedPhotoBottomBar(
    selectedImages: List<Uri>,
    onRemove: (Uri) -> Unit,
    onNextClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .shadow(10.dp) // 상단 그림자
    ) {
        // 안내 문구 & 다음 버튼
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "12장의 사진을 선택해주세요.",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = onNextClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedImages.size > 0) Color(0xFFFF5E00) else Color.Gray
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (selectedImages.size > 0) "다음(${selectedImages.size})" else "다음",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 선택된 사진 썸네일 리스트
        if (selectedImages.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp) // 높이 고정
                    .padding(bottom = 12.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(selectedImages) { uri ->
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // 삭제 버튼 (X)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(16.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .clickable { onRemove(uri) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "삭제",
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GalleryGrid(
    allImages: List<GalleryImage>,
    selectedImages: List<Uri>,
    onImageClick: (Uri) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        items(allImages) { item ->
            val isSelected = selectedImages.contains(item.uri)
            val index = selectedImages.indexOf(item.uri) + 1

            Box(modifier = Modifier
                .aspectRatio(1f)
                .clickable { onImageClick(item.uri) }) {
                AsyncImage(
                    model = item.uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (isSelected) {
                    Box(Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(0.4f)))
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(24.dp)
                            .background(Color(0xFFFF5E00), CircleShape)
                            .border(1.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "$index",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BottomEditController(
    currentStep: ManualCreateStep,
    frameColor: Color,
    onFrameColorChange: (Color) -> Unit,
    titleText: String,
    onTitleChange: (String) -> Unit,
    onNext: () -> Unit,
    onSave: () -> Unit
) {
    var showAddSheet by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 20.dp,
        color = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .navigationBarsPadding()
        ) {
            when (currentStep) {
                ManualCreateStep.FRAME -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            currentStep.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Button(
                            onClick = onNext,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF45151)),
                            shape = RoundedCornerShape(25.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Text("다음", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ColorBtn(
                            text = "흰색",
                            modifier = Modifier.weight(1f),
                            isSelected = frameColor == Color.White,
                            onClick = { onFrameColorChange(Color.White) }
                        )
                        ColorBtn(
                            text = "검은색",
                            modifier = Modifier.weight(1f),
                            isSelected = frameColor == Color.Black,
                            onClick = { onFrameColorChange(Color.Black) }
                        )
                    }
                }

                ManualCreateStep.TEXT -> {
                    showAddSheet = true
                }
                ManualCreateStep.RESULT -> {

                    // 1. 상대방 코드 입력하기 버튼
                    Button(
                        onClick = {
                            showAddSheet = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .border(1.dp, Color.Black, RoundedCornerShape(8.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("문구 다시 작성하기", color = Color.Black)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. 내 코드로 초대하기 버튼
                    Button(
                        onClick = onSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("추억 생성하기", color = Color.White)
                    }
                }

                else -> {

                }
            }

            // 4. ManualCreateStep.TEXT 일 때만 바텀시트가 렌더링되도록 조건 추가
            if (showAddSheet) {
                AddTextBottomSheet(
                    title = "원하는 문구를 작성해보세요.",
                    placeholderText = titleText, // 생성한 날짜 전달
                    onDismissRequest = { showAddSheet = false },
                    onKeywordAdded = { newKeyword ->
                        onTitleChange(newKeyword) // 입력받은 문구를 titleText에 반영
                        showAddSheet = false
                        onNext()
                    }
                )
            }
        }
    }
}

@Composable
fun SelectedPhotosBar(
    selectedImages: SnapshotStateList<Uri>,
    onNext: () -> Unit
) {
    // 1. 자동 스크롤을 위한 상태 저장
    val listState = rememberLazyListState()

    // 사진이 추가될 때마다 마지막 아이템으로 스크롤
    LaunchedEffect(selectedImages.size) {
        if (selectedImages.isNotEmpty()) {
            listState.animateScrollToItem(selectedImages.size - 1)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 10.dp,
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(16.dp)) { // 패딩 약간 조절
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("12장의 사진을 선택해주세요.", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Button(
                    onClick = onNext,
                    enabled = selectedImages.size == 12,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF45151),
                        disabledContainerColor = Color.LightGray
                    ),
                    shape = RoundedCornerShape(25.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Text("다음 (${selectedImages.size})", color = Color.White, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. 간격을 8.dp로 줄이고 listState 연결
            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(selectedImages) { uri ->
                    // 3. X버튼이 이미지 안쪽 우측 상단에 위치하도록 설정
                    Box(modifier = Modifier.size(60.dp)) {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize() // Box 크기에 꽉 채움
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )

                        // X 버튼 (이미지 내부 우측 상단)
                        IconButton(
                            onClick = { selectedImages.remove(uri) },
                            modifier = Modifier
                                .size(20.dp) // 버튼 크기 살짝 조절
                                .align(Alignment.TopEnd) // 안쪽 우측 상단에 고정
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close_circle),
                                contentDescription = "삭제",
                                tint = Color.Unspecified // 원본 아이콘 색상 유지
                            )
                        }
                    }
                }
            }
        }
    }
}

fun getTargetWeekEndDate(): String {
    val today = LocalDate.now()

    // 오늘 기준 가장 가까운 토요일(오늘이 토요일이면 오늘 날짜 반환)을 구합니다.
    val nextSaturday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))

    // ISO-8601 형식 (YYYY-MM-DD)으로 포맷팅
    return nextSaturday.format(DateTimeFormatter.ISO_LOCAL_DATE)
}
