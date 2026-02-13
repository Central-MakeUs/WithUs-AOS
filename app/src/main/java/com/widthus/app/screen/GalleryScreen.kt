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
import com.widthus.app.model.GridItem
import com.widthus.app.model.MemorySet
import com.widthus.app.model.QuestionAnswer
import com.widthus.app.utils.Utils.shareImage
import com.widthus.app.viewmodel.MainViewModel
import com.withus.app.R
import kotlinx.coroutines.launch
import org.withus.app.debug
import org.withus.app.model.ArchiveDetailItem
import org.withus.app.model.ArchiveQuestionItem
import org.withus.app.model.ArchiveUserAnswerInfo
import org.withus.app.model.CalendarDayInfo
import org.withus.app.model.QuestionDetailResponse
import org.withus.app.model.UserAnswerInfo
import org.withus.app.model.UserArchiveInfo
import saveBitmapToGallery
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.YearMonth
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
        ViewMode.CALENDAR -> /*viewModel.calendarDays.isEmpty()*/ false
    }

    LaunchedEffect(viewMode) {
        when (viewMode) {
            ViewMode.LATEST -> {
                viewModel.fetchArchives(true)
            }

            ViewMode.QUESTION -> {
                viewModel.fetchQuestionArchives(true)
            }

            ViewMode.CALENDAR -> {
                val date = viewModel.currentCalendarDate
                viewModel.fetchCalendar(date.year, date.monthValue)
            }
        }
    }

    val detailDataList = viewModel.detailList // 리스트 전체를 가져옵니다

    debug("selectedQuestionIndex : $selectedQuestionIndex, detailDataList : ${detailDataList.joinToString()}")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        if (selectedQuestionIndex != null && detailDataList.isNotEmpty()) {
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
                },
                isQuestion = viewMode == ViewMode.QUESTION
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                Scaffold(topBar = {
                    Column(modifier = Modifier.background(Color.White)) {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "보관",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 5.dp)
                                )
                            },
                            actions = {
                                // 기존에 사용하던 actions 로직을 그대로 넣으면 됩니다.
                                if (!isSelectionMode) {
                                    if (viewMode == ViewMode.LATEST) {
                                        IconButton(onClick = { isSelectionMode = true }) {
                                            Icon(
                                                imageVector = Icons.Default.MoreHoriz,
                                                contentDescription = "더보기",
                                                tint = Color.Black
                                            )
                                        }
                                    }

                                } else {
                                    TextButton(onClick = {
                                        isSelectionMode = false
                                        selectedIds.clear()
                                    }) {
                                        Text("취소", color = Color.Black)
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.White
                            )
                        )

                        if (!isSelectionMode) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                ViewModeToggle(
                                    currentMode = viewMode, onModeChanged = { viewMode = it })
                            }
                        }
                    }
                }, bottomBar = {
                    if (isSelectionMode) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                                .navigationBarsPadding() // 네비게이션 바(제스처 바) 영역 확보
                        ) {
                            // 1. 삭제하기 버튼
                            val isAnySelected = selectedIds.isNotEmpty()

                            Button(
                                onClick = { showDeleteDialog = true },
                                enabled = isAnySelected, // 선택된 게 없으면 비활성화
                                colors = ButtonDefaults.buttonColors(
                                    // 선택되면 검은색, 아니면 연한 회색
                                    containerColor = if (isAnySelected) Color.Black else Color(0xFFF3F3F3),
                                    contentColor = if (isAnySelected) Color.White else Color(0xFFBDBDBD),
                                    disabledContainerColor = Color(0xFFF3F3F3),
                                    disabledContentColor = Color(0xFFBDBDBD)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            ) {
                                Text(
                                    text = if (isAnySelected) "${selectedIds.size}장의 사진 삭제하기" else "삭제하기",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (isAnySelected) {
                                // 2. 돌아가기 버튼 (텍스트 형태)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp) // 터치 영역 확보
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            isSelectionMode = false
                                            selectedIds.clear()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "돌아가기",
                                        color = Color.Black,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
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
                                            items = viewModel.archiveItems,
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
                                                    targetId = info.id,          // UserAnswerInfo에 id 필드가 있어야 함
                                                    targetType = info.archiveType
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
            }
        }
        if (showDeleteDialog) {
            DeleteConfirmDialog(
                count = if (selectedQuestionIndex != null) 1 else selectedIds.size,
                onDismiss = { showDeleteDialog = false },
                onConfirm = {
                    // 삭제 로직 호출 (viewModel.delete...)
                    showDeleteDialog = false

                    if (selectedQuestionIndex != null) {
                        // 상세 화면에서 삭제한 경우
                        selectedQuestionIndex = null
                    } else {
                        // 리스트에서 삭제한 경우
                        isSelectionMode = false
                        selectedIds.clear()
                    }
                }
            )
        }
    }
}

// ====================================================================
// 🧩 하위 컴포넌트들
// ====================================================================

@Composable
fun ToggleOption(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier, // modifier를 받을 수 있게 추가
    onClick: () -> Unit
) {
    Box(
        modifier = modifier // 여기서 weight 등의 modifier를 적용받음
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 8.dp), // 상하 여백
        contentAlignment = Alignment.Center // ★ 텍스트 중앙 정렬 필수
    ) {
        Text(
            text = text,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.Black else Color.Gray,
            fontSize = 14.sp
        )
    }
}

