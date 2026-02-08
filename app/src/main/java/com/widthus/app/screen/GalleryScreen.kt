package com.widthus.app.screen

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.widthus.app.model.MemorySet
import com.widthus.app.model.QuestionAnswer
import com.widthus.app.viewmodel.MainViewModel
import com.withus.app.R
import kotlinx.coroutines.launch
import org.withus.app.debug
import org.withus.app.model.ArchiveDetailItem
import org.withus.app.model.ArchiveQuestionItem
import org.withus.app.model.CalendarDayInfo
import org.withus.app.model.CoupleQuestionData
import org.withus.app.model.QuestionDetailResponse
import org.withus.app.model.UserAnswerInfo
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class ViewMode { LATEST, CALENDAR, QUESTION }

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: MainViewModel = hiltViewModel(),
) {
    var selectedQuestionIndex by remember { mutableStateOf<Int?>(null) }
    var viewMode by remember { mutableStateOf(ViewMode.LATEST) }
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() } // ID를 Int(coupleQuestionId)로 관리
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isEmpty = when (viewMode) {
        ViewMode.LATEST -> viewModel.archiveItems.isEmpty()
        ViewMode.QUESTION -> viewModel.archiveQuestions.isEmpty()
        ViewMode.CALENDAR -> viewModel.calendarDays.isEmpty()
    }

    LaunchedEffect(viewMode) {
        when (viewMode) {
            ViewMode.LATEST -> {
                if (viewModel.archiveItems.isEmpty()) viewModel.fetchArchives(true)
            }

            ViewMode.QUESTION -> {
                if (viewModel.archiveQuestions.isEmpty()) viewModel.fetchQuestionArchives(true)
            }

            ViewMode.CALENDAR -> {
                val date = viewModel.currentCalendarDate
                viewModel.fetchCalendar(date.year, date.monthValue)
            }
        }
    }

    val detailData = viewModel.selectedQuestionDetail

    val detailDataList = viewModel.detailList // 리스트 전체를 가져옵니다

    debug("selectedQuestionIndex : $selectedQuestionIndex")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        if (selectedQuestionIndex != null && detailData != null) {
            DetailListWrapper(
                items = detailDataList,
                initialIndex = viewModel.scrollIndex,
                onBack = {
                    selectedQuestionIndex = null
                    viewModel.detailList = emptyList() // 데이터 초기화
                },
                onDelete = {
                    // 삭제 API 연결
                    showDeleteDialog = true
                }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                Scaffold(topBar = {
                    Column(modifier = Modifier.background(Color.White)) {
                        CenterAlignedTopAppBar(
                            title = { Text("추억", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                            actions = {
                                if (!isSelectionMode) {
                                    IconButton(onClick = { isSelectionMode = true }) {
                                        Icon(Icons.Default.MoreHoriz, "더보기", tint = Color.Black)
                                    }
                                } else {
                                    TextButton(onClick = {
                                        isSelectionMode = false
                                        selectedIds.clear()
                                    }) { Text("취소", color = Color.Black) }
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
                        )

                        if (!isSelectionMode) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                ViewModeToggle(
                                    currentMode = viewMode, onModeChanged = { viewMode = it })
                            }
                        }
                    }
                }, bottomBar = {
                    if (isSelectionMode) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .navigationBarsPadding()
                        ) {
                            Button(
                                onClick = { showDeleteDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFFFFFF
                                    )
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                enabled = selectedIds.isNotEmpty()
                            ) {
                                Text(
                                    "${selectedIds.size}장의 사진 삭제하기",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }) { paddingValues ->

                    if (isEmpty && !viewModel.isLoading) {
                        ArchiveEmptyView()
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {

                                when (viewMode) {
                                    ViewMode.LATEST -> {
                                        LatestGridView(
                                            items = viewModel.archiveItems, // (String, UserAnswerInfo) 페어 리스트
                                            isSelectionMode = isSelectionMode,
                                            selectedIds = selectedIds,
                                            onToggleSelect = { id ->
                                                // 선택 로직: 리스트에 있으면 제거, 없으면 추가
                                                if (selectedIds.contains(id)) selectedIds.remove(id)
                                                else selectedIds.add(id)
                                            },
                                            onLongClick = { id ->
                                                if (!isSelectionMode) {
                                                    isSelectionMode = true
                                                    selectedIds.add(id)
                                                }
                                            },
                                            onItemClick = { index ->
                                                val (date, info) = viewModel.archiveItems[index]

                                                // 2. 상세 API 호출 (아이템 식별을 위해 id와 type 전달)
                                                viewModel.fetchDetailByDate(
                                                    date = date,
                                                    targetId = info.userId,          // UserAnswerInfo에 id 필드가 있어야 함
//                                            targetType = info. // UserAnswerInfo에 archiveType 필드가 있어야 함
                                                )

                                                // 3. 인덱스 설정 (이 값이 null이 아니게 되어 상세 화면이 뜸)
                                                selectedQuestionIndex = index
                                            },
                                            loadMore = {
                                                viewModel.fetchArchives() // 스크롤 하단 도달 시 추가 데이터 로드
                                            })
                                    }

                                    ViewMode.CALENDAR -> {
                                        CalendarListView(
                                            viewModel = viewModel, onDateClick = { clickedDate ->
                                                viewModel.fetchDetailByDate(date = clickedDate)
                                                selectedQuestionIndex = 0
                                            })
                                    }

                                    ViewMode.QUESTION -> {
                                        QuestionListView(
                                            questions = viewModel.archiveQuestions,
                                            onQuestionClick = { index, item ->
                                                // 상세 API 호출
                                                viewModel.fetchQuestionDetail(item.coupleQuestionId)
                                                // 화면 전환 상태 업데이트
                                                selectedQuestionIndex = index
                                            },
                                            loadMore = { viewModel.fetchQuestionArchives() })
                                    }
                                }

                            }
                        }
                    }
                }

                // === 삭제 확인 다이얼로그 ===
                if (showDeleteDialog) {
                    DeleteConfirmDialog(
                        count = selectedIds.size,
                        onDismiss = { showDeleteDialog = false },
                        onConfirm = {
                            // 실제 삭제 로직 수행
                            // viewModel.delete(selectedIds)
                            showDeleteDialog = false
                            isSelectionMode = false
                            selectedIds.clear()
                        })
                }
            }
        }

    }}

// ====================================================================
// 🧩 하위 컴포넌트들
// ====================================================================

@Composable
fun ToggleOption(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color(0xFF222222) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.Gray,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

// 2. 최신순 그리드 뷰 (날짜 배지 + 선택 체크박스)
@OptIn(ExperimentalFoundationApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LatestGridView(
    items: List<Pair<String, UserAnswerInfo>>, // 변경된 타입
    isSelectionMode: Boolean,
    selectedIds: List<Long>,
    onToggleSelect: (Long) -> Unit,
    onLongClick: (Long) -> Unit,
    onItemClick: (Int) -> Unit,
    loadMore: () -> Unit // 페이지네이션 콜백 추가
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(1.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        itemsIndexed(items) { index, item ->
            // [페이지네이션] 리스트 끝에 도달하면 다음 페이지 요청
            if (index >= items.size - 1) {
                LaunchedEffect(Unit) { loadMore() }
            }

            val dateString = item.first // "2026-01-28"
            val info = item.second     // 이미지 정보

            val itemId = info.userId.toLong() // 임시 식별자

            Box(modifier = Modifier
                .aspectRatio(3f / 4f)
                .combinedClickable(onClick = {
                    if (isSelectionMode) onToggleSelect(itemId)
                    else onItemClick(index)
                }, onLongClick = { onLongClick(itemId) })) {
                // 이미지 표시
                AsyncImage(
                    model = info.questionImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // 날짜 배지 표시 (예: 1월 28일)
                val formattedDate = remember(dateString) {
                    try {
                        val date = LocalDate.parse(dateString)
                        "${date.monthValue}월 ${date.dayOfMonth}일"
                    } catch (e: Exception) {
                        dateString
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                        .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formattedDate,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                // 선택 모드 UI (기존과 동일)
                if (isSelectionMode) {
                    val isSelected = selectedIds.contains(itemId)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (isSelected) Color.Black.copy(0.3f) else Color.Transparent)
                    )
                    // 체크박스 (우측 상단)
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopEnd)
                            .size(24.dp)
                            .background(
                                color = if (isSelected) Color(0xFF222222) else Color.Transparent,
                                shape = CircleShape
                            )
                            .border(
                                1.5.dp,
                                if (isSelected) Color(0xFF222222) else Color.White,
                                CircleShape
                            ), contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// 3. 캘린더 뷰 (간단 구현)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarListView(
    viewModel: MainViewModel, onDateClick: (String) -> Unit
) {
    val calendarData = viewModel.calendarDays
    val viewDate = viewModel.currentCalendarDate

    // 핵심: viewDate(년/월)가 바뀔 때마다 서버 API 호출
    LaunchedEffect(viewDate.year, viewDate.monthValue) {
        viewModel.fetchCalendar(viewDate.year, viewDate.monthValue)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F7)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp)) {
                    // --- 캘린더 헤더 (월 변경 컨트롤) ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.updateCalendarMonth(-1) }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "이전 달")
                        }

                        Text(
                            text = "${viewDate.year}년 ${viewDate.monthValue}월",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        IconButton(onClick = { viewModel.updateCalendarMonth(1) }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "다음 달")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- 캘린더 그리드 ---
                    MonthCalendarGrid(
                        year = viewDate.year,
                        month = viewDate.monthValue,
                        calendarDays = calendarData,
                        onDayClick = onDateClick)
                }
            }
        }
    }
}

// 4. 삭제 확인 다이얼로그 (커스텀 디자인)
@Composable
fun DeleteConfirmDialog(
    count: Int, onDismiss: () -> Unit, onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 메시지 영역
                Column(
                    modifier = Modifier.padding(
                        top = 32.dp, bottom = 24.dp, start = 16.dp, end = 16.dp
                    ), horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${count}장의 사진을 삭제하시겠어요?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "상대방에게도 동일하게 삭제되고,\n사진은 영구적으로 삭제됩니다.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = Color(0xFF888888),
                        lineHeight = 20.sp
                    )
                }

                Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)

                // 삭제 버튼 (빨간색)
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onConfirm() }
                    .padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "${count}장의 사진 삭제", // 혹은 "종료하기" 처럼 고정 텍스트
                        color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold, fontSize = 16.sp
                    )
                }

                Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)

                // 취소 버튼
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDismiss() }
                    .padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
                    Text("취소", color = Color.Black, fontSize = 16.sp)
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonthCalendarGrid(
    year: Int, month: Int, calendarDays: List<CalendarDayInfo>, // 서버에서 받아온 해당 월의 데이터 리스트
    onDayClick: (String) -> Unit        // 날짜(YYYY-MM-DD)를 인자로 상세 페이지 이동
) {
    val firstDay = LocalDate.of(year, month, 1)
    val daysInMonth = firstDay.lengthOfMonth()
    val startOffset = firstDay.dayOfWeek.value % 7
    val totalSlots = startOffset + daysInMonth
    val rows = (totalSlots + 6) / 7

    Column {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                for (col in 0 until 7) {
                    val dayIndex = row * 7 + col - startOffset + 1

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.9f)
                    ) {
                        if (dayIndex in 1..daysInMonth) {
                            // 현재 날짜 계산 (YYYY-MM-DD 포맷)
                            val currentDate = LocalDate.of(year, month, dayIndex)
                            val dateString = currentDate.format(DateTimeFormatter.ISO_DATE)

                            // 해당 날짜에 데이터가 있는지 확인
                            val dayData = calendarDays.find { it.date == dateString }

                            if (dayData != null) {
                                // 썸네일 결정 (나의 사진 우선, 없으면 파트너 사진)
                                val thumbnailUrl =
                                    dayData.meImageThumbnailUrl ?: dayData.partnerImageThumbnailUrl

                                Box(
                                    modifier = Modifier
                                        .padding(2.dp)
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onDayClick(dateString) }) {
                                    AsyncImage(
                                        model = thumbnailUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // 숫자 가독성을 위한 오버레이
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.2f))
                                    )

                                    Text(
                                        text = "$dayIndex",
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            } else {
                                // 데이터가 없는 날짜
                                Text(
                                    text = "$dayIndex",
                                    fontSize = 14.sp,
                                    color = Color(0xFF888888),
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuestionListView(
    questions: List<ArchiveQuestionItem>,
    onQuestionClick: (Int, ArchiveQuestionItem) -> Unit,
    loadMore: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(questions) { index, item ->
            // 페이지네이션: 마지막 아이템 도달 시 추가 로드
            if (index >= questions.size - 1) {
                LaunchedEffect(Unit) { loadMore() }
            }

            Column(modifier = Modifier
                .fillMaxWidth()
                .clickable { onQuestionClick(index, item) }
                .padding(horizontal = 20.dp, vertical = 24.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    // 질문 번호 (서버에서 준 questionNumber 활용)
                    Text(
                        text = "#${item.questionNumber} ",
                        color = Color(0xFFFF5A5A),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.questionContent,
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp
                    )
                }
            }
            Divider(color = Color(0xFFF0F0F0), thickness = 1.dp)
        }
    }
}

// 5. 토글 버튼 업데이트 (3개 옵션)
@Composable
fun ViewModeToggle(
    currentMode: ViewMode, onModeChanged: (ViewMode) -> Unit
) {
    Row(
        modifier = Modifier
            .background(Color(0xFFF5F5F5), RoundedCornerShape(20.dp))
            .padding(4.dp)
    ) {
        ToggleOption("최신순", currentMode == ViewMode.LATEST) { onModeChanged(ViewMode.LATEST) }
        ToggleOption("캘린더", currentMode == ViewMode.CALENDAR) { onModeChanged(ViewMode.CALENDAR) }
        ToggleOption("질문", currentMode == ViewMode.QUESTION) { onModeChanged(ViewMode.QUESTION) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionDetailScreen(
    data: QuestionDetailResponse, onBack: () -> Unit, onDelete: () -> Unit
) {

    val scope = rememberCoroutineScope() // 코루틴 스코프 생성
    val graphicsLayer = rememberGraphicsLayer() // 캡처를 위한 레이어
    val context = LocalContext.current

    // 사진이 둘 다 없는 경우 체크
    val isBothEmpty =
        data.myInfo?.questionImageUrl == null && data.partnerInfo?.questionImageUrl == null

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("#${data.questionNumber}", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "삭제")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }, bottomBar = {
            QuestionDetailBottomBar(onShare = {
                scope.launch {
                    val bitmap = graphicsLayer.toImageBitmap()
//                        shareImage(context, bitmap)
                }
            }, onInstagram = {
                scope.launch {
                    val bitmap = graphicsLayer.toImageBitmap()
                    shareToInstagram(context, bitmap)
                }
            }, onDownload = {
                scope.launch {
                    val bitmap = graphicsLayer.toImageBitmap()
                    saveImageToGallery(context, bitmap)
                }
            })
        }, containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .drawWithContent {
                    // 이 레이어에 현재 화면 내용을 기록합니다.
                    graphicsLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(graphicsLayer)
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 질문 내용
            Text(
                text = data.questionContent,
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 26.sp,
                modifier = Modifier.padding(vertical = 32.dp, horizontal = 24.dp)
            )

            if (isBothEmpty) {
                // 사진이 모두 없는 경우 (명세 반영)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("삭제된 사진입니다.", color = Color.Gray)
                }
            } else {
                // 사진 영역
                Card(
                    modifier = Modifier.padding(20.dp).weight(1f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center // 답변이 하나일 때 중앙 정렬
                    ) {
                        data.myInfo?.let {
                            DetailPhotoSection(info = it, modifier = Modifier.weight(1f))
                        }
                        data.partnerInfo?.let {
                            DetailPhotoSection(info = it, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun DetailPhotoSection(info: UserAnswerInfo, modifier: Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        // 배경: 답변 이미지 (questionImageUrl)
        AsyncImage(
            model = info.questionImageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 왼쪽 상단: 프로필과 이름/시간
        Row(
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = info.profileThumbnailImageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(0.3f))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = info.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = info.answeredAt ?: "", color = Color.White.copy(0.8f), fontSize = 11.sp)
            }
        }

        // 만약 서버에서 '답변 텍스트'가 추가된다면 여기에 말풍선을 넣으세요!
    }
}

@Composable
fun PhotoSection(
    answer: QuestionAnswer, modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        // 배경 이미지
        AsyncImage(
            model = answer.imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 이름 & 시간 레이어 (좌측 상단)
        Row(
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = answer.userName,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = answer.time, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
            }
        }

        // 중앙 하단 말풍선
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = answer.comment,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun PhotoSection(
    userName: String, time: String, imageUrl: String, comment: String, modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        // 배경 이미지
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 상단 정보 (이름, 시간)
        Row(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.White.copy(alpha = 0.3f), CircleShape)
            ) // 아바타
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = userName,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = time, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
            }
        }

        // 말풍선 코멘트 (중앙 하단)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = comment,
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun AnswerItemCard(answer: QuestionAnswer) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        // 사용자 정보 (아바타, 이름, 시간)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEEEEEE)) // 프로필 이미지 없을 때 배경
            ) {
                // AsyncImage(model = answer.profileImageUrl, ...)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = answer.userName, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = answer.time, fontSize = 12.sp, color = Color.Gray)
            }
        }

        // 이미지 및 말풍선 오버레이
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f) // 스크린샷 비율에 맞춰 조정
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF0F0F0))
        ) {
            // 실제 이미지 (Coil 사용)
            AsyncImage(
                model = answer.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // ✅ 말풍선 스타일 코멘트 (중앙 하단 배치)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = answer.comment,
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun QuestionDetailBottomBar(
    onShare: () -> Unit, onInstagram: () -> Unit, onDownload: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp, top = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. 일반 공유 버튼
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(Color.Black, CircleShape)
                .clip(CircleShape) // 클릭 영역 제한
                .clickable { onShare() }, contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "공유",
                modifier = Modifier.size(24.dp),
                tint = Color.White
            )
        }

        // 2. 인스타그램 버튼
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(Color.Black, CircleShape)
                .clip(CircleShape)
                .clickable { onInstagram() }, contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_instargram),
                contentDescription = "인스타그램",
                modifier = Modifier.size(48.dp)
            )
        }

        // 3. 다운로드 버튼
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(Color.Black, CircleShape)
                .clip(CircleShape)
                .clickable { onDownload() }, contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FileDownload,
                contentDescription = "저장",
                modifier = Modifier.size(24.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
fun BottomActionButton(
    icon: ImageVector,
    backgroundColor: Long = 0xFFF0F0F0,
    iconTint: Color = Color.Unspecified // 기본값을 Unspecified로 설정
) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .background(Color(backgroundColor), CircleShape)
            .clickable { }, contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = iconTint // 전달받은 틴트 적용
        )
    }
}

