import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.kakao.sdk.share.ShareClient
import com.kakao.sdk.template.model.Button
import com.kakao.sdk.template.model.Content
import com.kakao.sdk.template.model.FeedTemplate
import com.kakao.sdk.template.model.Link
import com.widthus.app.model.CalendarDay
import com.widthus.app.model.MemoryItem
import com.widthus.app.model.ScheduleItem
import com.widthus.app.screen.BackButton
import com.widthus.app.screen.ImageMediaManager
import com.widthus.app.viewmodel.AuthViewModel
import com.widthus.app.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.collections.getOrNull
import com.withus.app.R
import org.withus.app.debug
import org.withus.app.model.CoupleQuestionData
import org.withus.app.model.JoinCouplePreviewData
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.Dp
import org.withus.app.model.UserAnswerInfo

enum class QuestionState {
    EMPTY,          // 둘 다 안 올림
    PARTNER_ONLY,   // 상대만 올림 (나에게는 잠금 상태)
    ME_ONLY,        // 나만 올림 (상대 기다리는 중)
    BOTH            // 둘 다 올림 (완성!)
}

object MainTab {
    const val TODAY_QUESTION = "오늘의 질문"
    const val TODAY_DAILY = "오늘의 일상"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun StepInputScreen(
    viewModel: AuthViewModel, mediaManager: ImageMediaManager,
    onAllFinish: () -> Unit
) {
    // 이제 단계는 1(닉네임)과 4(프로필)만 사용합니다.
    var currentStep by remember { mutableStateOf(1) }
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current

    val nickname = viewModel.currentUserInfo.nickname.text
    // 에러 상태 체크
    val isNicknameError =
        nickname.isNotEmpty() && (nickname.length !in 2..8)
    // 생일 에러: 입력이 시작되었으나 8자가 아닐 때
    val isBirthdayError = viewModel.birthdayValue.toString()
        .isNotEmpty() && viewModel.birthdayValue.toString().length < 8

    val raw = viewModel.birthdayValue.text
    val digits = raw.filter { it.isDigit() }

    debug("viewModel.birthdayValue.text='${raw}', digits='${digits}', textLen=${raw.length}, digitLen=${digits.length}, selection=${viewModel.birthdayValue.selection}")
    // 버튼 활성화 유효성 검사
    val currentValid = when (currentStep) {
        1 -> nickname.length in 2..8
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 8.dp)
                ) {
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
                nickname
            } else {
                viewModel.birthdayValue.toString() // 명시적으로 String 변환
            }

            // 2. 입력 영역 (닉네임 & 생일)
            if (currentStep == 1 || currentStep == 2) {
                val textValue = when (currentStep) {
                    1 -> viewModel.currentUserInfo.nickname // 뷰모델 상태 객체를 그대로 사용
                    2 -> viewModel.birthdayValue
                    else -> TextFieldValue("")
                }

                OutlinedTextField(
                    value = textValue,
                    onValueChange = { newValue ->
                        if (currentStep == 1) {
                            // TextFieldValue를 통째로 넘겨야 조합 상태가 유지됩니다.
                            viewModel.updateNickname(newValue)
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
                ProfileImagePicker(viewModel.currentUserInfo.selectedLocalUri) { showSheet = true }
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
                            showSheet = false
                            viewModel.updateProfileUrl(it)
                        }
                    })
                    ListItem(headlineContent = { Text("앨범에서 가져오기") }, leadingContent = {
                        Icon(
                            Icons.Default.DateRange, contentDescription = null
                        )
                    }, modifier = Modifier.clickable {
                        mediaManager.launchGallery {
                            showSheet = false
                            viewModel.updateProfileUrl(it)
                        }
                    })
                }
            }
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
    nickname: String,
    onInviteClick: () -> Unit,
    onEnterCodeClick: () -> Unit,
    onCloseClick: () -> Unit,
    topBar: @Composable () -> Unit,
    title: String = "${nickname}님, 가입을 축하드려요!",
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
fun CoupleConnectionLayout(
    title: String,
    subtitle: String,
    @DrawableRes imageRes: Int,
    imageHeight: Dp = 200.dp,
    primaryButtonText: String,
    onPrimaryClick: () -> Unit,
    secondaryButton: @Composable (() -> Unit)? = null // '다음에 할래요' 같은 선택적 버튼
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(bottom = 20.dp), // 바닥 면 여유
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. 상단 여백 (콘텐츠를 중앙으로 밀어줌)
        Spacer(modifier = Modifier.weight(1f))

        // 2. 콘텐츠 영역 (텍스트 + 이미지)
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = subtitle,
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight),
            contentScale = ContentScale.Fit
        )

        // 3. 하단 여백 (콘텐츠와 버튼 사이 균형)
        Spacer(modifier = Modifier.weight(1.2f))

        // 4. 버튼 영역
        Button(
            onClick = onPrimaryClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(primaryButtonText, color = Color.White, fontWeight = FontWeight.Bold)
        }

        // 선택적 보조 버튼 (있을 때만 표시)
        if (secondaryButton != null) {
            Spacer(modifier = Modifier.height(12.dp))
            secondaryButton()
        }
    }
}

