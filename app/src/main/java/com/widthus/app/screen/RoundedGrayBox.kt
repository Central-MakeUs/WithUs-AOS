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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
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
import com.widthus.app.screen.BackButton
import com.widthus.app.screen.EditMode
import com.widthus.app.screen.ImageMediaManager
import com.widthus.app.screen.Screen
import com.widthus.app.utils.DateMaskTransformation
import com.widthus.app.utils.Utils.calculateRemainingTime
import com.widthus.app.utils.Utils.checkIsTimePassed
import com.widthus.app.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import java.io.File
import java.util.Calendar
import kotlin.collections.getOrNull
import com.withus.app.R
import org.withus.app.debug
import org.withus.app.model.CoupleQuestionData
import org.withus.app.model.JoinCouplePreviewData

@Composable
fun TestHomeScreen(
    nickname: String,
    schedules: List<ScheduleItem>,
    memories: List<MemoryItem>,
    onNavigateToCalendar: () -> Unit
) {
    Scaffold(
        topBar = { TopTitleBar("LOGO") }, bottomBar = {
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
        }, containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // D-day 섹션 (기존 유지)
            RoundedGrayBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color(0xFFD6D6D6), CircleShape)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(nickname.ifEmpty { "유저" }, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("D+300", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("상대방", fontSize = 14.sp)
                    }
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color(0xFFD6D6D6), CircleShape)
                    )
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
    nickname: String, memories: List<MemoryItem>, onNavigateToDayUs: () -> Unit
) {
    val today = remember { Calendar.getInstance().get(Calendar.DAY_OF_MONTH) }
    var selectedDate by remember { mutableStateOf(today) }

    Scaffold(
        topBar = { TopTitleBar("LOGO") }, bottomBar = {
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
        }, containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 상단 D-Day 카드
            RoundedGrayBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp), color = Color(0xFFF2F2F2)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxSize()
                ) {
                    Text("${nickname}님과 함께한지", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("300일", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 주간 캘린더 영역
            WeeklyCalendar(
                selectedDate = selectedDate, onDateSelected = { newDate -> selectedDate = newDate })

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
    viewModel: MainViewModel, mediaManager: ImageMediaManager,
    onAllFinish: () -> Unit
) {
    // 이제 단계는 1(닉네임)과 4(프로필)만 사용합니다.
    var currentStep by remember { mutableStateOf(1) }
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current

    // 에러 상태 체크
    val isNicknameError =
        viewModel.nickname.isNotEmpty() && (viewModel.nickname.length < 2 || viewModel.nickname.length > 8)
    // 생일 에러: 입력이 시작되었으나 8자가 아닐 때
    val isBirthdayError = viewModel.birthdayValue.toString()
        .isNotEmpty() && viewModel.birthdayValue.toString().length < 8

    val raw = viewModel.birthdayValue.text
    val digits = raw.filter { it.isDigit() }

    debug("viewModel.birthdayValue.text='${raw}', digits='${digits}', textLen=${raw.length}, digitLen=${digits.length}, selection=${viewModel.birthdayValue.selection}")
    // 버튼 활성화 유효성 검사
    val currentValid = when (currentStep) {
        1 -> viewModel.nickname.length in 2..8
        2 -> {
            val digits = viewModel.birthdayValue.text.filter { it.isDigit() }
            // 8자리이면서 + 실제 유효한 날짜여야 true
            digits.length == 8 && isValidDate(digits)
        }

        else -> true
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            // 2단계나 4단계일 때 뒤로가기 버튼 표시
            if (currentStep != 1) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 8.dp)) {
                    IconButton(onClick = {
                        currentStep = if (currentStep == 4) 2 else 1
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로가기", tint = Color.Black)
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

            // 1. 타이틀 영역
            Text(
                text = when (currentStep) {
                    1 -> "위더스에서 활동할 닉네임은?"
                    2 -> "생일을 입력해 주세요"
                    else -> "프로필 사진을 등록해 주세요"
                },
                fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = when (currentStep) {
                    1 -> "상대방에게 주로 불리는 애칭을 입력해도 좋아요"
                    2 -> "서로의 생일에 특별한 사진을 주고 받아요"
                    else -> "사진을 등록하지 않으면 기본 프로필이 보여집니다."
                },
                fontSize = 16.sp, color = Color.Gray, textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(60.dp))

            val currentText = if (currentStep == 1) {
                viewModel.nickname
            } else {
                viewModel.birthdayValue.toString() // 명시적으로 String 변환
            }

            // 2. 입력 영역 (닉네임 & 생일)
            if (currentStep == 1 || currentStep == 2) {
                val textValue = if (currentStep == 1) {
                    // String인 nickname을 TextFieldValue로 변환 (커서 위치는 마지막으로 설정)
                    TextFieldValue(
                        text = currentText,
                        selection = TextRange(viewModel.nickname.length)
                    )
                } else {
                    // 이미 TextFieldValue인 birthdayValue 사용
                    viewModel.birthdayValue
                }

                OutlinedTextField(
                    value = textValue, // 이제 항상 TextFieldValue 타입입니다.
                    onValueChange = { newValue ->
                        if (currentStep == 1) {
                            // 닉네임 업데이트 (String만 추출해서 전달)
                            if (newValue.text.length <= 8) {
                                viewModel.updateNickname(newValue.text)
                            }
                        } else {
                            viewModel.updateBirthday(newValue)
                        }
                    },
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 18.sp, textAlign = TextAlign.Center, color = Color.Black
                    ),
                    placeholder = {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (currentStep == 1) "닉네임을 입력해주세요" else "YYYY-MM-DD",
                                color = Color(0xFFC7C7C7),
                                fontSize = 18.sp
                            )
                        }
                    },
                    // 2단계(생일)일 때만 마스크 및 숫자 키패드 적용
//                    visualTransformation = if (currentStep == 2) DateMaskTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (currentStep == 2) KeyboardType.Number else KeyboardType.Text
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color(0xFFF0F0F0),
                        cursorColor = Color.Black,
                        focusedContainerColor = Color(0xFFF0F0F0),
                        unfocusedContainerColor = Color(0xFFF0F0F0)
                    )
                )

                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .padding(top = 8.dp)
                ) {
                    if (!currentValid) {
                        Text(
                            text = when (currentStep) {
                                1 -> "2~8자 이내로 입력해주세요."
                                2 -> "올바른 생년월일을 입력해주세요."
                                else -> {
                                    ""
                                }
                            },
                            color = Color(0xFFF5A7B8),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

            } else {
                // (4단계 프로필 등록 UI - 기존 코드 유지)
                ProfileImagePicker(viewModel.profileImageUri) { showSheet = true }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 4. 하단 버튼
            Button(
                onClick = {
                    when (currentStep) {
                        1 -> currentStep = 2
                        2 -> currentStep = 4
                        4 -> onAllFinish()
                    }
                },
                enabled = currentValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentValid) Color.Black else Color(0xFFE0E0E0)
                )
            ) {
                Text(
                    text = if (currentStep == 4) "프로필 완성하기" else "다음",
                    fontSize = 18.sp, color = Color.White
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
                    Text(
                        "프로필 사진 설정",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    ListItem(headlineContent = { Text("사진 촬영") }, leadingContent = {
                        Icon(
                            Icons.Default.AccountCircle, contentDescription = null
                        )
                    }, modifier = Modifier.clickable {
                        mediaManager.launchCamera {
                            viewModel.profileImageUri = it
                        }
                    })
                    ListItem(headlineContent = { Text("앨범에서 가져오기") }, leadingContent = {
                        Icon(
                            Icons.Default.DateRange, contentDescription = null
                        )
                    }, modifier = Modifier.clickable {
                        mediaManager.launchGallery {
                            viewModel.profileImageUri = it
                        }
                    })
                }
            }
        }
    }
}

