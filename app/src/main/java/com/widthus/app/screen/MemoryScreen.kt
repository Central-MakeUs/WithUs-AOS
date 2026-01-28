package com.widthus.app.screen

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// --- 데이터 모델 (예시) ---
data class MemoryItem(
    val id: String,
    val uri: Uri, // 실제로는 Uri 사용, 테스트에선 null 가능
    val date: LocalDate
)

enum class ViewMode { CALENDAR, LATEST }

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen() {
    // === 상태 관리 ===
    var viewMode by remember { mutableStateOf(ViewMode.LATEST) }
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<String>() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // --- 더미 데이터 생성 (테스트용) ---
    val dummyMemories = remember {
        val today = LocalDate.now()
        (0..20).map { i ->
            MemoryItem(
                id = i.toString(),
                uri = Uri.EMPTY, // 실제 이미지 URI로 교체 필요
                date = today.minusDays(i.toLong() * 2)
            )
        }
    }

    // 현재 화면에 표시할 데이터
    val displayItems = dummyMemories // 실제로는 ViewModel 등에서 관리

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Scaffold(
            topBar = {
                Column(modifier = Modifier.background(Color.White)) {
                    // 1. 상단 타이틀 바
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                "추억",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        actions = {
                            if (!isSelectionMode) {
                                IconButton(onClick = { /* 더보기 액션 (선택모드 진입 등) */
                                    isSelectionMode = true
                                }) {
                                    Icon(Icons.Default.MoreHoriz, contentDescription = "더보기", tint = Color.Black)
                                }
                            } else {
                                // 선택 모드일 때 취소 버튼
                                TextButton(onClick = {
                                    isSelectionMode = false
                                    selectedIds.clear()
                                }) {
                                    Text("취소", color = Color.Black)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
                    )

                    // 2. 뷰 모드 토글 (캘린더 / 최신순)
                    // 선택 모드가 아닐 때만 표시하거나, 항상 표시하거나 디자인에 따라 결정
                    if (!isSelectionMode) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ViewModeToggle(
                                currentMode = viewMode,
                                onModeChanged = { viewMode = it }
                            )
                        }
                    }
                }
            },
            bottomBar = {
                // 삭제 버튼 (선택 모드일 때만 표시)
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .navigationBarsPadding() // 하단 네비게이션 바 고려
                    ) {
                        Button(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = selectedIds.isNotEmpty()
                        ) {
                            Text(
                                "${selectedIds.size}장의 사진 삭제하기",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            },
            containerColor = Color.White
        ) { paddingValues ->
            // === 메인 컨텐츠 ===
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                if (viewMode == ViewMode.LATEST) {
                    // [최신순 뷰] 3열 그리드
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
                        }
                    )
                } else {
                    // [캘린더 뷰] 월별 리스트
                    CalendarListView(
                        items = displayItems
                    )
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
                }
            )
        }
    }
}

// ====================================================================
// 🧩 하위 컴포넌트들
// ====================================================================

// 1. 토글 버튼 (캘린더 | 최신순)
@Composable
fun ViewModeToggle(
    currentMode: ViewMode,
    onModeChanged: (ViewMode) -> Unit
) {
    Row(
        modifier = Modifier
            .background(Color(0xFFF5F5F5), RoundedCornerShape(20.dp))
            .padding(4.dp)
    ) {
        // 캘린더 버튼
        ToggleOption(
            text = "캘린더",
            isSelected = currentMode == ViewMode.CALENDAR,
            onClick = { onModeChanged(ViewMode.CALENDAR) }
        )
        // 최신순 버튼
        ToggleOption(
            text = "최신순",
            isSelected = currentMode == ViewMode.LATEST,
            onClick = { onModeChanged(ViewMode.LATEST) }
        )
    }
}