// 캡처를 제어할 클래스
class CaptureController {
    var captureFunction: (() -> ImageBitmap)? = null
    fun capture() = captureFunction?.invoke()
}

// 비트맵을 갤러리에 저장하는 간단한 로직
private fun saveImageToGallery(context: Context, bitmap: ImageBitmap) {
    val androidBitmap = bitmap.asAndroidBitmap()
    val filename = "Connect_Archive_${System.currentTimeMillis()}.jpg"

    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
    }

    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    uri?.let {
        context.contentResolver.openOutputStream(it).use { out ->
            if (out != null) {
                androidBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
        }
        Toast.makeText(context, "갤러리에 저장되었습니다.", Toast.LENGTH_SHORT).show()
    }
}

private fun shareToInstagram(context: Context, bitmap: ImageBitmap) {
    val androidBitmap = bitmap.asAndroidBitmap()

    // 1. 임시 파일 저장 (FileProvider 필요)
    val file = File(context.cacheDir, "instagram_share.jpg")
    FileOutputStream(file).use { out ->
        androidBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
    }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    // 2. 인스타그램 스토리 인텐트 설정
    val intent = Intent("com.instagram.share.ADD_TO_STORY").apply {
        type = "image/jpeg"
        putExtra("interactive_asset_uri", uri)
        putExtra("top_background_color", "#000000") // 배경색 커스텀
        putExtra("bottom_background_color", "#000000")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    // 3. 인스타그램 앱 실행
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "인스타그램 앱이 설치되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ArchiveEmptyView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 80.dp), // 하단 탭바 고려
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "저장된 사진이 없어요",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(32.dp))

        Image(
            painter = painterResource(id = R.drawable.img_not_connected_yet),
            contentDescription = null,
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "연인과 사진을 공유하면\n이곳에 차곡차곡 저장돼요.",
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class) // Pager 사용을 위해 필요
@Composable
fun DetailListWrapper(
    items: List<QuestionDetailResponse>,
    initialIndex: Int,
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
    // 1. Pager 상태 관리 (현재 몇 번째 페이지인지)
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { items.size }
    )

    // 현재 보고 있는 페이지의 데이터
    val currentItem = items.getOrNull(pagerState.currentPage)

    Scaffold(
        containerColor = Color.White,
        topBar = {
            // 상단: 뒤로가기 / 날짜 / 삭제 버튼
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = currentItem?.myInfo?.answeredAt ?: "", // 데이터에 날짜가 있다면 표시
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "삭제")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            // 하단: 공유/다운로드 버튼들 (스크린샷 하단 동그라미 버튼 3개)
            // 기존 QuestionDetailBottomBar 사용하거나 직접 구현
            QuestionDetailBottomBar(
                onShare = { /* 공유 로직 */ },
                onInstagram = { /* 인스타 로직 */ },
                onDownload = { /* 다운로드 로직 */ }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 2. 질문 제목 (예: "오운완")
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = currentItem?.questionContent ?: "",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(20.dp))

            // 3. 가로 스와이프 페이저 (여기가 핵심!)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f) // 남은 공간 꽉 채우기
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 30.dp), // 양옆 살짝 보이게 (선택사항)
                pageSpacing = 16.dp // 카드 사이 간격
            ) { page ->
                // 각 페이지의 카드 내용
                val itemData = items[page]
                DetailCardItem(data = itemData)
            }

            // 4. 페이지 인디케이터 (점, 점, 점)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .height(20.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(items.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) Color.Black else Color.LightGray
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailListWrapper2(
    items: List<QuestionDetailResponse>,
    initialIndex: Int,
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
    val listState = rememberLazyListState()

    // 데이터가 로드되면 해당 위치로 스크롤
    LaunchedEffect(initialIndex) {
        if (items.isNotEmpty()) {
            listState.scrollToItem(initialIndex)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("상세 조회", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "뒤로가기")
                    }
                }
            )
        },
    ) { paddingValues ->
        // 2. 중요: Box나 Column으로 감싸고 반드시 fillMaxSize를 먼저 줍니다.
        Box(
            modifier = Modifier
                .fillMaxSize()           // 이 부분이 없으면 LazyColumn이 무한대 높이를 가지려 합니다.
                .padding(paddingValues)  // Scaffold의 상단바 영역만큼 패딩 처리
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(), // 3. 리스트도 꽉 채우기
            ) {
                items(items) { item ->
                    // 상세 카드 레이아웃 (알맹이)
                    QuestionDetailScreen(
                        data = item,
                        onBack = onBack,
                        onDelete = onDelete
                    )

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
fun DetailCardItem(data: QuestionDetailResponse) {
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        val hasMyInfo = data.myInfo != null
        val hasPartnerInfo = data.partnerInfo != null
        val singleInfo = data.myInfo ?: data.partnerInfo

        Box(modifier = Modifier.fillMaxSize()) {
            if (hasMyInfo && hasPartnerInfo) {
                // 1. 둘 다 있을 때: 기존처럼 50/50 배분
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        PhotoSection(info = data.myInfo!!, isTop = true)
                    }
                    Divider(color = Color.White, thickness = 2.dp)
                    Box(modifier = Modifier.weight(1f)) {
                        PhotoSection(info = data.partnerInfo!!, isTop = false)
                    }
                }
            } else if (singleInfo != null) {
                // 2. 하나만 있을 때: 전체 배경 블러 + 중앙 원본

                // [배경] 전체 영역에 블러 처리된 이미지 배치
                AsyncImage(
                    model = singleInfo.questionImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(30.dp) // 블러 강도 조절
                )

                // [배경 딤 처리] 블러 이미지를 살짝 어둡게 (선택사항)
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))

                // [중앙 원본] 25% ~ 75% 영역 (weight 0.5 : 1 : 0.5)
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.weight(0.5f))

                    Box(modifier = Modifier.weight(1f)) {
                        PhotoSection(
                            info = singleInfo,
                            isTop = true // 테두리 둥글게 등 기존 스타일 유지
                        )
                    }

                    Spacer(modifier = Modifier.weight(0.5f))
                }
            } else {
                EmptyPhotoPlaceholder("등록된 사진이 없습니다.")
            }
        }
    }
}

@Composable
fun PhotoSection(info: UserAnswerInfo, isTop: Boolean) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 배경 이미지
        AsyncImage(
            model = info.questionImageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 오버레이 정보 (프로필, 이름, 시간)
        Row(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 프로필 썸네일
            AsyncImage(
                model = info.profileThumbnailImageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.White, CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = info.name, color = Color.White, fontWeight = FontWeight.Bold)
                Text(text = info.answeredAt ?: "", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            }
        }

//        // 말풍선 (중앙 하단) - 예: "오빠 이때 잘생겼어!"
//        // 데이터 모델에 comment 필드가 있다면 여기에 표시
//        Box(
//            modifier = Modifier
//                .align(Alignment.BottomCenter)
//                .padding(bottom = 20.dp)
//                .background(Color.Black.copy(0.6f), RoundedCornerShape(16.dp))
//                .padding(horizontal = 16.dp, vertical = 8.dp)
//        ) {
//            Text("코멘트", color = Color.White)
//        }
    }
}

@Composable
fun EmptyPhotoPlaceholder(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEEEEEE)),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.Gray)
    }
}