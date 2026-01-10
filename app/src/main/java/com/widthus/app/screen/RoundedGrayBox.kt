import android.Manifest.permission.CAMERA
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.kakao.sdk.share.ShareClient
import com.kakao.sdk.template.model.Button
import com.kakao.sdk.template.model.Content
import com.kakao.sdk.template.model.FeedTemplate
import com.kakao.sdk.template.model.Link
import com.widthus.app.model.CalendarDay
import com.widthus.app.model.MemoryItem
import com.widthus.app.model.ScheduleItem
import com.widthus.app.screen.AppNavigation
import com.widthus.app.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import java.io.File
import java.util.Calendar
import kotlin.collections.getOrNull
import com.withus.app.R

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                ) { Text("다음 화면 (캘린더) 보기") }
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
            // D-day 섹션 (기존 유지)
            RoundedGrayBox(modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(modifier = Modifier
                        .size(50.dp)
                        .background(Color(0xFFD6D6D6), CircleShape))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(nickname.ifEmpty { "유저" }, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("D+300", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("상대방", fontSize = 14.sp)
                    }
                    Box(modifier = Modifier
                        .size(50.dp)
                        .background(Color(0xFFD6D6D6), CircleShape))
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
            RoundedGrayBox(modifier = Modifier
                .fillMaxWidth()
                .height(160.dp), color = Color(0xFFF2F2F2)) {
                Column(modifier = Modifier
                    .padding(20.dp)
                    .fillMaxSize()) {
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun StepInputScreen(
    viewModel: MainViewModel,
    onAllFinish: () -> Unit
) {
    // 이제 단계는 1(닉네임)과 4(프로필)만 사용합니다.
    var currentStep by remember { mutableStateOf(1) }
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current

    val cameraPermissionState = rememberPermissionState(
        permission = CAMERA
    )

    // 카메라/갤러리 런처 로직 (기존과 동일)
    val tempImageUri = remember {
        val file = File.createTempFile("profile_", ".jpg", context.externalCacheDir)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) viewModel.updateProfileImage(tempImageUri)
        showSheet = false
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.updateProfileImage(uri)
        showSheet = false
    }
    // 에러 메시지 표시 로직
    val isNicknameError = viewModel.nickname.isNotEmpty() && (viewModel.nickname.length < 2 || viewModel.nickname.length > 8)

    // 유효성 검사: 1단계는 닉네임 필수, 4단계는 건너뛰기 가능하므로 항상 true
    val currentValid = when(currentStep) {
        1 -> !isNicknameError
        else -> true
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            // 4단계(프로필)일 때만 뒤로가기 버튼 표시
            if (currentStep == 4) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 8.dp)
                ) {
                    IconButton(onClick = { currentStep = 1 }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = Color.Black
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // 타이틀 및 설명 영역
            Text(
                text = if (currentStep == 1) "위더스에서 활동할 닉네임은?" else "프로필 사진을 등록해 주세요",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (currentStep == 1)
                    "상대방에게 주로 불리는 애칭을 입력해도 좋아요"
                else "사진을 등록하지 않으면 기본 프로필이 보여집니다.",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(60.dp))

            // 입력 영역
            if (currentStep == 1) {
                // 1단계: 닉네임 입력 (중앙 정렬)
                OutlinedTextField(
                    value = viewModel.nickname,
                    onValueChange = { if (it.length <= 8) viewModel.updateNickname(it) },
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        color = Color.Black
                    ),
                    placeholder = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("닉네임을 입력해주세요.", color = Color(0xFFC7C7C7), fontSize = 18.sp)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(8.dp), // 네모 박스의 둥글기 조절
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,   // 포커스 되었을 때 테두리 색
                        unfocusedBorderColor =  Color(0xFFF0F0F0), // 기본 테두리 색
                        cursorColor = Color.Black,
                        focusedContainerColor =  Color(0xFFF0F0F0),
                        unfocusedContainerColor = Color(0xFFF0F0F0)
                    )
                )

                // 메시지 영역의 높이를 고정(height)하면 메시지가 나타날 때 UI가 덜컹거리는 것을 방지할 수 있습니다.
                Box(modifier = Modifier.height(30.dp).padding(top = 8.dp)) {
                    if (isNicknameError) {
                        Text(
                            text = "2~8자 이내로 입력해주세요.",
                            color = Color(0xFFF5A7B8),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // 4단계: 프로필 이미지 등록
                Box(
                    modifier = Modifier
                        .size(160.dp) // 버튼 공간까지 고려하여 전체 크기 설정
                        .clickable(
                        ) {
                            if (viewModel.profileImageUri == null) {
                                showSheet = true
                            }
                          },
                    contentAlignment = Alignment.Center
                ) {
                    // 4단계: 프로필 이미지 등록
                    Box(
                        modifier = Modifier
                            .size(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // 1. 메인 프로필 원형 박스 (테두리 검정, 안쪽 회색)
                        Box(
                            modifier = Modifier
                                .size(150.dp)

                                .background(Color(0xFFD9D9D9), CircleShape) // 안쪽은 회색 배경
                                .clip(CircleShape)
                                .clickable {
                                    if (viewModel.profileImageUri == null) showSheet = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (viewModel.profileImageUri != null) {
                                // 이미지가 있을 때: 사진 표시
                                AsyncImage(
                                    model = viewModel.profileImageUri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                // 이미지가 없을 때: 격자 아이콘 표시
                                Icon(
                                    painter = painterResource(id = R.drawable.photo_grid),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(100.dp) // 격자는 조금 더 크게 설정
                                )
                            }
                        }

                        // 2. 우측 하단 카메라 추가 버튼 (이미지가 없을 때만 표시)
                        if (viewModel.profileImageUri == null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd) // 가장 바깥 Box 기준 우측 하단
                                    .padding(bottom = 10.dp, end = 10.dp) // 테두리 위에 걸치도록 위치 조정
                                    .size(44.dp)
                                    .shadow(4.dp, CircleShape)
                                    .background(Color.White, CircleShape)
                                    .clickable { showSheet = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.photo_add),
                                    contentDescription = "사진 추가",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))

            // 하단 버튼
            Button(
                onClick = {
                    if (currentStep == 1) currentStep = 4 else onAllFinish()
                },
                enabled = currentValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if(currentValid) Color.Black else Color(0xFFE0E0E0)
                )
            ) {
                Text(
                    text = if(currentStep == 1) "다음" else "프로필 완성하기",
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 바텀 시트 (기존과 동일)
        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 40.dp, start = 20.dp, end = 20.dp)
                ) {
                    Text("프로필 사진 설정", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                    ListItem(
                        headlineContent = { Text("사진 촬영") },
                        leadingContent = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                        modifier = Modifier.clickable {
                            // 권한 체크 후 분기 처리
                            when {
                                cameraPermissionState.status.isGranted -> {
                                    // 권한이 이미 있음: 카메라 바로 실행
                                    cameraLauncher.launch(tempImageUri)
                                }
                                cameraPermissionState.status.shouldShowRationale -> {
                                    // 사용자가 한 번 거절했음: 왜 필요한지 설명 후 다시 요청
                                    // (간단하게 토스트를 띄우거나 바로 다시 요청할 수 있음)
                                    cameraPermissionState.launchPermissionRequest()
                                }
                                else -> {
                                    // 처음 요청하거나 거절된 상태: 권한 요청 팝업 띄우기
                                    cameraPermissionState.launchPermissionRequest()
                                }
                            }
                        }
                    )
                    ListItem(
                        headlineContent = { Text("앨범에서 가져오기") },
                        leadingContent = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        modifier = Modifier.clickable { galleryLauncher.launch("image/*") }
                    )
                }
            }
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // D-day 카드
            RoundedGrayBox(modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(modifier = Modifier
                        .size(50.dp)
                        .background(Color(0xFFD6D6D6), CircleShape))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(nickname.ifEmpty { "유저" }, fontSize = 14.sp) // 닉네임 표시
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("D+300", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("상대방", fontSize = 14.sp)
                    }
                    Box(modifier = Modifier
                        .size(50.dp)
                        .background(Color(0xFFD6D6D6), CircleShape))
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
            Text("오늘 일정", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            RoundedGrayBox(modifier = Modifier
                .fillMaxWidth()
                .height(100.dp))
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            RoundedGrayBox(modifier = Modifier
                .fillMaxWidth()
                .height(160.dp), color = Color(0xFFF2F2F2)) {
                Column(modifier = Modifier
                    .padding(20.dp)
                    .fillMaxSize()) {
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
                        modifier = Modifier
                            .size(64.dp)
                            .offset(y = (-10).dp) // 살짝 위로 올림
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
            RoundedGrayBox(modifier = Modifier
                .fillMaxWidth()
                .height(100.dp))
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
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
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
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 첫 번째 사진 (왼쪽 큰 거)
            val firstImg = memories.getOrNull(0)?.imageResId
            PhotoBox(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                imageResId = firstImg
            )

            // 오른쪽 컬럼
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 두 번째 사진 (우측 상단)
                val secondImg = memories.getOrNull(1)?.imageResId
                PhotoBox(modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(), imageResId = secondImg)

                // 세 번째 사진 (우측 하단)
                val thirdImg = memories.getOrNull(2)?.imageResId
                PhotoBox(modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(), imageResId = thirdImg)
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
    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
fun RoundedGrayBox(modifier: Modifier = Modifier, color: Color = Color(0xFFF2F2F2), content: @Composable BoxScope.() -> Unit = {}) {
    Box(modifier = modifier
        .clip(RoundedCornerShape(16.dp))
        .background(color), content = content)
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
    Row(modifier = Modifier
        .fillMaxWidth()
        .height(200.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RoundedGrayBox(modifier = Modifier
            .weight(1f)
            .fillMaxHeight())
        Column(modifier = Modifier
            .weight(1f)
            .fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RoundedGrayBox(modifier = Modifier
                .weight(1f)
                .fillMaxWidth())
            RoundedGrayBox(modifier = Modifier
                .weight(1f)
                .fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class) // TopAppBar 사용을 위해 필요
@Composable
fun OnboardingConnectScreen(
    viewModel: MainViewModel,
    onInviteClick: () -> Unit,
    onEnterCodeClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    Scaffold(
        containerColor = Color.White,
        topBar = {
            // 상단 바 영역
            TopAppBar(
                title = { }, // 제목은 비워둠
                actions = {
                    // 오른쪽 버튼들 (actions)
                    IconButton(onClick = onCloseClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = "닫기",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues) // 상단 바 영역만큼 띄워줌
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("${viewModel.nickname}님, 가입을 축하드려요!", fontSize = 18.sp)
            Text("상대방을 연결하고\n둘만의 추억을 쌓아가요",
                fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(40.dp))

            // 프로필 이미지 영역
            Box(
                modifier = Modifier
                    .size(width = 300.dp, height = 200.dp)
                    .background(Color(0xFFE6E6E6)),
                contentAlignment = Alignment.Center
            ) {
                if (viewModel.profileImageUri != null) {
                    AsyncImage(
                        model = viewModel.profileImageUri,
                        contentDescription = "프로필 이미지",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color.LightGray)
                }
            }

            Spacer(modifier = Modifier.height(60.dp))

            // 1. 상대방 코드 입력하기 버튼
            Button(
                onClick = onEnterCodeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(1.dp, Color.Black, RoundedCornerShape(8.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("상대방 코드 입력하기", color = Color.Black)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. 내 코드로 초대하기 버튼
            Button(
                onClick = onInviteClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("내 코드로 초대하기", color = Color.White)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class) // TopAppBar 사용을 위해 필요
@Composable
fun ConnectionPendingScreen(
    viewModel: MainViewModel,
    onConnectClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "앗!\n아직 커플 연결이 되지 않았어요",
            fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 프로필 이미지 영역
        Box(
            modifier = Modifier
                .size(width = 300.dp, height = 200.dp)
                .background(Color(0xFFE6E6E6)),
            contentAlignment = Alignment.Center
        ) {
            if (viewModel.profileImageUri != null) {
                AsyncImage(
                    model = viewModel.profileImageUri,
                    contentDescription = "프로필 이미지",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = Color.LightGray
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "연결을 완료하고 \n사진으로 일상을 공유해보세요!",
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onConnectClick,
            modifier = Modifier
                .padding(horizontal = 96.dp)
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("연결하러 가기 →", color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
fun ConnectConfirmScreen(
    viewModel: MainViewModel,
    onConfirmClick: () -> Unit,
    onLaterClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("${viewModel.nickname}님이\n ${viewModel.partnerNickname} 님을 초대했어요!",
            fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("초대를 수락하면, 두 사람의 기록이 이어져요", fontSize = 18.sp)

        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .size(width = 300.dp, height = 200.dp)
                .background(Color(0xFFE6E6E6)),
            contentAlignment = Alignment.Center
        ) {
            if (viewModel.profileImageUri != null) {
                AsyncImage(
                    model = viewModel.profileImageUri,
                    contentDescription = "프로필 이미지",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color.LightGray)
            }
        }

        Spacer(modifier = Modifier.height(60.dp))

        Button(
            onClick = onConfirmClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black // 배경 검은색
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("초대 수락하기", color = Color.White) // 텍스트 흰색
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onLaterClick) {
            Text("다음에 할래요", color = Color.Gray, textDecoration = TextDecoration.Underline)
        }
    }
}

@Composable
fun ConnectCompleteScreen(
    viewModel: MainViewModel,
    onStartClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("커플 연결 완료!",
            fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("둘만의 사진 기록을 시작해 보세요", fontSize = 18.sp)

        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .size(width = 300.dp, height = 121.dp)
                .background(Color(0xFFE6E6E6)),
            contentAlignment = Alignment.Center
        ) {
            if (viewModel.profileImageUri != null) {
                AsyncImage(
                    model = viewModel.profileImageUri,
                    contentDescription = "프로필 이미지",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color.LightGray)
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        Box(
            modifier = Modifier
                .size(width = 300.dp, height = 121.dp)
                .background(Color(0xFFE6E6E6)),
            contentAlignment = Alignment.Center
        ) {
            if (viewModel.profileImageUri != null) {
                AsyncImage(
                    model = viewModel.profileImageUri,
                    contentDescription = "프로필 이미지",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color.LightGray)
            }
        }

        Spacer(modifier = Modifier.height(60.dp))

        Button(
            onClick = onStartClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black // 배경 검은색
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("시작하기", color = Color.White) // 텍스트 흰색
        }
    }
}

@Composable
fun InviteScreen(onBack: () -> Unit) {
    var showCopyPopup by remember { mutableStateOf(false) }
    val myCode = "99744211" // 서버에서 받아온 내 초대 코드 예시
    val context = LocalContext.current

    // 복사 완료 팝업 자동 사라짐 로직
    LaunchedEffect(showCopyPopup) {
        if (showCopyPopup) {
            delay(2000)
            showCopyPopup = false
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            IconButton(onClick = onBack, modifier = Modifier.padding(16.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    text = "상대방에게 코드를\n공유해서 초대해 보세요",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(60.dp))

                // 1. 내 코드를 밑줄 UI 위에 표시
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    myCode.forEach { char ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = char.toString(),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // 모든 숫자가 이미 존재하므로 검정색 밑줄 표시
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(2.dp)
                                    .background(Color.Black)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // 2. 코드 복사 버튼 (흰색 배경 + 검정 테두리)
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Invite Code", myCode)
                        clipboard.setPrimaryClip(clip)
                        showCopyPopup = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .border(1.dp, Color.Black, RoundedCornerShape(8.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_copy), // 복사 아이콘
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("코드 복사", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. 링크 공유 버튼 (검정 배경)
                Button(
                    onClick = {
                        // 1. 공유할 텍스트 내용 작성

                        val shareText = "[위더스] 상대방이 보낸 초대 코드: $myCode\n\n" +
                                "아래 링크를 누르면 바로 연결 화면으로 이동해요!\n" +
                                "widthus://connect?code=$myCode" // 👈 커스텀 스킴 적용

                        // 2. 공유를 위한 인텐트 생성
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText) // 공유할 텍스트 삽입
                            type = "text/plain" // 전송 데이터 타입 (일반 텍스트)
                        }

                        // 버튼 클릭 시 실행
                        val defaultFeed = FeedTemplate(
                            content = Content(
                                title = "위더스(WITHÜS) 초대",
                                description = "상대방이 보낸 초대 코드: $myCode",
                                imageUrl = "https://your-image-url.com/logo.png", // 앱 로고나 대표 이미지 URL
                                link = Link(androidExecutionParams = mapOf("invite_code" to myCode))
                            ),
                            buttons = listOf(
                                Button(
                                    "앱에서 연결하기",
                                    Link(androidExecutionParams = mapOf("invite_code" to myCode))
                                )
                            )
                        )

                        // 카카오톡 설치 여부 확인 후 공유
                        if (ShareClient.instance.isKakaoTalkSharingAvailable(context)) {
                            ShareClient.instance.shareDefault(context, defaultFeed) { sharingResult, error ->
                                if (error != null) {
                                    Log.e("KAKAO", "공유 실패", error)
                                } else if (sharingResult != null) {
                                    context.startActivity(sharingResult.intent)
                                }
                            }
                        }

                        // 3. 공유 선택창(Chooser) 띄우기
//                        val shareIntent = Intent.createChooser(sendIntent, "초대 코드 공유하기")
//                        context.startActivity(shareIntent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_share), // 공유 아이콘
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("링크 공유", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 4. 복사 완료 토스트 팝업 (중앙 위치)
            AnimatedVisibility(
                visible = showCopyPopup,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 8.dp
                ) {
                    Text(
                        "코드가 성공적으로 복사되었어요!",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }
            }
        }
    }
}
@Composable
fun EnterCodeScreen(onBack: () -> Unit, onConnect: (String) -> Unit) {
    var codeInput by remember { mutableStateOf("") }
    val isComplete = codeInput.length == 8
    val focusRequester = remember { FocusRequester() }
    var isError by remember { mutableStateOf(true) } // 에러 상태 추가

    // 키보드가 올라올 때 버튼이 밀려 올라오도록 Scaffold 사용
    Scaffold(
        containerColor = Color.White,
        topBar = {
            IconButton(onClick = onBack, modifier = Modifier.padding(16.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        },
        bottomBar = {
            // 하단 버튼 영역: IME(키보드)에 반응하여 자동으로 위치 조절
            Box(modifier = Modifier
                .navigationBarsPadding() // 네비게이션 바 대응
                .imePadding() // 키보드가 올라오면 그만큼 패딩 추가
                .padding(24.dp)
            ) {
                Button(
                    onClick = { onConnect(codeInput) },
                    enabled = isComplete,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isComplete) Color.Black else Color(0xFFE0E0E0)
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("연결하기", color = Color.White, fontSize = 18.sp)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                "상대방에게 받은 코드를\n입력해 주세요",
                fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(60.dp))

            // 실제 입력을 받는 투명 TextField와 화면에 그려지는 밑줄 UI 조합
            Box(contentAlignment = Alignment.Center) {
                // 1. 각 숫자 아래 밑줄을 그리는 UI
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 8) {
                        val isEntered = i < codeInput.length
                        val char = if (isEntered) codeInput[i].toString() else "0"

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = char,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isEntered) Color.Black else Color(0xFFC7C7C7)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // 밑줄 UI: 입력되면 검은색, 아니면 회색
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(2.dp)
                                    .background(if (isEntered) Color.Black else Color(0xFFEEEEEE))
                            )
                        }
                    }
                }

                // 2. 실제 입력을 처리하는 투명 TextField
                BasicTextField(
                    value = codeInput,
                    onValueChange = {
                        if (it.length <= 8) codeInput = it.filter { c -> c.isDigit() }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    // 텍스트 색상을 투명하게 설정하여 물리적으로 숨김
                    textStyle = LocalTextStyle.current.copy(color = Color.Transparent),
                    cursorBrush = SolidColor(Color.Transparent), // 커서 숨김
                    decorationBox = { innerTextField ->
                        // innerTextField()를 호출하지 않거나, 투명한 Box로 감싸서 텍스트 노출 차단
                        Box(modifier = Modifier.fillMaxWidth()) {
                            innerTextField()
                        }
                    }
                )
            }

            if (isError) {
                CodeErrorView()
            }
        }
    }

    // 진입 시 키보드 자동 실행
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun CodeErrorView() {
    val errorColor = Color(0xFFFFB2BC) // 이미지와 유사한 핑크색

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 느낌표 아이콘 (!)
        Icon(
            painter = painterResource(id = R.drawable.code_warning),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = errorColor
        )
        Spacer(modifier = Modifier.width(4.dp))
        // 에러 텍스트
        Text(
            text = "초대코드를 다시 확인해주세요.",
            color = errorColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun isValidDate(dateString: String): Boolean {
    if (dateString.length != 8) return false
    return try {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")
        java.time.LocalDate.parse(dateString, formatter)
        true
    } catch (e: Exception) {
        false
    }
}

// 프리뷰
@androidx.compose.ui.tooling.preview.Preview
@Composable
fun PreviewApp() {
    AppNavigation()
}