// --- 홈 화면 ---
@Composable
fun TestHomeScreen(
    nickname: String, onNavigateToCalendar: () -> Unit
) {
    Scaffold(
        topBar = { TopTitleBar("LOGO") }, bottomBar = {
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
        }, containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // D-day 카드
            RoundedGrayBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color(0xFFD6D6D6), CircleShape)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(nickname.ifEmpty { "유저" }, fontSize = 14.sp) // 닉네임 표시
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("D+300", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("상대방", fontSize = 14.sp)
                    }
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color(0xFFD6D6D6), CircleShape)
                    )
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
            Text("오늘 일정", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            RoundedGrayBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
            Spacer(modifier = Modifier.height(30.dp))
            MemoryGridSection()
        }
    }
}

// --- 캘린더 홈 화면 ---
@Composable
fun CalendarHomeScreen(
    nickname: String, onNavigateToDayUs: () -> Unit
) {
    Scaffold(
        topBar = { TopTitleBar("LOGO") }, bottomBar = {
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
        }, containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            RoundedGrayBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp), color = Color(0xFFF2F2F2)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxSize()
                ) {
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
        }, bottomBar = {
            // 커스텀 하단 바 (FAB가 중앙에 있는 형태)
            BottomAppBar(containerColor = Color.White, tonalElevation = 10.dp, actions = {
                IconButton(onClick = {}) {
                    Icon(
                        Icons.Outlined.GridView, contentDescription = "Menu"
                    )
                }
                Spacer(modifier = Modifier.weight(1f)) // 중앙 공간 확보
                IconButton(onClick = {}) {
                    Icon(
                        Icons.Default.Person, contentDescription = "Profile", tint = Color.LightGray
                    )
                }
            }, floatingActionButton = {
                FloatingActionButton(
                    onClick = {},
                    containerColor = Color(0xFF1C1C1E), // 검은색에 가까운 다크그레이
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(64.dp)
                        .offset(y = (-10).dp) // 살짝 위로 올림
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add",
                        modifier = Modifier.size(32.dp)
                    )
                }
            })
        }, containerColor = Color.White
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
                    ), contentAlignment = Alignment.Center
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
                    modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center
                ) {
                    // 아이콘 겹침 효과 흉내
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(color = Color.Black, radius = size.minDimension / 2.2f)
                        drawCircle(
                            color = Color.White,
                            radius = size.minDimension / 2.2f,
                            center = center.copy(x = center.x + 20f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                        )
                    }
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
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
        Text(text, fontSize = 12.sp, color = if (isActive) Color.Black else Color.Gray)
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
            RoundedGrayBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
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
                    .fillMaxHeight(), imageResId = firstImg
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
                PhotoBox(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(), imageResId = secondImg
                )

                // 세 번째 사진 (우측 하단)
                val thirdImg = memories.getOrNull(2)?.imageResId
                PhotoBox(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(), imageResId = thirdImg
                )
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
                    .padding(4.dp)) {
                // 날짜 원형 배경
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCurrentSelected) Color.Black else Color(0xFFEEEEEE)
                        ), contentAlignment = Alignment.Center
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
fun RoundedGrayBox(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFF2F2F2),
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color), content = content
    )
}

