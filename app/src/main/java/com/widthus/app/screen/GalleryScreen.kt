package com.widthus.app.screen

import android.net.Uri
import android.os.Build
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.widthus.app.model.MemorySet
import com.widthus.app.model.QuestionAnswer
import com.widthus.app.viewmodel.MainViewModel
import com.withus.app.R
import org.withus.app.model.CoupleQuestionData
import org.withus.app.model.UserAnswerInfo
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// --- 데이터 모델 (예시) ---
data class MemoryItem(
    val id: String, val uri: Uri, // 실제로는 Uri 사용, 테스트에선 null 가능
    val date: LocalDate
)

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
    val selectedIds = remember { mutableStateListOf<Int>() } // ID를 Int(coupleQuestionId)로 관리
    var showDeleteDialog by remember { mutableStateOf(false) }

    // ViewModel의 실제 데이터를 사용
    val displayItems = viewModel.memorySets

    if (selectedQuestionIndex != null) {
        // 상세 화면 표시
        QuestionDetailScreen(
            response = displayItems[selectedQuestionIndex!!],
            onBack = { selectedQuestionIndex = null },
            onDelete = { /* 삭제 로직 수행 */ }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Scaffold(
                topBar = {
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
                            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), contentAlignment = Alignment.Center) {
                                ViewModeToggle(currentMode = viewMode, onModeChanged = { viewMode = it })
                            }
                        }
                    }
                },
                bottomBar = {
                    if (isSelectionMode) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
                            Button(
                                onClick = { showDeleteDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                enabled = selectedIds.isNotEmpty()
                            ) {
                                Text("${selectedIds.size}장의 사진 삭제하기", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                    when (viewMode) {
                        ViewMode.LATEST -> {
                            LatestGridView(
                                items = displayItems,
                                isSelectionMode = isSelectionMode,
                                selectedIds = selectedIds,
                                onToggleSelect = { id ->
                                    if (selectedIds.contains(id)) selectedIds.remove(id)
                                    else selectedIds.add(id)
                                },
                                onLongClick = { id ->
                                    if (!isSelectionMode) {
                                        isSelectionMode = true
                                        selectedIds.add(id)
                                    }
                                },
                                onItemClick = { index -> selectedQuestionIndex = index }
                            )
                        }
                        ViewMode.CALENDAR -> {
                            CalendarListView(
                                items = displayItems,
                                onItemClick = { index -> selectedQuestionIndex = index }
                            )
                        }
                        ViewMode.QUESTION -> {
                            QuestionListView(
                                memorySets = displayItems,
                                onQuestionClick = { index -> selectedQuestionIndex = index }
                            )
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
}

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
    items: List<CoupleQuestionData>,
    isSelectionMode: Boolean,
    selectedIds: List<Int>,
    onToggleSelect: (Int) -> Unit,
    onLongClick: (Int) -> Unit,
    onItemClick: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(1.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        itemsIndexed(items) { index, item ->
            Box(
                modifier = Modifier
                    .aspectRatio(3f / 4f)
                    .combinedClickable(
                        onClick = {
                            if (isSelectionMode) onToggleSelect(item.coupleQuestionId)
                            else onItemClick(index)
                        },
                        onLongClick = { onLongClick(item.coupleQuestionId) }
                    )
            ) {
                AsyncImage(
                    model = item.myInfo.questionImageUrl, // 대표 이미지로 내 사진 표시
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // 날짜 배지
                Box(
                    modifier = Modifier.padding(8.dp).align(Alignment.TopStart)
                        .background(Color.White, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = item.date.format(DateTimeFormatter.ofPattern("M월 d일")), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                if (isSelectionMode) {
                    val isSelected = selectedIds.contains(item.coupleQuestionId)
                    Box(modifier = Modifier.fillMaxSize().background(if (isSelected) Color.Black.copy(0.3f) else Color.Transparent))

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
    items: List<CoupleQuestionData>,
    onItemClick: (Int) -> Unit
) {
    val grouped = remember(items) { items.groupBy { it.date.year to it.date.monthValue } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF7F7F7)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        grouped.forEach { (key, monthItems) ->
            item {
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp)) {
                        Text(text = "${key.first}년 ${key.second}월", modifier = Modifier.align(Alignment.CenterHorizontally), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        // 요일 헤더...
                        MonthCalendarGrid(key.first, key.second, monthItems, items, onItemClick)
                    }
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
    year: Int,
    month: Int,
    monthItems: List<CoupleQuestionData>, // 현재 달의 데이터들
    allItems: List<CoupleQuestionData>,  // 전체 리스트 (인덱스 추출용)
    onItemClick: (Int) -> Unit            // 상세 화면 이동 콜백
) {
    val firstDay = LocalDate.of(year, month, 1)
    val daysInMonth = firstDay.lengthOfMonth()

    // 일요일 시작 기준 오프셋 (일:0, 월:1 ...)
    val startOffset = firstDay.dayOfWeek.value % 7
    val totalSlots = startOffset + daysInMonth
    val rows = (totalSlots + 6) / 7

    Column {
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                for (col in 0 until 7) {
                    val dayIndex = row * 7 + col - startOffset + 1

                    Box(modifier = Modifier.weight(1f).aspectRatio(0.9f)) {
                        if (dayIndex in 1..daysInMonth) {
                            // 1. 해당 날짜에 데이터가 있는지 확인
                            val memory = monthItems.find { it.date.dayOfMonth == dayIndex }

                            if (memory != null) {
                                // ✅ 사진이 있는 날짜 디자인
                                Box(
                                    modifier = Modifier
                                        .padding(2.dp)
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFEEEEEE))
                                        .clickable {
                                            // 전체 리스트에서 해당 객체의 인덱스를 찾아 전달
                                            val globalIndex = allItems.indexOf(memory)
                                            if (globalIndex != -1) onItemClick(globalIndex)
                                        }
                                ) {
                                    // 내 사진을 대표 이미지로 사용
                                    AsyncImage(
                                        model = memory.myInfo.questionImageUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // 가독성을 위한 오버레이
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.2f))
                                    )

                                    // 날짜 숫자
                                    Text(
                                        text = "$dayIndex",
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            } else {
                                // 사진이 없는 날짜
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
    memorySets: List<CoupleQuestionData>,
    onQuestionClick: (Int) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(memorySets) { index, item ->
            Column(modifier = Modifier.fillMaxWidth().clickable { onQuestionClick(index) }.padding(horizontal = 20.dp, vertical = 24.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(text = "#${String.format("%02d", index + 1)} ", color = Color(0xFFFF5A5A), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = item.question, color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp)
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
    response: CoupleQuestionData, // API에서 받아온 data 객체
    onBack: () -> Unit, onDelete: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                // #505 처럼 ID 표시
                title = {
                    Text(
                        "#${response.coupleQuestionId}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }, navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBackIosNew, null) }
                }, actions = {
                    IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, null) }
                })
        }, bottomBar = { QuestionDetailBottomBar() }, containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. 서버에서 온 질문 (상대가 가장 사랑스러워 보였던 순간은?)
            Text(
                text = response.question,
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 26.sp,
                modifier = Modifier.padding(vertical = 32.dp, horizontal = 24.dp)
            )

            // 2. 상/하 분할 카드
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 20.dp, vertical = 24.dp,
                    )
                    .weight(1f), shape = RoundedCornerShape(24.dp)
            ) {
                Column {
                    // 상단: 내 정보 (myInfo)
                    DetailPhotoSection(info = response.myInfo, modifier = Modifier.weight(1f))

                    // 하단: 상대방 정보 (partnerInfo)
                    DetailPhotoSection(info = response.partnerInfo, modifier = Modifier.weight(1f))
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
                Text(text = info.answeredAt, color = Color.White.copy(0.8f), fontSize = 11.sp)
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
fun QuestionDetailBottomBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp, top = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(Color.Black, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.White // 전달받은 틴트 적용
            )
        }

        Box(
            modifier = Modifier
                .size(50.dp)
                .background(Color.Black, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_instargram),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(50.dp)
                .background(Color.Black, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FileDownload,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.White // 전달받은 틴트 적용
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