@Composable
fun ConnectConfirmScreen(
    previewData: JoinCouplePreviewData?,
    onConfirmClick: (String) -> Unit,
    onLaterClick: () -> Unit,
) {
    CoupleConnectionLayout(
        title = "${previewData?.senderName}님이\n${previewData?.receiverName}님을 초대했어요!",
        subtitle = "초대를 수락하면, 두 사람의 기록이 이어져요",
        imageRes = R.drawable.image_connect_noti, // 연결 중 이미지
        imageHeight = 160.dp,
        primaryButtonText = "초대 수락하기",
        onPrimaryClick = { previewData?.let { onConfirmClick(it.inviteCode) } },
        secondaryButton = {
            TextButton(onClick = onLaterClick) {
                Text(
                    "다음에 할래요",
                    color = Color.Gray,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    )
}

@Composable
fun ConnectCompleteScreen(
    onStartClick: () -> Unit,
) {
    CoupleConnectionLayout(
        title = "커플 연결 완료!",
        subtitle = "둘만의 사진 기록을 시작해 보세요",
        imageRes = R.drawable.image_connect_complete, // 하트 있는 완성 이미지
        imageHeight = 130.dp,
        primaryButtonText = "시작하기",
        onPrimaryClick = onStartClick
    )
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
) {
    // 기본 키워드 리스트 (가변 리스트로 선언하여 추가 가능하게 함)

    val context = LocalContext.current

    val defaultKeywords by viewModel.defaultKeywords.collectAsState()
    val editKeywords by viewModel.editKeywords.collectAsState()

    val isKeywordInit = !viewModel.coupleKeyword.collectAsState().value.isEmpty()

    val keywordContents = remember(isKeywordInit, defaultKeywords, editKeywords) {
        if (isKeywordInit) {
            editKeywords.map { it.content }
        } else {
            defaultKeywords.map { it.content }
        }
    }

    LaunchedEffect(Unit) {
        if (isKeywordInit) {
            viewModel.loadEditableKeywords()
        } else {
            viewModel.loadDefaultKeywords()
        }
    }

    val savedCoupleKeywords by viewModel.coupleKeyword.collectAsState()
    var selectedKeywords by remember { mutableStateOf(setOf<String>()) }
    var showAddSheet by remember { mutableStateOf(false) }

    val isNextEnabled = selectedKeywords.size in 1..3

    LaunchedEffect(savedCoupleKeywords) {
        if (savedCoupleKeywords.isNotEmpty()) {
            selectedKeywords = savedCoupleKeywords.map { it.content }.toSet()
        }
    }

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
                keywordContents.forEach { keyword ->
                    val keywordContent = keyword
                    val isSelected = selectedKeywords.contains(keywordContent)

                    KeywordChip(
                        text = keywordContent,
                        isSelected = isSelected,
                        onClick = {
                            selectedKeywords = if (isSelected) {
                                // 이미 선택된 경우 제거
                                selectedKeywords - keywordContent
                            } else {
                                // 선택되지 않은 경우: 3개 미만일 때만 추가 허용
                                if (selectedKeywords.size < 3) {
                                    selectedKeywords + keywordContent
                                } else {
                                    // 3개를 이미 선택한 경우 (선택사항: 토스트 메시지 띄우기)
                                    // Toast.makeText(context, "최대 3개까지 선택 가능합니다.", Toast.LENGTH_SHORT).show()
                                    selectedKeywords // 상태 유지
                                }
                            }
                        }
                    )
                }

                KeywordChip(
                    text = "+ 직접 추가",
                    isSelected = false,
                    onClick = {
                        // 직접 추가할 때도 이미 3개를 채웠는지 확인
                        if (selectedKeywords.size < 3) {
                            showAddSheet = true
                        } else {
                            Toast.makeText(context, "최대 3개까지 선택 가능합니다.", Toast.LENGTH_SHORT).show()
                        }
                    },

                    isAddButton = true
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            WithUsButton(
                text = if (isKeywordInit) "수정하기" else "다음",
                enabled = isNextEnabled, // 2. 여기 조건 적용
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
            AddTextBottomSheet(
                title = "새로운 키워드 추가",
                text = "",
                placeholderText = "키워드를 입력해주세요.",
                onDismissRequest = { showAddSheet = false },
                onKeywordAdded = { newKeyword ->
                    // 5. ViewModel을 통해 UI 리스트 업데이트
                    viewModel.addCustomKeywordToDisplay(newKeyword)
                    // 추가된 키워드 바로 선택 상태로
                    selectedKeywords = selectedKeywords + newKeyword
                },
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
fun AddTextBottomSheet(
    title: String, placeholderText: String,
    onDismissRequest: () -> Unit, onKeywordAdded: (String) -> Unit, text: String
) {
    var text by remember { mutableStateOf(text) }
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
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // 입력 필드 (이미지 스타일)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = {
                    Text(
                        text = placeholderText,
                        color = Color.LightGray,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
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
    onNavigateToKeywordSelect: () -> Unit
) {
    val mainTabs = listOf(MainTab.TODAY_QUESTION, MainTab.TODAY_DAILY)
    val selectedMainTab by viewModel.selectedMainTab.collectAsState()
    val dailyQuestionData = viewModel.questionData

    // 2. ViewModel의 StateFlow 구독
    val keywords by viewModel.coupleKeyword.collectAsState()
    val selectedKeywordId by viewModel.selectedKeywordId.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchTodayQuestion()
        viewModel.getCoupleKeyword()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("WITHUS", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {

            // 1. 메인 탭 (질문 vs 일상) UI 구현 (기존 코드 유지)
            Row(modifier = Modifier.fillMaxWidth()) {
                mainTabs.forEach { tabTitle ->
                    val isSelected = selectedMainTab == tabTitle
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.updateMainTab(tabTitle)},
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
                if (selectedMainTab == MainTab.TODAY_QUESTION) {
                    Spacer(modifier = Modifier.height(30.dp))
                    TodayQuestionContentLegacy(
                        selectedTab = MainTab.TODAY_QUESTION, // 탭 정보 전달
                        data = dailyQuestionData, // 질문용 데이터
                        onUpload = { uri -> viewModel.uploadTodayQuestionImage(uri) }, // 질문용 업로드
                        onPoke = { viewModel.pokePartner() },
                        showPokeDialog = viewModel.showPokeSuccessDialog,
                        onDismissPokeDialog = { viewModel.dismissPokeDialog() },
                        mediaManager = mediaManager,
                    )
                } else {
                    if (keywords.isEmpty()) {
                        Spacer(modifier = Modifier.height(30.dp))
                        DailyEmptyContent(onRegisterClick = onNavigateToKeywordSelect)
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            items(keywords) { keywordInfo ->
                                // ID 비교로 선택 여부 판단
                                debug("viewModel.selectedKeywordId : ${viewModel.selectedKeywordId}")
                                val isSelected =
                                    selectedKeywordId == keywordInfo.keywordId.toLong()

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
                                        modifier = Modifier.padding(
                                            horizontal = 16.dp,
                                            vertical = 8.dp
                                        ),
                                        color = if (isSelected) Color(0xFFF05A5A) else Color.Gray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(30.dp))

                        // 공통 컨텐츠 영역 호출
                        TodayQuestionContentLegacy(
                            selectedTab = MainTab.TODAY_DAILY, // 탭 정보 전달
                            data = viewModel.keywordDailyData, // API 조회 결과
                            onUpload = { uri -> viewModel.uploadDailyImage(uri) }, // 현재 선택된 ID로 업로드
                            onPoke = { viewModel.pokePartner() },
                            showPokeDialog = viewModel.showPokeSuccessDialog,
                            onDismissPokeDialog = { viewModel.dismissPokeDialog() },
                            mediaManager = mediaManager,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
        ) {
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
fun QuestionHeader(question: String, title: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = question,
            fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ComparisonLayout(
    myInfo: UserAnswerInfo?,
    partnerInfo: UserAnswerInfo?,
    isPartnerUploaded: Boolean,
    onPoke: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFFF5F5F5))
    ) {
        // 내 영역
        ImageSection(
            imageUrl = myInfo?.questionImageUrl,
            profileUrl = myInfo?.profileThumbnailImageUrl,
            name = myInfo?.name ?: "나",
            time = myInfo?.answeredAt ?: "방금 전",
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 상대 영역
        if (isPartnerUploaded) {
            ImageSection(
                imageUrl = partnerInfo?.questionImageUrl,
                profileUrl = partnerInfo?.profileThumbnailImageUrl,
                name = partnerInfo?.name ?: "상대방",
                time = partnerInfo?.answeredAt ?: "",
                modifier = Modifier.weight(1f)
            )
        } else {
            // 콕 찌르기 영역
            PokePlaceholder(onPoke = onPoke, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun PokePlaceholder(
    onPoke: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "사진을 기다리고 있다고\n상대방에게 알림을 보내보세요!",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onPoke,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(
                        0xFF222222
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    painterResource(id = android.R.drawable.ic_input_add),
                    null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("콕 찌르기", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TodayQuestionContentLegacy(
    data: CoupleQuestionData?, // API에서 받아온 데이터 (질문 or 일상)
    onUpload: (Uri) -> Unit,    // 탭별 업로드 로직 주입 (질문용 or 일상용)
    onPoke: () -> Unit,        // 콕 찌르기 로직 주입
    showPokeDialog: Boolean,    // 콕 찌르기 다이얼로그 상태
    onDismissPokeDialog: () -> Unit,
    mediaManager: ImageMediaManager,
    selectedTab: String
) {
    debug("TodayQuestionContent ! data : $data")
    var showPhotoFlow by remember { mutableStateOf(false) }

    if (data == null) return // 로딩 중 처리

    // API 응답 기반 상태 정의
    val myInfo = data.myInfo
    val partnerInfo = data.partnerInfo
    var showSheet by remember { mutableStateOf(false) }

    // 사진 업로드 여부 판단
    val isUserUploaded = myInfo?.questionImageUrl != null
    val isPartnerUploaded = partnerInfo?.questionImageUrl != null
    debug("isUserUploaded : $isUserUploaded, isPartnerUploaded : $isPartnerUploaded")
    // 콕 찌르기 다이얼로그
    if (showPokeDialog) {
        PokeSuccessDialog(onDismiss = onDismissPokeDialog)
    }

    if (showPhotoFlow) {
        PhotoFlowDialog(
            onFinish = { uri ->
                showPhotoFlow = false
                debug("onUpload !")
                onUpload(uri) // 최종 결과물 처리
            },
            onCancel = { showPhotoFlow = false }
        )
    }

    if (isUserUploaded) {
        // [나만 보냈거나 둘 다 보낸 경우] -> 2분할 레이아웃
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFFF5F5F5))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // [위 영역] 나
                    ImageSectionLegacy(
                        imageUrl = myInfo?.questionImageUrl,
                        profileUrl = myInfo?.profileThumbnailImageUrl,
                        name = myInfo?.name ?: "나",
                        time = myInfo?.answeredAt ?: "방금 전",
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // [아래 영역] 상대방
                    if (isPartnerUploaded) {
                        ImageSectionLegacy(
                            imageUrl = partnerInfo?.questionImageUrl,
                            profileUrl = partnerInfo?.profileThumbnailImageUrl,
                            name = partnerInfo?.name ?: "상대방",
                            time = partnerInfo?.answeredAt ?: "",
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // 상대방 대기 및 콕 찌르기 영역
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "사진을 기다리고 있다고\n상대방에게 알림을 보내보세요!",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = onPoke,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(
                                            0xFF222222
                                        )
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        painterResource(id = android.R.drawable.ic_input_add),
                                        null,
                                        tint = Color.White
                                    )
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
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f), // 이미지와 유사한 비율 유지
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 1. 배경 설정 (상대방만 보냈을 경우 블러 이미지 배경)
                if (isPartnerOnly) {
                    AsyncImage(
                        model = partnerInfo?.questionImageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(30.dp), // 강한 블러 효과
                        contentScale = ContentScale.Crop
                    )
                    // 이미지 위를 어둡게 덮어서 글씨 가독성 확보
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                    )
                }

                // 2. 카드 내부 콘텐츠
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // [상단 영역] 상대방 정보 (상대방만 보냈을 때 표시)
                    if (isPartnerOnly) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = partnerInfo?.profileThumbnailImageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = partnerInfo?.name ?: "상대방",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "${partnerInfo?.answeredAt ?: "방금 전"} 응답",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    val title = if (selectedTab == MainTab.TODAY_QUESTION) "#${data.coupleQuestionId}" else "오늘의 일상"
                    debug("title : $title, selectedTab : $selectedTab")

                    // [중앙 영역] 질문 정보 및 일러스트
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 16.sp,
                            color = if (isPartnerOnly) Color.White.copy(alpha = 0.7f) else Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = data.question,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = if (isPartnerOnly) Color.White else Color.Black,
                            lineHeight = 30.sp
                        )

                        // 아무도 안 보냈을 때만 일러스트 표시 (image_f4b25d.png 참고)
                        if (!isPartnerOnly) {
                            Spacer(modifier = Modifier.height(30.dp))
                            AsyncImage(
                                model = R.drawable.image_today_question_default, // 강아지/고양이 일러스트
                                contentDescription = null,
                                modifier = Modifier.size(160.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    // [하단 영역] 버튼 및 안내 문구
                    Button(
                        onClick = { showSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF222222)
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "사진 전송하기",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isPartnerOnly) "내 사진을 공유하고 상대의 사진을 확인해보세요."
                        else "먼저 오늘의 질문에 답해보세요.",
                        color = if (isPartnerOnly) Color.White.copy(alpha = 0.6f) else Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    ImageBottomSheet(
                        showSheet = showSheet,
                        onDismiss = { showSheet = false },
                        onCameraClick = {
                            showSheet = false
                            showPhotoFlow = true // 다이얼로그 실행
                        },
                        onGalleryClick = { mediaManager.launchGallery {
                            onUpload(it)
                        } }
                    )
                }
            }
        }
    }
}

@Composable
fun ImageSectionLegacy(
    imageUrl: String?,
    profileUrl: String?,
    name: String,
    time: String,
    modifier: Modifier
) {
    debug("imageUrl : $imageUrl")
    Box(modifier = modifier.fillMaxWidth()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center
        )
        // 상단 오버레이 정보
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = profileUrl ?: R.drawable.image_today_question_default,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
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
fun LockedCardLayout(
    uiState: QuestionState,
    data: CoupleQuestionData?,
    title: String,
    onUploadClick: () -> Unit
) {/*
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(0.75f),
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 상대방만 보냈을 때 배경에 블러 처리된 상대 사진 노출
            if (uiState == QuestionState.PARTNER_ONLY) {
                AsyncImage(
                    model = data?.partnerInfo?.questionImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().blur(30.dp),
                    contentScale = ContentScale.Crop
                )
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
            }

            // 중앙 콘텐츠 (일러스트 혹은 문구)
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                QuestionHeader(question = data!!.question, title)
                Spacer(modifier = Modifier.height(24.dp))

                if (uiState == QuestionState.EMPTY) {
                    Image(
                        painter = painterResource(id = R.drawable.image_today_question_default),
                        contentDescription = null,
                        modifier = Modifier.size(160.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { showSheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF222222)
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "사진 전송하기",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Text(
                    text = if (uiState == QuestionState.PARTNER_ONLY) "내 사진을 공유하고 상대의 사진을 확인해보세요."
                    else "먼저 오늘의 질문에 답해보세요.",
                    color = if (uiState == QuestionState.PARTNER_ONLY) Color.White.copy(alpha = 0.6f) else Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

            }
        }
    }

    ImageBottomSheet(
        showSheet = showSheet,
        onDismiss = { showSheet = false },
        onCameraClick = { showSheet = false; showPhotoFlow = true },
        onGalleryClick = { mediaManager.launchGallery { onUpload(it) } }
    )
*/}


@Composable
fun DailyEmptyContent(onRegisterClick: () -> Unit) {
    // 1. 전체 배경 (회색)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F7)) // 배경색 (이미지와 유사한 연회색)
            .padding(horizontal = 20.dp), // 좌우 여백
        contentAlignment = Alignment.Center // 화면 중앙 정렬
    ) {
        // 2. 카드 영역 (하얀색 박스)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(), // 내용은 내용물만큼만 높이 차지
            shape = RoundedCornerShape(24.dp), // 둥근 모서리 (이미지와 유사하게)
            color = Color.White,
            shadowElevation = 4.dp // 살짝 그림자 추가 (선택사항, 입체감)
        ) {
            // 3. 카드 내부 내용물
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 40.dp, horizontal = 24.dp) // 카드 내부 여백
            ) {
                Text(
                    text = "키워드 설정",
                    fontSize = 14.sp,
                    color = Color(0xFF888888), // 조금 더 진한 회색
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "사진을 공유할 키워드를 등록하고\n일상을 특별하게 기록해보세요",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp, // 줄 간격 넉넉하게
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(40.dp))

                // 이미지 (크기 조절 및 비율 유지)
                AsyncImage(
                    model = R.drawable.image_keyword_setting,
                    contentDescription = "키워드 설정 일러스트",
                    modifier = Modifier
                        .size(160.dp), // 이미지 크기 고정 (적절히 조절하세요)
                    contentScale = ContentScale.Fit // 잘리지 않게 비율 유지
                )

                Spacer(modifier = Modifier.height(40.dp))

                // 버튼
                Button(
                    onClick = onRegisterClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF222222) // 진한 검정색
                    )
                ) {
                    Text(
                        text = "키워드 등록하기",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
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
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center
        )
        // 상단 오버레이 정보
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = profileUrl ?: R.drawable.image_today_question_default,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
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
fun ImageBottomSheet(
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
