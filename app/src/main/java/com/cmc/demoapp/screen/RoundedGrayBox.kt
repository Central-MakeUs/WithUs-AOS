import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cmc.demoapp.model.CalendarDay
import com.cmc.demoapp.model.MemoryItem
import com.cmc.demoapp.model.ScheduleItem
import com.cmc.demoapp.viewmodel.MainViewModel
import java.util.Calendar
import kotlin.collections.getOrNull

// ==========================================
// 2. Navigation
// ==========================================
@Composable
fun AppNavigation(viewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()

    // 뷰모델에서 데이터 가져오기
    val schedules = viewModel.dummySchedules
    val memories = viewModel.dummyMemories

    NavHost(navController = navController, startDestination = "nickname") {

        composable("nickname") {
            NicknameScreen(
                viewModel = viewModel,
                onNextClick = { if (viewModel.nickname.isNotEmpty()) navController.navigate("home") }
            )
        }

        composable("home") {
            HomeScreen(
                nickname = viewModel.nickname,
                schedules = schedules, // 데이터 전달
                memories = memories,   // 데이터 전달
                onNavigateToCalendar = { navController.navigate("calendar") }
            )
        }

        composable("calendar") {
            CalendarHomeScreen(
                nickname = viewModel.nickname,
                memories = memories,   // 캘린더 화면에도 추억 사진 전달
                onNavigateToDayUs = { navController.navigate("dayUs") }
            )
        }

        composable("dayUs") {
            DayUsScreen()
        }
    }
}