@Composable
fun AppBottomNavigation() {
    NavigationBar(containerColor = Color(0xFFF2F2F2)) {
        listOf(Icons.Outlined.Home, Icons.Default.DateRange, Icons.Default.Person).forEach {
            NavigationBarItem(
                selected = false,
                onClick = {},
                icon = { Icon(it, contentDescription = null) })
        }
    }
}

@Composable
fun MemoryGridSection() {
    Text(
        "오늘 추억",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RoundedGrayBox(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RoundedGrayBox(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
            RoundedGrayBox(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class) // TopAppBar 사용을 위해 필요
@Composable
fun OnboardingConnectScreen(
    viewModel: MainViewModel,
    onInviteClick: () -> Unit,
    onEnterCodeClick: () -> Unit,
    onCloseClick: () -> Unit,
    topBar: @Composable () -> Unit,
    title: String = "${viewModel.nickname}님, 가입을 축하드려요!",
) {
    Scaffold(
        containerColor = Color.White, topBar = topBar
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues) // 상단 바 영역만큼 띄워줌
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "상대방을 연결하고\n둘만의 추억을 쌓아가요",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 프로필 이미지 영역
            Box(
                modifier = Modifier
                    .size(width = 300.dp, height = 200.dp), contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = R.drawable.img_onboarding_connect_invite,
                    contentDescription = null,
                    modifier = Modifier.size(200.dp)
                )
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

@Composable
fun ConnectionPendingScreen(
    viewModel: MainViewModel,
    title: String,
    body: String,
    buttonText: String,
    onConnectClick: () -> Unit,
    bottomBar: @Composable () -> Unit // 바텀 바를 인자로 받음
) {
    Scaffold(
        bottomBar = bottomBar, // Scaffold의 바텀 바 자리에 주입
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. 상단 타이틀
            Text(
                text = title,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 2. 중앙 이미지 (이미지처럼 둥근 사각형)
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(32.dp)) // 이미지와 유사한 둥근 모서리
                    .background(Color(0xFFF0F0F0)), contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = R.drawable.img_not_connected_yet,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // 3. 중앙 설명 문구 (요청하신 문구로 변경)
            Text(
                text = body,
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 4. 설정하러 가기 버튼 (이미지 스타일 적용)
            Button(
                onClick = onConnectClick,
                modifier = Modifier
                    .fillMaxWidth(0.7f) // 버튼 너비 조절
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        buttonText,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 바텀 바 공간 확보를 위한 마지막 스페이서
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun ConnectConfirmScreen(
    viewModel: MainViewModel,
    onConfirmClick: (String) -> Unit,
    onLaterClick: () -> Unit,
    navController: NavHostController
) {

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val previewFlow = savedStateHandle?.getStateFlow<JoinCouplePreviewData?>("join_preview", null)
    val preview by previewFlow?.collectAsState() ?: remember { mutableStateOf(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "${preview?.senderName}님이\n ${preview?.receiverName} 님을 초대했어요!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
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
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = Color.LightGray
                )
            }
        }

        Spacer(modifier = Modifier.height(60.dp))

        Button(
            onClick = {
                onConfirmClick(preview!!.inviteCode)
            },
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
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "커플 연결 완료!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
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
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = Color.LightGray
                )
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
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = Color.LightGray
                )
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
fun InviteScreen(onBack: () -> Unit, viewModel: MainViewModel) {
    var showCopyPopup by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val myCode by viewModel.myCode.collectAsState()
    val loading by viewModel.loading.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadInvitationCode() }

    // 복사 완료 팝업 자동 사라짐 로직
    LaunchedEffect(showCopyPopup) {
        if (showCopyPopup) {
            delay(2000)
            showCopyPopup = false
        }
    }

    Scaffold(
        containerColor = Color.White, topBar = {
            IconButton(onClick = onBack, modifier = Modifier.padding(16.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        }) { paddingValues ->
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues), // 상단 바 영역 침범 방지
                contentAlignment = Alignment.Center // 내용물을 정가운데로 정렬
            ) {
                CircularProgressIndicator()
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
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
                        myCode?.forEach { char ->
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
                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
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
                            Text(
                                "코드 복사",
                                color = Color.Black,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. 링크 공유 버튼 (검정 배경)
                    Button(
                        onClick = {
                            // 1. 공유할 텍스트 내용 작성

                            val shareText =
                                "[위더스] 상대방이 보낸 초대 코드: $myCode\n\n" + "아래 링크를 누르면 바로 연결 화면으로 이동해요!\n" + "widthus://connect?code=$myCode" // 👈 커스텀 스킴 적용

                            // 2. 공유를 위한 인텐트 생성
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText) // 공유할 텍스트 삽입
                                type = "text/plain" // 전송 데이터 타입 (일반 텍스트)
                            }

                            myCode?.let {
                                // 버튼 클릭 시 실행
                                val defaultFeed = FeedTemplate(
                                    content = Content(
                                        title = "위더스(WITHÜS) 초대",
                                        description = "상대방이 보낸 초대 코드: $myCode",
                                        imageUrl = "https://your-image-url.com/logo.png", // 앱 로고나 대표 이미지 URL
                                        link = Link(androidExecutionParams = mapOf("invite_code" to it))
                                    ), buttons = listOf(
                                        Button(
                                            "앱에서 연결하기",
                                            Link(androidExecutionParams = mapOf("invite_code" to it))
                                        )
                                    )
                                )

                                // 카카오톡 설치 여부 확인 후 공유
                                if (ShareClient.instance.isKakaoTalkSharingAvailable(context)) {
                                    ShareClient.instance.shareDefault(
                                        context, defaultFeed
                                    ) { sharingResult, error ->
                                        if (error != null) {
                                            Log.e("KAKAO", "공유 실패", error)
                                        } else if (sharingResult != null) {
                                            context.startActivity(sharingResult.intent)
                                        }
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
                            Text(
                                "링크 공유",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
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
}

@Composable
fun EnterCodeScreen(
    onBack: () -> Unit,
    // 변경: onConnect가 코드와 결과 콜백(성공여부, 에러메시지)을 받음
    onConnect: (String, (Boolean, String?) -> Unit) -> Unit
) {
    var codeInput by remember { mutableStateOf("") }
    val isComplete = codeInput.length == 8
    val focusRequester = remember { FocusRequester() }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("초대코드를 다시 확인해주세요.") }

    // 키보드가 올라올 때 버튼이 밀려 올라오도록 Scaffold 사용
    Scaffold(containerColor = Color.White, topBar = {
        IconButton(onClick = onBack, modifier = Modifier.padding(16.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }
    }, bottomBar = {
        // 하단 버튼 영역: IME(키보드)에 반응하여 자동으로 위치 조절
        Box(
            modifier = Modifier
                .navigationBarsPadding() // 네비게이션 바 대응
                .imePadding() // 키보드가 올라오면 그만큼 패딩 추가
                .padding(24.dp)
        ) {
            Button(
                onClick = {
                    onConnect(codeInput) { success, message ->
                        if (success) {
                            isError = false
                        } else {
                            isError = true
                            // 서버에서 받은 메시지가 있으면 업데이트, 없으면 기본 메시지 유지
                            if (message != null) {
                                errorMessage = message
                            }
                        }
                    }
                },
                enabled = isComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isComplete) Color.Black else Color(0xFFE0E0E0)
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("연결하기", color = Color.White, fontSize = 18.sp)
            }
        }
    }) { paddingValues ->
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
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
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
                    })
            }

            if (isError) {
                CodeErrorView(message = errorMessage) // 메시지 전달
            }
        }
    }

    // 진입 시 키보드 자동 실행
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun CodeErrorView(message: String) {
    val errorColor = Color(0xFFFFB2BC)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.code_warning),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = errorColor
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = message,
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun KeywordSelectionScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
    isMyPage: Boolean = false
) {
    // 기본 키워드 리스트 (가변 리스트로 선언하여 추가 가능하게 함)

    val keywordList by viewModel.displayKeywords.collectAsState()

    LaunchedEffect(Unit) {
        if (keywordList.isEmpty()) {
            viewModel.loadDefaultKeywords()
        }
    }

    var selectedKeywords by remember { mutableStateOf(setOf<String>()) }
    var showAddSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.White, topBar = {
            CenterAlignedTopAppBar(
                title = {

                },
                navigationIcon = { BackButton(onBackClick) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White),
                actions = {

                }
            )

        }) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 54.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // (상단 타이틀 및 설명 부분 동일...)

            Text(
                "연인과 자주 사진을 주고받는\n" +
                        "일상 키워드를 골라 주세요", fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "새로운 키워드를 이후에 추가할 수 있어요",
                fontSize = 18.sp,
            )

            Spacer(modifier = Modifier.height(70.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center
            ) {
                // 3. 서버에서 받아온 리스트로 칩 생성
                keywordList.forEach { keyword ->
                    val keywordContent = keyword.content
                    KeywordChip(
                        text = keywordContent,
                        isSelected = selectedKeywords.contains(keywordContent),
                        onClick = {
                            selectedKeywords = if (selectedKeywords.contains(keywordContent)) {
                                selectedKeywords - keywordContent
                            } else {
                                selectedKeywords + keywordContent
                            }
                        }
                    )
                }

                KeywordChip(
                    text = "+ 직접 추가",
                    isSelected = false,
                    onClick = { showAddSheet = true },
                    isAddButton = true
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            WithUsButton(
                text = if (isMyPage) "수정하기" else "다음",
                enabled = selectedKeywords.isNotEmpty(),
                onClick = {
                    // ViewModel의 API 호출 함수 실행
                    viewModel.saveKeywords(selectedKeywords) { isSuccess ->
                        if (isSuccess) {
                            onNextClick() // 성공 시 다음 화면 이동
                        } else {
                            // 에러 처리 (예: Toast 메시지)
                        }
                    }
                },
                modifier = Modifier.padding(bottom = 24.dp)
            )

        }

        // --- 새로운 키워드 추가 바텀 시트 ---
        if (showAddSheet) {
            AddKeywordBottomSheet(
                onDismissRequest = { showAddSheet = false },
                onKeywordAdded = { newKeyword ->
                    // 5. ViewModel을 통해 UI 리스트 업데이트
                    viewModel.addCustomKeywordToDisplay(newKeyword)
                    // 추가된 키워드 바로 선택 상태로
                    selectedKeywords = selectedKeywords + newKeyword
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotificationTimeScreen(
    onBackClick: () -> Unit, onFinish: (String) -> Unit // "08:00 PM" 형식으로 전달
) {
    // 1. 데이터 정의
    val hours = (1..12).toList()
    val minutes = (0..59).toList()
    val amPm = listOf("AM", "PM")

    // 2. 페이저 상태 (초기값 설정: 8시 00분 PM)
    val hourPagerState = rememberPagerState(initialPage = 7) { hours.size }
    val minutePagerState = rememberPagerState(initialPage = 0) { minutes.size }
    val amPmPagerState = rememberPagerState(initialPage = 1) { amPm.size }

    Scaffold(
        containerColor = Color.White, topBar = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                "오늘의 랜덤 질문을\n받을 시간을 정해 주세요",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            // 3. 중앙 휠 피커 영역
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(), contentAlignment = Alignment.Center
            ) {
                // 선택 영역 강조 배경
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    color = Color(0xFFF5F5F5),
                    shape = RoundedCornerShape(8.dp)
                ) {}

                // 휠 피커들
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 시(Hour)
                    WheelPicker(state = hourPagerState, items = hours)
                    Text(":", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    // 분(Minute)
                    WheelPicker(state = minutePagerState, items = minutes, format = "%02d")
                    Spacer(modifier = Modifier.width(16.dp))
                    // AM/PM
                    WheelPicker(state = amPmPagerState, items = amPm)
                }
            }

            // 4. 완료 버튼 클릭 시 실제 선택된 값 추출
            WithUsButton(
                text = "설정 완료하기", onClick = {
                    val finalHour = hours[hourPagerState.currentPage]
                    val finalMinute = minutes[minutePagerState.currentPage]
                    val finalAmPm = amPm[amPmPagerState.currentPage]
                    val timeResult =
                        String.format("%02d:%02d %s", finalHour, finalMinute, finalAmPm)

                    onFinish(timeResult) // 예: "08:00 PM"
                }, modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> WheelPicker(
    state: PagerState, items: List<T>, format: String? = null
) {
    VerticalPager(
        state = state, modifier = Modifier
            .width(60.dp)
            .height(150.dp), // 3개 정도 보이게 높이 조절
        contentPadding = PaddingValues(vertical = 50.dp) // 중앙 정렬 효과
    ) { page ->
        val isSelected = state.currentPage == page
        Box(
            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (format != null) String.format(
                    format, items[page]
                ) else items[page].toString(),
                fontSize = if (isSelected) 22.sp else 18.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.Black else Color.LightGray
            )
        }
    }
}

@Composable
fun WithUsButton(
    text: String, onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) Color(0xFF222222) else Color(0xFFE0E0E0),
            contentColor = Color.White,
            disabledContainerColor = Color(0xFFE0E0E0),
            disabledContentColor = Color.White
        )
    ) {
        Text(
            text = text, fontSize = 16.sp, fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun KeywordChip(
    text: String, isSelected: Boolean, onClick: () -> Unit, isAddButton: Boolean = false
) {
    Surface(
        modifier = Modifier
            .padding(6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = if (isSelected) Color(0xFFF05A5A) else Color.White, // 선택 시 빨간색
        border = BorderStroke(1.dp, if (isSelected) Color.Transparent else Color(0xFFE0E0E0))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            fontSize = 16.sp,
            color = if (isSelected) Color.White else if (isAddButton) Color.Gray else Color.Black,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddKeywordBottomSheet(
    onDismissRequest: () -> Unit, onKeywordAdded: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val isEnabled = text.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Black) }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp), // 키보드 고려 및 하단 여백
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "새로운 키워드 추가",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // 입력 필드 (이미지 스타일)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("키워드를 입력해주세요.", color = Color.LightGray) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color(0xFFF5F5F5),
                    unfocusedContainerColor = Color(0xFFF5F5F5)
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 공통 버튼 사용
            WithUsButton(
                text = "추가하기", onClick = {
                    if (isEnabled) {
                        onKeywordAdded(text)
                        onDismissRequest()
                    }
                }, enabled = isEnabled
            )
        }
    }
}

// (임시 Placeholder 아이콘 - 실제 프로젝트 리소스로 교체 필요)
val PlaceholderCameraIcon = Icons.Default.CameraAlt
val PlaceholderGalleryIcon = Icons.Default.PhotoLibrary

@RequiresApi(Build.VERSION_CODES.S) // 블러 효과를 위해 필요 (Android 12+)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    mediaManager: ImageMediaManager,
    myKeywords: List<String>,
    onNavigateToKeywordSelect: () -> Unit
) {
    // ... (상단 탭 및 Scaffold 기존 코드와 동일) ...
    // 변수 선언부 (mainTabs, selectedMainTab 등)는 기존 유지

    val mainTabs = listOf("오늘의 질문", "오늘의 일상")
    var selectedMainTab by remember { mutableStateOf("오늘의 질문") }
    var selectedDailyKeyword by remember(myKeywords) { mutableStateOf(myKeywords.firstOrNull()) }
    val keywords by viewModel.displayKeywords.collectAsState()

    // 업로드 상태
    val userImageUri = viewModel.userUploadedImage
    val partnerImageUri = viewModel.partnerUploadedImage

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("WITHUS", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()) {

            // 1. 메인 탭 (질문 vs 일상) UI 구현 (기존 코드 유지)
            Row(modifier = Modifier.fillMaxWidth()) {
                mainTabs.forEach { tabTitle ->
                    val isSelected = selectedMainTab == tabTitle
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedMainTab = tabTitle },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = tabTitle,
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.Black else Color.Gray,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isSelected) 2.dp else 1.dp)
                                .background(if (isSelected) Color(0xFFF05A5A) else Color(0xFFEEEEEE))
                        )
                    }
                }
            }

            // 2. 컨텐츠 영역
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(30.dp))

                if (selectedMainTab == "오늘의 질문") {
                    TodayQuestionContent(
                        data = viewModel.questionData, // 질문용 데이터
                        onUpload = { uri -> viewModel.uploadImage(uri) }, // 질문용 업로드
                        onPoke = { viewModel.pokePartner() },
                        showPokeDialog = viewModel.showPokeSuccessDialog,
                        onDismissPokeDialog = { viewModel.dismissPokeDialog() },
                        mediaManager = mediaManager
                    )
                } else {
                    if (myKeywords.isEmpty()) {
                        DailyEmptyContent(onRegisterClick = onNavigateToKeywordSelect)
                    } else {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            items(keywords) { keywordInfo ->
                                // ID 비교로 선택 여부 판단
                                val isSelected = viewModel.selectedKeywordId == keywordInfo.keywordId.toLong()

                                Surface(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .clickable {
                                            // 클릭 시 해당 ID를 선택하고 데이터를 불러옴
                                            viewModel.selectKeyword(keywordInfo.keywordId.toLong())
                                        },
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFFF05A5A) else Color(0xFFE0E0E0)
                                    )
                                ) {
                                    Text(
                                        text = keywordInfo.content,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        color = if (isSelected) Color(0xFFF05A5A) else Color.Gray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(30.dp))

                        // 공통 컨텐츠 영역 호출
                        TodayQuestionContent(
                            data = viewModel.dailyData, // API 조회 결과
                            onUpload = { uri -> viewModel.uploadDailyImage(uri) }, // 현재 선택된 ID로 업로드
                            onPoke = { viewModel.pokePartner() },
                            showPokeDialog = viewModel.showPokeSuccessDialog,
                            onDismissPokeDialog = { viewModel.dismissPokeDialog() },
                            mediaManager = mediaManager
                        )

                    }
                }
            }
        }
    }
}

// =================================================================================================
// 공통 UI 컴포넌트: 3가지 업로드 상태를 처리하는 컨테이너
// =================================================================================================
// =================================================================================================
// [수정됨] 공통 컨테이너: 이제 '둘 다 안 보냄'과 '둘 다 보냄' 상태 위주로 처리
// =================================================================================================
@Composable
fun PhotoResponseContainer(
    userImageUri: Uri?,
    partnerImageUri: Uri?,
    onUploadClick: () -> Unit,
    uploadButtonText: String,
    isDailyMode: Boolean
) {
    val isUserUploaded = userImageUri != null
    val isPartnerUploaded = partnerImageUri != null

    // 1. 둘 다 보냄 (스택형 UI)
    if (isUserUploaded && isPartnerUploaded) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))) {
            UploadedPhotoItem(
                imageUri = partnerImageUri,
                label = "상대방",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) // 높이 지정
            Spacer(modifier = Modifier.height(4.dp))
            UploadedPhotoItem(
                imageUri = userImageUri,
                label = "나",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
    // 2. 둘 다 안 보냄 (기본 대기 상태)
    else if (!isUserUploaded && !isPartnerUploaded) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFD9D9D9))
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onUploadClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222))
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_camera),
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                uploadButtonText,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
    // 나머지 케이스는 상위(Parent) 컴포넌트에서 별도 처리 (커스텀 UI) 했으므로 여기 올 일 없음
}