// 2. 최신순 그리드 뷰 (날짜 배지 + 선택 체크박스)
@OptIn(ExperimentalFoundationApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LatestGridView(
    items: List<Pair<String, ArchiveUserAnswerInfo>>,
    isSelectionMode: Boolean,
    selectedIds: List<Long>,
    onToggleSelect: (Long) -> Unit,
    onLongClick: (Long) -> Unit,
    onItemClick: (Int) -> Unit,
    loadMore: () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentPadding = PaddingValues(1.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        itemsIndexed(items) { index, item ->
            // 페이지네이션 호출
            if (index >= items.size - 1) {
                LaunchedEffect(Unit) { loadMore() }
            }

            val dateString = item.first     // "2026-02-10"
            val info = item.second          // ArchiveUserAnswerInfo 객체
            val itemId = info.id            // 식별자

            Box(
                modifier = Modifier
                    .aspectRatio(3f / 4f)
                    .background(Color.White)
                    .combinedClickable(
                        onClick = {
                            if (isSelectionMode) onToggleSelect(itemId)
                            else onItemClick(index)
                        },
                        onLongClick = { onLongClick(itemId) }
                    )
            ) {
                // --- [핵심] 이미지 위아래 배치 ---
                Column(modifier = Modifier.fillMaxSize()) {
                    // 1. 내 이미지 (위)₩
                    if (!info.myImageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = info.myImageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .weight(1f)         // [핵심] 5:5 비율을 위해 동일한 가중치 부여
                                .fillMaxWidth()     // 가로 꽉 채우기
                                .fillMaxHeight(),   // 할당된 weight 안에서 세로 꽉 채우기
                            contentScale = ContentScale.Crop // [핵심] 비율이 달라도 잘라서 꽉 채움
                        )
                    }

                    // 2. 파트너 이미지 (아래)
                    if (!info.partnerImageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = info.partnerImageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .weight(1f)         // [핵심] 위 이미지와 동일한 1f 가중치
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            contentScale = ContentScale.Crop // [핵심] 동일하게 크롭 적용
                        )
                    }
                }

                // 날짜 배지 (예: 2월 10일)
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
                        .padding(6.dp)
                        .align(Alignment.TopStart)
                        .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(text = formattedDate, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                // 선택 모드 오버레이 및 체크박스
                if (isSelectionMode) {
                    val isSelected = selectedIds.contains(itemId)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (isSelected) Color.Black.copy(0.3f) else Color.Transparent)
                    )
                    // 체크박스 UI (우측 상단)
                    Box(
                        modifier = Modifier
                            .padding(6.dp)
                            .align(Alignment.TopEnd)
                            .size(20.dp)
                            .background(
                                color = if (isSelected) Color(0xFF222222) else Color.Transparent,
                                shape = CircleShape
                            )
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFF222222) else Color.White,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarListView(
    viewModel: MainViewModel,
    onDateClick: (String) -> Unit
) {
    val months = viewModel.displayedMonths
    val calendarDataMap = viewModel.calendarDataMap

    // 전체 화면 배경색
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F7)),
        contentPadding = PaddingValues(bottom = 20.dp), // 하단 여백
        verticalArrangement = Arrangement.spacedBy(24.dp) // 카드 간 간격
    ) {
        // 리스트 아이템: 각 "달(Month)"을 그립니다.
        items(months) { yearMonth ->

            // 핵심: 아이템이 화면에 그려질 때(데이터가 없으면) API 호출
            LaunchedEffect(yearMonth) {
                viewModel.fetchCalendar(yearMonth)
            }

            // 해당 달의 데이터 가져오기 (없으면 null)
            val daysData = calendarDataMap[yearMonth] ?: emptyList()

            // 월별 카드 UI
            CalendarMonthCard(
                yearMonth = yearMonth,
                calendarDays = daysData,
                onDayClick = onDateClick
            )
        }

        item {
            LaunchedEffect(Unit) {
                viewModel.loadMorePastMonths()
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
                Box(
                    modifier = Modifier
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
                Box(
                    modifier = Modifier
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
                                debug("thumbnailUrl : $thumbnailUrl")

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

            Column(
                modifier = Modifier
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

@Composable
fun ViewModeToggle(
    currentMode: ViewMode,
    onModeChanged: (ViewMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth() // 1. 전체 너비 차지
            .background(Color(0xFFF5F5F5), RoundedCornerShape(20.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween // 아이템 간 간격 균등 배치
    ) {
        // 2. 각 아이템에 weight(1f) 적용 -> 남은 공간을 1:1:1로 나눔
        ToggleOption(
            text = "최신순",
            selected = currentMode == ViewMode.LATEST,
            onClick = { onModeChanged(ViewMode.LATEST) },
            modifier = Modifier.weight(1f)
        )
        ToggleOption(
            text = "캘린더",
            selected = currentMode == ViewMode.CALENDAR,
            onClick = { onModeChanged(ViewMode.CALENDAR) },
            modifier = Modifier.weight(1f)
        )
        ToggleOption(
            text = "질문",
            selected = currentMode == ViewMode.QUESTION,
            onClick = { onModeChanged(ViewMode.QUESTION) },
            modifier = Modifier.weight(1f)
        )
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

fun shareBitmap(context: Context, bitmap: Bitmap) {
    val uri = saveBitmapToCache(context, bitmap) ?: return

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "공유하기"))
}

private fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri? {
    return try {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val stream = FileOutputStream("$cachePath/share_image.png")
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val imagePath = File(context.cacheDir, "images")
        val newFile = File(imagePath, "share_image.png")

        // AndroidManifest.xml에 FileProvider 설정이 필요합니다.
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", newFile)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


fun shareToInstagram(context: Context, bitmap: Bitmap) {
    val uri = saveBitmapToCache(context, bitmap) ?: return

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        setPackage("com.instagram.android") // 인스타그램 패키지 지정
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // 인스타그램이 설치되어 있지 않은 경우 처리
        Toast.makeText(context, "인스타그램이 설치되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ArchiveEmptyView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 80.dp)
            .background(Color(0xFFF8F8F8)),
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DetailListWrapper(
    items: List<QuestionDetailResponse>,
    initialIndex: Int,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    isQuestion: Boolean
) {

    debug("DetailListWrapper : items :${items.joinToString()}")
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 1. Pager 상태
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { items.size }
    )

    // 2. 화면 캡처를 위한 GraphicsLayer 생성
    // Compose 1.6+ 기능. 만약 낮은 버전이라면 dev.shreyaspatil.capturable 라이브러리 사용 권장
    val graphicsLayer = rememberGraphicsLayer()

    val currentItem = items.getOrNull(pagerState.currentPage)

    Scaffold(
        containerColor = Color.White,
        topBar = {
            // 상단: 뒤로가기 / 날짜 / 삭제 버튼
            CenterAlignedTopAppBar(
                title = {
                    val title = if (isQuestion) {
                        "#${currentItem?.questionNumber?.toString()}"
                    } else {
                        currentItem?.myInfo?.answeredAt ?: ""
                    }

                    Text(
                        text = title,
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
                        Icon(
                            painter = painterResource(id = R.drawable.ic_trash),
                            contentDescription = "",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },

        bottomBar = {
            QuestionDetailBottomBar(
                onShare = {
                    scope.launch {
                        // 비트맵 캡처 및 변환
                        val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                        shareBitmap(context, bitmap)
                    }
                },
                onInstagram = {
                    scope.launch {
                        val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                        shareToInstagram(context, bitmap)
                    }
                },
                onDownload = {
                    scope.launch {
                        val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                        saveBitmapToGallery(context, bitmap)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = currentItem?.questionContent ?: "",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(20.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 30.dp),
                pageSpacing = 16.dp
            ) { page ->
                val itemData = items[page]

                // ★ 핵심: 현재 보고 있는 페이지만 GraphicsLayer에 기록하도록 설정
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            // 현재 페이지인 경우에만 drawWithContent로 캡처 레이어에 기록
                            if (pagerState.currentPage == page) {
                                Modifier.drawWithContent {
                                    // 1. GraphicsLayer에 그리기 명령 기록
                                    graphicsLayer.record {
                                        this@drawWithContent.drawContent()
                                    }
                                    // 2. 실제 화면에 그리기
                                    drawContent()
                                }
                            } else {
                                Modifier
                            }
                        )
                ) {
                    DetailCardItem(data = itemData)
                }
            }
            // 4. 페이지 인디케이터 (점, 점, 점)
            Spacer(modifier = Modifier.height(16.dp))
            if (items.size > 1) {
                Row(
                    modifier = Modifier
                        .height(20.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(items.size) { iteration ->
                        val color =
                            if (pagerState.currentPage == iteration) Color.Black else Color.LightGray
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
}

@Composable
fun DetailCardItem(data: QuestionDetailResponse) {
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        val hasMyInfo = data.myInfo?.answerImageUrl != null
        val hasPartnerInfo = data.partnerInfo?.answerImageUrl != null
        val singleInfo = data.myInfo ?: data.partnerInfo

        debug("hasMyInfo : $hasMyInfo, hasPartnerInfo : $hasPartnerInfo, singleInfo : $singleInfo")
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
                    model = singleInfo.answerImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(30.dp) // 블러 강도 조절
                )

                // [배경 딤 처리] 블러 이미지를 살짝 어둡게 (선택사항)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f))
                )

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
fun PhotoSection(info: UserArchiveInfo, isTop: Boolean) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 배경 이미지
        AsyncImage(
            model = info.answerImageUrl,
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
                Text(
                    text = info.answeredAt ?: "",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
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


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarMonthCard(
    yearMonth: YearMonth,
    calendarDays: List<CalendarDayInfo>,
    onDayClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // 2. 카드 영역
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // 스크린샷은 그림자가 거의 없어 보임
        ) {
            Column(modifier = Modifier.padding(vertical = 20.dp, horizontal = 16.dp)) {

                Text(
                    text = "${yearMonth.year}년 ${yearMonth.monthValue}월",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black, // 혹은 디자인에 맞는 색상
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    textAlign = TextAlign.Center
                )

                // 요일 헤더 (일 월 화 수 목 금 토)
                DayOfWeekHeader()

                Spacer(modifier = Modifier.height(10.dp))

                // 날짜 그리드
                MonthCalendarGrid(
                    year = yearMonth.year,
                    month = yearMonth.monthValue,
                    calendarDays = calendarDays,
                    onDayClick = onDayClick
                )
            }
        }
    }
}

@Composable
fun DayOfWeekHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        val days = listOf("일", "월", "화", "수", "목", "금", "토")
        days.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}