@Composable
fun HomeScreen(
    nickname: String,
    schedules: List<ScheduleItem>,
    memories: List<MemoryItem>,
    onNavigateToCalendar: () -> Unit
) {
    Scaffold(
        topBar = { TopTitleBar("LOGO") },
        bottomBar = {
            Column {
                Button(
                    onClick = onNavigateToCalendar,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                ) { Text("다음 화면 (캘린더) 보기") }
                AppBottomNavigation()
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues).padding(horizontal = 20.dp).verticalScroll(rememberScrollState())
        ) {
            // D-day 섹션 (기존 유지)
            RoundedGrayBox(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(modifier = Modifier.size(50.dp).background(Color(0xFFD6D6D6), CircleShape))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(nickname.ifEmpty { "유저" }, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("D+300", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("상대방", fontSize = 14.sp)
                    }
                    Box(modifier = Modifier.size(50.dp).background(Color(0xFFD6D6D6), CircleShape))
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 실제 데이터를 받는 일정 리스트
            ScheduleListSection(schedules = schedules)

            Spacer(modifier = Modifier.height(30.dp))

            // 실제 데이터를 받는 추억 그리드
            MemoryGridSection(memories = memories)

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun CalendarHomeScreen(
    nickname: String,
    memories: List<MemoryItem>,
    onNavigateToDayUs: () -> Unit
) {
    val today = remember { Calendar.getInstance().get(Calendar.DAY_OF_MONTH) }
    var selectedDate by remember { mutableStateOf(today) }

    Scaffold(
        topBar = { TopTitleBar("LOGO") },
        bottomBar = {
            Column {
                Button(
                    onClick = onNavigateToDayUs,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) { Text("다음 화면 (DAYUS) 보기") }
                AppBottomNavigation()
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 상단 D-Day 카드
            RoundedGrayBox(modifier = Modifier.fillMaxWidth().height(160.dp), color = Color(0xFFF2F2F2)) {
                Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
                    Text("${nickname}님과 함께한지", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("300일", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 주간 캘린더 영역
            WeeklyCalendar(
                selectedDate = selectedDate,
                onDateSelected = { newDate -> selectedDate = newDate }
            )

            // 일정 안내 문구
            RoundedGrayBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                color = Color(0xFFF2F2F2)
            ) {
                Text(
                    "일정을 공유해 보세요",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 추억 그리드
            MemoryGridSection(memories = memories)
        }
    }
}

// ==========================================
// NicknameScreen
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NicknameScreen(
    viewModel: MainViewModel,
    onNextClick: () -> Unit
) {
    Scaffold(containerColor = Color.White) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(100.dp))
            Text(
                text = "닉네임을 입력해 주세요",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "상대방에게 주로 불리는 애칭을 입력해도 좋아요",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(40.dp))

            // 입력 필드
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFFFF5252), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("1", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))

                // 실제 입력 가능한 TextField로 변경
                TextField(
                    value = viewModel.nickname,
                    onValueChange = { viewModel.updateNickname(it) },
                    placeholder = { Text("닉네임 입력") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Black,
                        unfocusedIndicatorColor = Color.LightGray
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 다음 버튼
            Button(
                onClick = onNextClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                // 닉네임 입력 여부에 따라 색상 변경
                colors = ButtonDefaults.buttonColors(
                    containerColor = if(viewModel.nickname.isNotEmpty()) Color.Black else Color(0xFFC4C4C4)
                )
            ) {
                Text("다음 (홈으로)", fontSize = 18.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// --- 홈 화면 ---
@Composable
fun HomeScreen(
    nickname: String,
    onNavigateToCalendar: () -> Unit
) {
    Scaffold(
        topBar = { TopTitleBar("LOGO") },
        bottomBar = {
            // 데모용: 네비게이션 바 대신 다음 화면 버튼 포함
            Column {
                Button(
                    onClick = onNavigateToCalendar,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                ) {
                    Text("다음 화면 (캘린더) 보기")
                }
                AppBottomNavigation()
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues).padding(horizontal = 20.dp).verticalScroll(rememberScrollState())
        ) {
            // D-day 카드
            RoundedGrayBox(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(modifier = Modifier.size(50.dp).background(Color(0xFFD6D6D6), CircleShape))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(nickname.ifEmpty { "유저" }, fontSize = 14.sp) // 닉네임 표시
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("D+300", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("상대방", fontSize = 14.sp)
                    }
                    Box(modifier = Modifier.size(50.dp).background(Color(0xFFD6D6D6), CircleShape))
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
            Text("오늘 일정", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            RoundedGrayBox(modifier = Modifier.fillMaxWidth().height(100.dp))
            Spacer(modifier = Modifier.height(30.dp))
            MemoryGridSection()
        }
    }
}

// --- 캘린더 홈 화면 ---
@Composable
fun CalendarHomeScreen(
    nickname: String,
    onNavigateToDayUs: () -> Unit
) {
    Scaffold(
        topBar = { TopTitleBar("LOGO") },
        bottomBar = {
            Column {
                Button(
                    onClick = onNavigateToDayUs,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text("다음 화면 (DAYUS) 보기")
                }
                AppBottomNavigation()
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues).padding(horizontal = 20.dp).verticalScroll(rememberScrollState())
        ) {
            RoundedGrayBox(modifier = Modifier.fillMaxWidth().height(160.dp), color = Color(0xFFF2F2F2)) {
                Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
                    Text("${nickname}님과 함께한지", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("300일", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            // (캘린더 UI 생략 - 이전 코드와 동일)
            Text("주간 캘린더 영역 (생략)", color = Color.Gray)
            Spacer(modifier = Modifier.height(20.dp))
            MemoryGridSection()
        }
    }
}

// --- DAYUS (새로 요청하신 화면) ---
@Composable
fun DayUsScreen() {
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "DAYUS", fontSize = 20.sp, fontWeight = FontWeight.Black)
                Icon(imageVector = Icons.Default.DateRange, contentDescription = "Calendar")
            }
        },
        bottomBar = {
            // 커스텀 하단 바 (FAB가 중앙에 있는 형태)
            BottomAppBar(
                containerColor = Color.White,
                tonalElevation = 10.dp,
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Outlined.GridView, contentDescription = "Menu") }
                    Spacer(modifier = Modifier.weight(1f)) // 중앙 공간 확보
                    IconButton(onClick = {}) { Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.LightGray) }
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {},
                        containerColor = Color(0xFF1C1C1E), // 검은색에 가까운 다크그레이
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(64.dp).offset(y = (-10).dp) // 살짝 위로 올림
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(32.dp))
                    }
                }
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // 1. 프로필 영역
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ProfileCircleItem(text = "나", isActive = true)
                ProfileCircleItem(text = "이미지", isActive = false)
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 2. 타이틀
            Text("오늘", fontSize = 24.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(16.dp))

            // 3. 잠겨있는 카드 영역
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp) // 적절한 높이 설정
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF555555), Color(0xFF333333))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // 잠금 아이콘 + 원 (겹친 느낌 단순화)
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "상대방이 오늘의 하루를 보냈지만\n아직 확인할 수 없어요",
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 4. 하단 안내 텍스트 및 아이콘
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 플러스 아이콘 + 원
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 아이콘 겹침 효과 흉내
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(color = Color.Black, radius = size.minDimension / 2.2f)
                        drawCircle(color = Color.White, radius = size.minDimension / 2.2f, center = center.copy(x = center.x + 20f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
                    }
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "당신의 하루를 상대방에게 보내고\n상대방의 하루를 확인해보세요",
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// DayUsScreen용 보조 컴포넌트
@Composable
fun ProfileCircleItem(text: String, isActive: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (isActive) Color(0xFFE0E0E0) else Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Text(text, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text, fontSize = 12.sp, color = if(isActive) Color.Black else Color.Gray)
    }
}


// 일정 리스트 컴포넌트 (리스트 형태)
@RequiresApi(Build.VERSION_CODES.N)
@Composable
fun ScheduleListSection(schedules: List<ScheduleItem>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "오늘 일정",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (schedules.isEmpty()) {
            // 일정이 없을 때 빈 박스
            RoundedGrayBox(modifier = Modifier.fillMaxWidth().height(100.dp))
        } else {
            // 일정이 있을 때 리스트 출력
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                schedules.forEach { item ->
                    RoundedGrayBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .height(60.dp),
                        color = Color(0xFFF9F9F9)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(item.title, fontSize = 16.sp, color = Color.Black)
                            Text(item.time, fontSize = 14.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

// 추억 그리드 컴포넌트 (이미지 리소스 받기)
@Composable
fun MemoryGridSection(memories: List<MemoryItem>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "오늘 추억",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().height(260.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 첫 번째 사진 (왼쪽 큰 거)
            val firstImg = memories.getOrNull(0)?.imageResId
            PhotoBox(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                imageResId = firstImg
            )

            // 오른쪽 컬럼
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 두 번째 사진 (우측 상단)
                val secondImg = memories.getOrNull(1)?.imageResId
                PhotoBox(modifier = Modifier.weight(1f).fillMaxWidth(), imageResId = secondImg)

                // 세 번째 사진 (우측 하단)
                val thirdImg = memories.getOrNull(2)?.imageResId
                PhotoBox(modifier = Modifier.weight(1f).fillMaxWidth(), imageResId = thirdImg)
            }
        }
    }
}

// 이미지를 실제로 그려주는 박스 (이미지가 없으면 회색 박스)
@Composable
fun PhotoBox(modifier: Modifier, @DrawableRes imageResId: Int?) {
    if (imageResId != null) {
        // 이미지가 있을 경우
        Box(modifier = modifier.clip(RoundedCornerShape(16.dp))) {
            // 실제 이미지를 꽉 채워서 보여줌
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = imageResId),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop, // 이미지를 꽉 차게 자름
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        // 이미지가 없을 경우 기본 회색 박스
        RoundedGrayBox(modifier = modifier)
    }
}

@Composable
fun WeeklyCalendar(
    selectedDate: Int, // 현재 선택된 날짜 (Day of Month)
    onDateSelected: (Int) -> Unit
) {
    // 1. 현재 날짜 기준으로 이번 주의 데이터 생성
    val calendarDays = remember {
        val cal = Calendar.getInstance()

        // 이번 주의 일요일로 설정
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)

        val dayNames = listOf("일", "월", "화", "수", "목", "금", "토")
        List(7) { index ->
            val date = cal.get(Calendar.DAY_OF_MONTH)
            val dayName = dayNames[index]

            val dayModel = CalendarDay(date, dayName)
            cal.add(Calendar.DAY_OF_MONTH, 1) // 다음 날로 이동
            dayModel
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        calendarDays.forEach { day ->
            val isCurrentSelected = day.date == selectedDate

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp)) // 클릭 영역 제한
                    .clickable { onDateSelected(day.date) }
                    .padding(4.dp)
            ) {
                // 날짜 원형 배경
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCurrentSelected) Color.Black else Color(0xFFEEEEEE)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.date.toString(),
                        color = if (isCurrentSelected) Color.White else Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 요일 텍스트
                Text(
                    text = day.dayOfWeek,
                    fontSize = 12.sp,
                    color = if (isCurrentSelected) Color.Black else Color.Gray,
                    fontWeight = if (isCurrentSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// ==========================================
// 4. 공통 컴포넌트 (이전 코드 재사용)
// ==========================================
@Composable
fun TopTitleBar(title: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
fun RoundedGrayBox(modifier: Modifier = Modifier, color: Color = Color(0xFFF2F2F2), content: @Composable BoxScope.() -> Unit = {}) {
    Box(modifier = modifier.clip(RoundedCornerShape(16.dp)).background(color), content = content)
}

@Composable
fun AppBottomNavigation() {
    NavigationBar(containerColor = Color(0xFFF2F2F2)) {
        listOf(Icons.Outlined.Home, Icons.Default.DateRange, Icons.Default.Person).forEach {
            NavigationBarItem(selected = false, onClick = {}, icon = { Icon(it, contentDescription = null) })
        }
    }
}

@Composable
fun MemoryGridSection() {
    Text("오늘 추억", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
    Row(modifier = Modifier.fillMaxWidth().height(200.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RoundedGrayBox(modifier = Modifier.weight(1f).fillMaxHeight())
        Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RoundedGrayBox(modifier = Modifier.weight(1f).fillMaxWidth())
            RoundedGrayBox(modifier = Modifier.weight(1f).fillMaxWidth())
        }
    }
}

// 프리뷰
@androidx.compose.ui.tooling.preview.Preview
@Composable
fun PreviewApp() {
    AppNavigation()
}