@Composable
fun ToggleOption(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color(0xFF222222) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
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
    items: List<MemoryItem>,
    isSelectionMode: Boolean,
    selectedIds: List<String>,
    onToggleSelect: (String) -> Unit,
    onLongClick: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(1.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        items(items) { item ->
            Box(
                modifier = Modifier
                    .aspectRatio(3f / 4f) // 네컷 비율
                    .background(Color.LightGray) // 로딩 전 배경
                    .combinedClickable(
                        onClick = {
                            if (isSelectionMode) onToggleSelect(item.id)
                        },
                        onLongClick = { onLongClick(item.id) }
                    )
            ) {
                // 이미지 (Coil 등 사용)
                // AsyncImage(...)
                // 더미 이미지 박스
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFFDDDDDD)))

                // 날짜 배지 (좌측 상단)
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                        .background(Color.White, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    val dateFormatter = DateTimeFormatter.ofPattern("M월 d일")
                    Text(
                        text = item.date.format(dateFormatter),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                // 선택 모드 오버레이
                if (isSelectionMode) {
                    val isSelected = selectedIds.contains(item.id)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (isSelected) Color.Black.copy(alpha = 0.3f) else Color.Transparent)
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
                            .border(1.5.dp, if(isSelected) Color(0xFF222222) else Color.White, CircleShape),
                        contentAlignment = Alignment.Center
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
fun CalendarListView(items: List<MemoryItem>) {
    // 월별로 데이터 그룹화
    val grouped = remember(items) {
        items.groupBy { it.date.year to it.date.monthValue }
    }

    // 배경을 연한 회색으로 설정하여 흰색 카드가 잘 보이게 함
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F7)), // 배경색 변경
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp) // 카드 간 간격
    ) {
        grouped.forEach { (key, monthItems) ->
            val (year, month) = key
            item {
                // ✅ [수정] 월별 데이터를 담는 둥근 카드
                Card(
                    shape = RoundedCornerShape(24.dp), // 모서리 둥글게 (스크린샷과 유사하게)
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // 그림자 없이 깔끔하게 (필요시 추가)
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp)
                    ) {
                        // 1. 년/월 타이틀
                        Text(
                            text = "${year}년 ${month}월",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // 2. 요일 헤더 (일 ~ 토)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf("일", "월", "화", "수", "목", "금", "토").forEach { day ->
                                Text(
                                    text = day,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF666666) // 약간 연한 회색 글씨
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3. 날짜 그리드 (기존 함수 재사용)
                        MonthCalendarGrid(year, month, monthItems)
                    }
                }
            }
        }
    }
}

// 4. 삭제 확인 다이얼로그 (커스텀 디자인)
@Composable
fun DeleteConfirmDialog(
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
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
                    modifier = Modifier.padding(top = 32.dp, bottom = 24.dp, start = 16.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
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
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${count}장의 사진 삭제", // 혹은 "종료하기" 처럼 고정 텍스트
                        color = Color(0xFFFF3B30),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)

                // 취소 버튼
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDismiss() }
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("취소", color = Color.Black, fontSize = 16.sp)
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonthCalendarGrid(year: Int, month: Int, items: List<MemoryItem>) {
    val firstDay = LocalDate.of(year, month, 1)
    val daysInMonth = firstDay.lengthOfMonth()

    // DayOfWeek 값: 월(1)~일(7).
    // 일요일 시작 달력을 원하면: (dayOfWeek.value % 7) -> 일(0), 월(1)...
    val startOffset = firstDay.dayOfWeek.value % 7

    val totalSlots = startOffset + daysInMonth
    val rows = (totalSlots + 6) / 7

    Column {
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                for (col in 0 until 7) {
                    val dayIndex = row * 7 + col - startOffset + 1

                    // 날짜 셀의 비율 (가로세로 비율 조정, 1f = 정사각형)
                    Box(modifier = Modifier.weight(1f).aspectRatio(0.9f)) {
                        if (dayIndex in 1..daysInMonth) {
                            val currentDate = LocalDate.of(year, month, dayIndex)
                            // 해당 날짜에 사진이 있는지 확인 (첫 번째 사진만)
                            val memory = items.find { it.date == currentDate }

                            if (memory != null) {
                                // ✅ [수정] 사진이 있는 날짜 디자인
                                Box(
                                    modifier = Modifier
                                        .padding(2.dp) // 셀 간 간격
                                        .fillMaxSize()
                                        // 1. 둥근 모서리 적용 (12dp 정도가 스크린샷과 비슷합니다)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFEEEEEE))
                                ) {
                                    // 2. 실제 이미지 로드 (Coil)
                                    AsyncImage(
                                        model = memory.uri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop, // 이미지를 꽉 채움
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // 3. 텍스트 가독성을 위한 검은 반투명 레이어 (선택 사항)
                                    // 사진이 너무 밝으면 숫자가 안 보일 수 있어서 20% 정도 어둡게 깝니다.
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.2f))
                                    )

                                    // 4. 날짜 텍스트 (흰색, 중앙 정렬)
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
                                    color = Color.Gray, // 혹은 Color(0xFF888888)
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