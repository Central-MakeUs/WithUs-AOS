package com.widthus.app.screen

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.widthus.app.model.LocalPartnerNickname
import com.widthus.app.model.LocalUserNickname
import com.widthus.app.viewmodel.AuthViewModel
import com.widthus.app.viewmodel.MainViewModel
import com.widthus.app.viewmodel.MemoryViewModel
import com.withus.app.R
import kotlinx.coroutines.delay
import org.withus.app.debug
import org.withus.app.model.MemoryStatus
import org.withus.app.model.WeekMemorySummary
import org.withus.app.repository.TestTest
import java.time.LocalDate


@Composable
fun MemoryArchiveScreen(
    viewModel: MemoryViewModel = hiltViewModel(),
    mainViewModel: MainViewModel,
    mediaManager: ImageMediaManager
) {
    // 1. 플로우 진입 여부를 관리하는 상태

    val isCreatingManual = mainViewModel.isCreatingManual

    if (isCreatingManual) {
        // 2. 플로우 진입 시 보여줄 화면
        ManualCreateScreen(
            memoryViewModel = viewModel,
            onClose = { mainViewModel.isCreatingManual = false }, // 닫기 버튼 시 다시 배너 화면으로
            onSaveComplete = { uri ->
                // 저장 완료 후 로직 (예: 서버 전송 또는 상태 해제)
                mainViewModel.isCreatingManual = false
            })
    } else {
        MemoryScreen(
            viewModel = viewModel,
            onCreateClick = { mainViewModel.isCreatingManual = true })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MemoryScreen(
    viewModel: MemoryViewModel,
    onCreateClick: () -> Unit // 함수 타입을 인자로 받음
) {
    var showSheet by remember { mutableStateOf(false) }
    val memoryData = viewModel.memoryData

    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    var captureTargetSummary by remember { mutableStateOf<WeekMemorySummary?>(null) }
    var loadedCount by remember { mutableIntStateOf(0) }
    val targetCount = 14

    LaunchedEffect(loadedCount) {
        if (captureTargetSummary != null && loadedCount >= targetCount) {
            delay(800) // 모든 요소가 그려질 최종 여유 시간
            val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()

            // 서버 업로드
            viewModel.uploadCustomMemory(bitmap, captureTargetSummary?.weekEndDate ?: "")

            // 상태 초기화
            captureTargetSummary = null
            loadedCount = 0
        }
    }

    // 진입 시 데이터 로드
    LaunchedEffect(Unit) {
        viewModel.fetchMemories()
    }

    Scaffold(
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            // 4. [캡처 전용 오버레이]
            if (captureTargetSummary != null) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    // 사용자에게는 로딩 바만 보여줌
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("추억을 생성하고 있어요...", color = Color.White)
                    }

                    // [핵심] 로딩 바 아래에 숨겨서 실제로 렌더링 (캡처 대상)
                    Box(
                        modifier = Modifier
                            .size(360.dp, 600.dp) // 표준화된 캡처 사이즈 고정
                            .alpha(0.01f) // 아주 미세하게 남겨 렌더링 강제
                            .drawWithContent {
                                graphicsLayer.record { this@drawWithContent.drawContent() }
                                // drawLayer(graphicsLayer) // 화면에 실제로 그릴 필요는 없음
                            }
                    ) {
                        TwelveCutFrame(
                            images = captureTargetSummary!!.needCreateImageUrls.map { Uri.parse(it) },
                            backgroundColor = Color.White,
                            contentColor = Color.Black,
                            title = captureTargetSummary!!.title.split(" (").first(),
                            myProfileUrl = TestTest.testImageUrl, // 실제 프로필 데이터
                            partnerProfileUrl = TestTest.testImageUrl,
                            onSlotClick = {},
                            currentStep = ManualCreateStep.RESULT,
                            onImageLoaded = { loadedCount++ }
                        )
                    }
                }
            }

            // 상단 영역
            Column(modifier = Modifier.padding(20.dp)) {
                Text("추억", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))
                ManualCreateBanner(
                    onClick = onCreateClick, nickName = LocalUserNickname.current.nickname.text, LocalPartnerNickname.current.nickname.text
                )
                Spacer(modifier = Modifier.height(30.dp))
                MonthSelector(viewModel.currentMonth) { showSheet = true }
            }

            // 주차별 리스트 (가로 스크롤)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                memoryData?.weekMemorySummaries?.let { items ->
                    items(items) { summary ->
                        MemoryWeekCard(summary, memoryViewModel = viewModel, onAutoCreateClick = { captureTargetSummary = it })
                    }
                }
            }
        }
    }

    // 월 선택 바텀시트 (image_f1cc7e 구현)
    if (showSheet) {
        MonthSelectionBottomSheet(
            currentDate = viewModel.currentMonth,
            onDismiss = { showSheet = false },
            onMonthSelected = { selectedDate ->
                viewModel.fetchMemories(selectedDate)
                showSheet = false
            })
    }
}