@Composable
fun UploadedPhotoItem(imageUri: Uri?, label: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        AsyncImage(
            model = imageUri,
            contentDescription = label,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            color = Color.Black.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun TodayQuestionContent(
    data: CoupleQuestionData?, // API에서 받아온 데이터 (질문 or 일상)
    onUpload: (Uri) -> Unit,    // 탭별 업로드 로직 주입 (질문용 or 일상용)
    onPoke: () -> Unit,        // 콕 찌르기 로직 주입
    showPokeDialog: Boolean,    // 콕 찌르기 다이얼로그 상태
    onDismissPokeDialog: () -> Unit,
    mediaManager: ImageMediaManager
) {
    if (data == null) return // 로딩 중 처리

    // API 응답 기반 상태 정의
    val myInfo = data.myInfo
    val partnerInfo = data.partnerInfo

    // 사진 업로드 여부 판단
    val isUserUploaded = myInfo?.questionImageUrl != null
    val isPartnerUploaded = partnerInfo?.questionImageUrl != null

    // 콕 찌르기 다이얼로그
    if (showPokeDialog) {
        PokeSuccessDialog(onDismiss = onDismissPokeDialog)
    }

    if (isUserUploaded) {
        // [나만 보냈거나 둘 다 보낸 경우] -> 2분할 레이아웃
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "#${data.coupleQuestionId ?: ""}.", fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = data.question,
                fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier.fillMaxWidth().height(500.dp).clip(RoundedCornerShape(32.dp)).background(Color(0xFFF5F5F5))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // [위 영역] 나
                    ImageSection(
                        imageUrl = myInfo?.questionImageUrl,
                        profileUrl = myInfo?.profileThumbnailImageUrl,
                        name = myInfo?.name ?: "나",
                        time = myInfo?.answeredAt ?: "방금 전",
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // [아래 영역] 상대방
                    if (isPartnerUploaded) {
                        ImageSection(
                            imageUrl = partnerInfo?.questionImageUrl,
                            profileUrl = partnerInfo?.profileThumbnailImageUrl,
                            name = partnerInfo?.name ?: "상대방",
                            time = partnerInfo?.answeredAt ?: "",
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // 상대방 대기 및 콕 찌르기 영역
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("사진을 기다리고 있다고\n상대방에게 알림을 보내보세요!", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = onPoke,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(painterResource(id = android.R.drawable.ic_input_add), null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("콕 찌르기", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // [아무도 안 보냈거나 상대방만 보낸 경우] -> 통합 카드 레이아웃
        val isPartnerOnly = isPartnerUploaded
        Card(
            modifier = Modifier.fillMaxWidth().aspectRatio(0.75f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isPartnerOnly) {
                    AsyncImage(
                        model = partnerInfo?.questionImageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().blur(30.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                }

                Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    // 상단 프로필 (상대방만 보냈을 때)
                    if (isPartnerOnly) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = partnerInfo?.profileThumbnailImageUrl ?: R.drawable.image_today_question_default,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(partnerInfo?.name ?: "상대방", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("${partnerInfo?.answeredAt ?: ""} 응답", color = Color.White.copy(0.8f), fontSize = 12.sp)
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(text = "#${data.coupleQuestionId ?: ""}.", fontSize = 14.sp, color = if(isPartnerOnly) Color.White.copy(0.7f) else Color.Gray)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = data.question, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = if(isPartnerOnly) Color.White else Color.Black)
                    }

                    Button(
                        onClick = { mediaManager.launchGallery { uri -> onUpload(uri) } },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222))
                    ) {
                        Text("사진 전송하기", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DailyActiveContent(
    keywords: List<String>,
    selectedKeyword: String,
    onKeywordSelected: (String) -> Unit,
    userImageUri: Uri?,
    partnerImageUri: Uri?,
    mediaManager: ImageMediaManager,
    onUpload: (Uri) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 키워드 리스트 (기존 코드 유지)
        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            items(keywords) { keyword ->
                val isSelected = keyword == selectedKeyword
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clickable { onKeywordSelected(keyword) },
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFFF05A5A) else Color(0xFFE0E0E0)
                    )
                ) {
                    Text(
                        text = keyword,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = if (isSelected) Color(0xFFF05A5A) else Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 나만 보냈을 때 (스크린샷 2번 스타일: 콕 찌르기 버튼)
        if (userImageUri != null && partnerImageUri == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 1. 내 사진 카드
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f) // 1:1 정방형
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    AsyncImage(
                        model = userImageUri,
                        contentDescription = "내 사진",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // 상단 프로필 오버레이
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray)
                        ) // 내 프사
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "jpg",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("PM 12:30", color = Color.White, fontSize = 10.sp)
                        }
                    }
                    // 하단 캡션 (말풍선 느낌)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 20.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("국밥 먹는중이지롱 ! 오빠는 점심 뭐먹어 ??", color = Color.White, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // 2. 하단 텍스트 및 콕 찌르기 버튼
                Text(
                    "사진을 기다리고 있다고\n상대방에게 알림을 보내보세요!",
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { /* 콕 찌르기 액션 */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    // 손가락 아이콘 (없으면 기본 아이콘 사용)
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_input_add),
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("콕 찌르기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // 그 외 (아직 안 보냄 or 둘 다 보냄) - 기존 로직 유지
            Text("오늘의 일상", fontSize = 14.sp, color = Color.Gray)
            // ... (타이틀 텍스트 등 생략) ...

            Spacer(modifier = Modifier.height(20.dp))
            PhotoResponseContainer(
                userImageUri = userImageUri,
                partnerImageUri = partnerImageUri,
                onUploadClick = { mediaManager.launchCamera { uri -> onUpload(uri) } },
                uploadButtonText = "사진 촬영하기",
                isDailyMode = true
            )
        }
    }
}

@Composable
fun DailyEmptyContent(onRegisterClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = "키워드 설정", fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "사진을 공유할 키워드를 등록하고\n일상을 특별하게 기록해보세요",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp
        )
        Spacer(modifier = Modifier.height(40.dp))
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFD9D9D9))
        )
        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = onRegisterClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222))
        ) {
            Text("키워드 등록하기", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// --- [UI 컴포넌트] 오늘의 일상 - 업로드 화면 (Case B: 마지막 사진 스타일) ---
@Composable
fun DailyUploadScreen(
    keyword: String,
    isUserUploaded: Boolean,
    viewModel: MainViewModel,
    mediaManager: ImageMediaManager,
    onReselect: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 타이틀 (예: 오늘의 '지금 이순간' 사진을 보내주세요.)
        Text(
            text = "오늘의 일상",
            fontSize = 14.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            buildAnnotatedString {
                append("오늘의 '")
                withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                    append(keyword)
                }
                append("' 사진을\n보내주세요.")
            },
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 30.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        if (isUserUploaded) {
            // 업로드 완료 시 카드 보여주기 (기존 컴포넌트 재사용)
            UploadedContentCard(
                userImageUri = viewModel.userUploadedImage,
                partnerImageUri = null, // 일상은 혼자 올릴 수도 있으므로 null 처리 고려
                userComment = "일상 기록 완료!",
                partnerComment = "",
                isUserUploaded = true,
                isPartnerUploaded = false,
                onUploadClick = {}
            )
        } else {
            // 업로드 전: 회색 박스 (마지막 사진 스타일)
            Box(
                modifier = Modifier
                    .size(240.dp) // 정사각형에 가까운 둥근 박스
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFD9D9D9)), // 진한 회색
                contentAlignment = Alignment.Center
            ) {
                // 비어있는 상태
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 검정색 버튼
            Button(
                onClick = {
                    mediaManager.launchCamera { uri ->
                        viewModel.userUploadedImage = uri
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222))
            ) {
                Text(
                    text = "사진 전송하기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 키워드 다시 고르기 (선택 사항)
            TextButton(onClick = onReselect) {
                Text(
                    "키워드 다시 선택하기",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }
}

@Composable
fun UploadedContentCard(
    userImageUri: Uri?,
    partnerImageUri: Uri?,
    userComment: String,
    partnerComment: String,
    isUserUploaded: Boolean,
    isPartnerUploaded: Boolean,
    onUploadClick: () -> Unit
) {/*
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            // 1. 둘 다 업로드했을 때: 2분할 레이아웃
            isUserUploaded && isPartnerUploaded -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp)
                        .clip(RoundedCornerShape(32.dp))
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        ImageSection(
                            partnerImageUri,
                            "쏘피",
                            "PM 12:30",
                            partnerComment,
                            true,
                            Modifier.weight(1f)
                        )
                        ImageSection(
                            userImageUri,
                            "jpg",
                            "PM 12:30",
                            userComment,
                            true,
                            Modifier.weight(1f)
                        )
                    }
                }
            }

            // 2. 나만 업로드했을 때: 내 사진 전체 + 상대 대기 문구
            isUserUploaded && !isPartnerUploaded -> {
                // 상단 대기 안내 영역
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFE0E0E0))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "jpg님이 쏘피님의 사진을\n기다리고 있어요!",
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { *//* 콕 찌르기 등 *//* },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("콕 찌르기 →", color = Color.White) }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 내 사진 단독 표시
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(32.dp))
                ) {
                    ImageSection(
                        userImageUri,
                        "jpg",
                        "PM 12:30",
                        userComment,
                        true,
                        Modifier.fillMaxSize()
                    )
                }
            }

            // 3. 상대만 업로드했을 때: 버튼 먼저 + 블러 처리된 상대 사진
            !isUserUploaded && isPartnerUploaded -> {
                Button(
                    onClick = onUploadClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) { Text("앨범으로 이동하기 →", color = Color.White) }

                Spacer(modifier = Modifier.height(32.dp))

                // 블러 처리된 상대방 사진 영역
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(32.dp))
                ) {
                    // 실제 구현 시에는 이미지를 가져와서 Blur 처리를 해야 함
                    ImageSection(
                        partnerImageUri,
                        "jpg",
                        "1시간 전 응답",
                        "",
                        true,
                        Modifier.fillMaxSize(),
                        isBlurred = true
                    )
                }
            }
        }
    }
*/
}

@Composable
fun ImageSection(
    imageUrl: String?,
    profileUrl: String?,
    name: String,
    time: String,
    modifier: Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // 상단 오버레이 정보
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = profileUrl ?: R.drawable.image_today_question_default,
                contentDescription = null,
                modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.Gray)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(time, color = Color.White.copy(0.8f), fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun KeywordTabChip(
    text: String, isSelected: Boolean, onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = if (isSelected) Color(0xFFF05A5A) else Color.White,
        border = BorderStroke(1.dp, if (isSelected) Color.Transparent else Color(0xFFE0E0E0))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) Color.White else Color.Black
        )
    }
}

@Composable
fun ProfileImagePicker(
    uri: Uri?,
    onImageClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(160.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // 클릭 효과 제거 (이미지 내부에서 처리)
            ) { onImageClick() },
        contentAlignment = Alignment.Center
    ) {
        // 1. 메인 프로필 원형 박스
        Box(
            modifier = Modifier
                .size(150.dp)
                .background(Color(0xFFD9D9D9), CircleShape)
                .clip(CircleShape)
                .border(1.dp, Color(0xFFF0F0F0), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (uri != null) {
                // 이미지가 있을 때: 사진 표시
                AsyncImage(
                    model = uri,
                    contentDescription = "프로필 이미지",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // 이미지가 없을 때: 기본 격자 아이콘 표시
                Icon(
                    painter = painterResource(id = R.drawable.photo_grid),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(100.dp)
                )
            }
        }

        // 2. 우측 하단 카메라 추가 버튼 (이미지가 없을 때만 표시)
        if (uri == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 10.dp, end = 10.dp)
                    .size(44.dp)
                    .shadow(4.dp, CircleShape)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.photo_add),
                    contentDescription = "사진 추가",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            // 이미지가 있을 때 편집 모드라면 작은 카메라 아이콘 표시 (선택 사항)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 10.dp, end = 10.dp)
                    .size(32.dp)
                    .background(Color.White, CircleShape)
                    .border(1.dp, Color(0xFFEEEEEE), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "사진 변경",
                    modifier = Modifier.size(18.dp),
                    tint = Color.Gray
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ProfileImageBottomSheet(
    showSheet: Boolean,
    onDismiss: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFE0E0E0)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp, start = 20.dp, end = 20.dp)
            ) {
                Text(
                    "프로필 사진 설정",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                ListItem(
                    headlineContent = { Text("사진 촬영") },
                    leadingContent = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                    modifier = Modifier.clickable { onCameraClick() }
                )

                ListItem(
                    headlineContent = { Text("앨범에서 가져오기") },
                    leadingContent = {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable { onGalleryClick() }
                )
            }
        }
    }
}


@Composable
fun PokeSuccessDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("확인", color = Color.Red, fontWeight = FontWeight.Bold) // 이미지상 빨간색 텍스트
            }
        },
        title = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("콕 찌르기 완료!", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "상대방의 사진이 도착하면\n알림을 보내드릴게요.",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

// 프리뷰
@androidx.compose.ui.tooling.preview.Preview
@Composable
fun PreviewApp() {
    AppNavigation()
}