@Composable
fun MemoryWeekCard(
    summary: WeekMemorySummary,
    memoryViewModel: MemoryViewModel,
    onAutoCreateClick: (WeekMemorySummary) -> Unit
) {

    val weekTitle = summary.title.split(" (").firstOrNull() ?: summary.title

    Column(modifier = Modifier.width(280.dp)) {
        Text(text = summary.title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(12.dp))
        ) {
            when (summary.status) {
                MemoryStatus.UNAVAILABLE -> {
                    // [UNAVAILABLE] 사진 부족 (이미지 f1c918 왼쪽)
                    Image(
                        painter = painterResource(id = R.drawable.img_background_blur), // 블러된 배경
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                    )
                    Text(
                        text = "두명 모두 6장 이상\n사진을 보내면\n추억이 자동 생성돼요.",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }

                MemoryStatus.NEED_CREATE -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                onAutoCreateClick(summary)
                            }) {
                        // 1. 기존 배경 및 텍스트 UI
                        Image(
                            painter = painterResource(id = R.drawable.img_background_blur),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = "${weekTitle} 추억이 만들어졌어요.\n화면을 터치해 확인해 보세요!",
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }

                MemoryStatus.CREATED -> {
                    // [CREATED] 생성 완료 (이미지 f1c9b7)
                    AsyncImage(
                        model = summary.createdImageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun ManualCreateBanner(
    onClick: () -> Unit,
    nickName: String, partnerNickname: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .clickable { onClick() }, // 클릭 시 플로우 시작
        shape = RoundedCornerShape(16.dp), color = Color(0xFF222222)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${nickName}님과 ${partnerNickname}님이 함께한\n추억을 직접 만들어 보세요!",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 22.sp
            )

            // 화살표 아이콘 영역
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "시작하기",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun MonthSelector(
    currentDate: LocalDate, onClick: () -> Unit
) {
    Row(
        modifier = Modifier.clickable { onClick() }, verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${currentDate.year}년 ${currentDate.monthValue}월",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthSelectionBottomSheet(
    currentDate: LocalDate,
    onDismiss: () -> Unit,
    onMonthSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val daysUntilSaturday = (6 - today.dayOfWeek.value).let { if (it < 0) 0 else it }
    val saturdayOfThisWeek = today.plusDays(daysUntilSaturday.toLong())

    // 규칙: 이번 주 토요일이 다음 달에 걸쳐있으면 다음 달까지 허용
    val maxDate = saturdayOfThisWeek.withDayOfMonth(1)

    var tempYear by remember { mutableIntStateOf(currentDate.year) }
    var tempMonth by remember { mutableIntStateOf(currentDate.monthValue) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFDDDDDD)) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 연도 선택
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { tempYear-- }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = null)
                }
                Text(text = "${tempYear}년", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(
                    onClick = { if (tempYear < maxDate.year) tempYear++ },
                    enabled = tempYear < maxDate.year // 연도 증가 제한
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = if (tempYear < maxDate.year) Color.Black else Color.LightGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 월 그리드
            val months = (1..12).toList()
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                months.chunked(3).forEach { rowMonths ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowMonths.forEach { month ->
                            // 선택 가능 여부 계산
                            val isEnabled = LocalDate.of(tempYear, month, 1) <= maxDate

                            MonthItem(
                                month = month,
                                isSelected = month == tempMonth,
                                isEnabled = isEnabled, // 추가된 파라미터
                                modifier = Modifier.weight(1f),
                                onClick = { if (isEnabled) tempMonth = month }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = { onMonthSelected(LocalDate.of(tempYear, tempMonth, 1)) },
                enabled = LocalDate.of(tempYear, tempMonth, 1) <= maxDate,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF222222),
                    disabledContainerColor = Color(0xFFEEEEEE) // 비활성화 시 색상
                )
            ) {
                Text("날짜 선택", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MonthItem(
    month: Int,
    isSelected: Boolean,
    isEnabled: Boolean, // 선택 가능 여부
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isSelected -> Color(0xFF222222)
                    else -> Color.Transparent
                }
            )
            .clickable(enabled = isEnabled) { onClick() }, // 클릭 비활성화
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${month}월",
            color = when {
                isSelected -> Color.White
                !isEnabled -> Color(0xFFDDDDDD) // 비활성화된 월은 아주 흐릿하게
                else -> Color(0xFF666666)
            },
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun AutoCaptureFrame(
    imageUrls: List<String>,
    title: String,
    myProfileUrl: String?,
    partnerProfileUrl: String?,
    graphicsLayer: GraphicsLayer,
    onReadyToCapture: () -> Unit
) {
    // 총 14장의 이미지 로딩 상태 추적 (메인 12장 + 프로필 2장)
    var loadedImagesCount by remember { mutableIntStateOf(0) }
    val totalRequired = imageUrls.size

    debug("loadedImagesCount : $loadedImagesCount")

    LaunchedEffect(loadedImagesCount) {
        if (loadedImagesCount >= totalRequired) {
            // 모든 이미지가 로드된 후 렌더링이 완료될 시간을 넉넉히 줌 (에러 방지)
            delay(500)
            debug("onReadyToCapture !")
            onReadyToCapture()
        }
    }

    // 캡처 대상이 되는 루트 컨테이너
    Column(
        modifier = Modifier
            .width(360.dp) // 캡처될 이미지의 기준 너비 설정
        .wrapContentHeight()
            .drawWithContent {
                // graphicsLayer에 전체 내용을 기록
                graphicsLayer.record {
                    this@drawWithContent.drawContent()
                }
            }
            .background(Color.White) // 배경색 흰색 고정
        .padding(16.dp)) {
        // [1] 사진 그리드 영역 (4행 3열)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            imageUrls.chunked(3).forEach { rowImages ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    rowImages.forEach { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            contentScale = ContentScale.Crop,
                            onSuccess = { loadedImagesCount++ },
                            onError = { loadedImagesCount++ } // 에러 시에도 카운트는 올려야 캡처 진행됨
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // [2] 하단 Footer (텍스트 + 프로필)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = title.ifEmpty { "우리의 추억" },
                color = Color.Black,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "by",
                    color = Color.Black,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                    ProfileCircleForCapture(myProfileUrl) {
                        debug("ProfileCircleForCapture onSuccess ! ")
                        loadedImagesCount++
                    }
                    ProfileCircleForCapture(partnerProfileUrl) { loadedImagesCount++ }
                }
            }
        }
    }
}

@Composable
fun ProfileCircleForCapture(model: String?, onSuccess: () -> Unit) {
    AsyncImage(
        model = model,
        contentDescription = null,
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .border(1.5.dp, Color.White, CircleShape),
        contentScale = ContentScale.Crop,
        onSuccess = { onSuccess() },
        onError = { onSuccess() })
}

@Composable
fun AutoFrameCompositeLayout(
    imageUrls: List<String>,
    title: String,
    graphicsLayer: GraphicsLayer,
    onAllImagesLoaded: () -> Unit
) {
    var loadedCount by remember { mutableIntStateOf(0) }

    // 모든 이미지 로드 완료 체크
    LaunchedEffect(loadedCount) {
        if (loadedCount >= 12) { // 12장이 모두 로드되면
            delay(500) // 렌더링 안정화를 위해 살짝 대기
            onAllImagesLoaded()
        }
    }

    // 실제 캡처될 디자인 (흰색 배경 프레임)
    Column(
        modifier = Modifier
            .width(360.dp) // 적절한 출력 너비
        .drawWithContent {
            graphicsLayer.record { this@drawWithContent.drawContent() }
        }
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        // 4x3 또는 3x4 그리드 (12장)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(4) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(3) { col ->
                        val index = row * 3 + col
                        AsyncImage(
                            model = imageUrls.getOrNull(index),
                            contentDescription = null,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            contentScale = ContentScale.Crop,
                            onSuccess = { loadedCount++ })
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 하단 타이틀 (검은색 글자)
        Text(
            text = title, color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold
        )
    }
}
