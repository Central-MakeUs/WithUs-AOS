package com.widthus.app.screen

import CalendarHomeScreen
import ConnectCompleteScreen
import ConnectConfirmScreen
import ConnectionPendingScreen
import OnboardingConnectScreen
import DayUsScreen
import EnterCodeScreen
import HomeScreen
import TestHomeScreen
import InviteScreen
import KeywordSelectionScreen
import NotificationTimeScreen
import PhotoFlowScreen
import StepInputScreen
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Picture
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kakao.sdk.common.util.Utility
import com.kakao.sdk.user.UserApiClient
import com.widthus.app.viewmodel.MainViewModel
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Gallery : Screen("gallery")
    object Login : Screen("login")
    object StepInput : Screen("step_input")
    object OnboardingConnect : Screen("onboarding_connect")
    object ConnectionPending : Screen("connection_pending")
    object RandomQuestionTime : Screen("random_question_time")
    object ConnectionLastSetting : Screen("connection_last_setting")
    object ConnectConfirm : Screen("connect_confirm")
    object ConnectComplete : Screen("connect_complete")
    object KeywordSelect : Screen("keyword_select")
    object Invite : Screen("invite")
    object EnterCode : Screen("enter_code")
    object Home : Screen("home")
    object PhotoFlow : Screen("photo_flow")
    object Calendar : Screen("calendar")
    object DayUs : Screen("dayus")
}

// ==========================================
// 2. Navigation
// ==========================================
@Composable
fun AppNavigation(viewModel: MainViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val schedules = viewModel.dummySchedules
    val memories = viewModel.dummyMemories
    var currentRoute by remember { mutableStateOf(BottomNavItem.Home.route) }

    NavHost(
        navController = navController,
//        startDestination = Screen.Onboarding.route
//        startDestination = Screen.Home.route
        startDestination = Screen.Gallery.route
//        startDestination = Screen.PhotoFlow.route
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                viewModel = viewModel,
                onFinish = {
                    navController.navigate(Screen.Login.route)
                }
            )
        }

        composable(Screen.Gallery.route) {
            MemoryScreen()
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onKakaoLogin = {
                    UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                        if (error != null) {
                            Log.e("TAG", "로그인 실패", error)
                            val keyHash = Utility.getKeyHash(context)
                            Log.e("KeyHash", "현재 내 기기의 키 해시: $keyHash")
                        } else if (token != null) {
                            Log.i("TAG", "로그인 성공 ${token.accessToken}")
                            coroutineScope.launch {
                                val isSuccess = viewModel.handleKakaoLogin(token.accessToken)
                                if (isSuccess) {
                                    navController.navigate(Screen.StepInput.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                } else {
                                    Toast.makeText(context, "서버 로그인 실패", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                },
                onGoogleLogin = {
                    navController.navigate(Screen.StepInput.route)
                }
            )
        }

        composable(Screen.StepInput.route) {
            StepInputScreen(
                viewModel = viewModel,
                onAllFinish = { navController.navigate(Screen.OnboardingConnect.route) }
            )
        }

        composable(Screen.OnboardingConnect.route) {
            OnboardingConnectScreen(
                viewModel = viewModel,
                onInviteClick = { navController.navigate(Screen.Invite.route) },
                onEnterCodeClick = { navController.navigate(Screen.EnterCode.route) },
                onCloseClick = { navController.navigate(Screen.ConnectionPending.route) }
            )
        }

        composable(Screen.ConnectionPending.route) {
            ConnectionPendingScreen(
                viewModel = viewModel,
                title = "앗!\n아직 커플 연결이 되지 않았어요",
                body = "연결을 완료하고 \n사진으로 일상을 공유해보세요!",
                buttonText = "연결하러 가기",
                onConnectClick = {
                    navController.navigate(Screen.OnboardingConnect.route)
                },
                bottomBar = {}
            )
        }

        composable(Screen.RandomQuestionTime.route) {
            NotificationTimeScreen(
                onBackClick = {},
                onFinish = { timeString ->
                    Log.d("SETTING", "선택된 시간: $timeString")
                }
            )
        }

        composable(Screen.ConnectionLastSetting.route) {
            ConnectionPendingScreen(
                viewModel = viewModel,
                title = "기록을 남기기 위한 \n마지막 설정이 남아있어요",
                body = "랜덤 질문 알림 시간과 \n키워드 설정을 완료해주세요.",
                buttonText = "   설정하러 가기",
                onConnectClick = { /* 설정 페이지 이동 로직 */ },
                bottomBar = {
                    MainBottomNavigationBar(
                        currentRoute = currentRoute,
                        onItemSelected = { selectedItem ->
                            currentRoute = selectedItem.route
                        }
                    )
                }
            )
        }

        composable(Screen.ConnectConfirm.route) {
            ConnectConfirmScreen(
                viewModel = viewModel,
                onConfirmClick = {
                    navController.navigate(Screen.ConnectComplete.route)
                },
                onLaterClick = {},
            )
        }

        composable(Screen.ConnectComplete.route) {
            ConnectCompleteScreen(
                viewModel = viewModel,
                onStartClick = {},
            )
        }

        composable(Screen.KeywordSelect.route) {
            KeywordSelectionScreen(
                onBackClick = {},
                onNextClick = { strings ->
                    Log.d("TAG", "selected items : $strings")
                }
            )
        }

        composable(Screen.Invite.route) {
            InviteScreen(
                onBack = {
                    if (viewModel.nickname.isNotEmpty()) {
                        navController.navigate(Screen.OnboardingConnect.route)
                    }
                }
            )
        }

        composable(Screen.EnterCode.route) {
            EnterCodeScreen(
                onBack = {
                    if (viewModel.nickname.isNotEmpty()) {
                        navController.navigate(Screen.OnboardingConnect.route)
                    }
                },
                onConnect = {
                    navController.navigate(Screen.ConnectConfirm.route)
                }
            )
        }

        composable(Screen.Home.route) {
            MainScreen(viewModel)
        }

        composable(Screen.PhotoFlow.route) {
            PhotoFlowScreen()
        }

        composable(Screen.Calendar.route) {
            CalendarHomeScreen(
                nickname = viewModel.nickname,
                memories = memories,
                onNavigateToDayUs = { navController.navigate(Screen.DayUs.route) }
            )
        }

        composable(Screen.DayUs.route) {
            DayUsScreen()
        }
    }
}

// MainScreen.kt

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    // 1. 네비게이션 상태
    var currentRoute by remember { mutableStateOf(BottomNavItem.Home.route) }

    // 2. 에디터(네컷 만들기) 화면 표시 여부 - true면 바텀바 숨김
    var isEditorOpen by remember { mutableStateOf(false) }

    // 3. 미디어 매니저 (최상위 공유)
    val mediaManager = rememberImageMediaManager()

    // 4. 저장된 네컷 리스트 (실제로는 ViewModel이나 Room DB에서 관리 권장)
    // 테스트를 위해 여기서 state로 관리합니다.
    val savedFourCuts = remember { mutableStateListOf<Uri>() }

    // 에디터가 열려있다면 -> 전체 화면으로 에디터 표시 (바텀바 없음)
    if (isEditorOpen) {
        FourCutScreen(
            mediaManager = mediaManager,
            onClose = { isEditorOpen = false }, // X 버튼이나 뒤로가기 시 닫기
            onSaveComplete = { uri ->
                savedFourCuts.add(0, uri) // 리스트 맨 앞에 추가
                isEditorOpen = false // 에디터 닫고 갤러리로 복귀
            }
        )
    } else {
        // 에디터가 닫혀있다면 -> 일반적인 스캐폴드 화면 (바텀바 있음)
        Scaffold(
            bottomBar = {
                MainBottomNavigationBar(
                    currentRoute = currentRoute,
                    onItemSelected = { item -> currentRoute = item.route }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (currentRoute) {
                    BottomNavItem.Home.route -> {
                        HomeScreen(
                            viewModel = viewModel,
                            mediaManager = mediaManager,
                            keywords = listOf("음식", "여행", "일상"),
                            notificationTime = "18:00"
                        )
                    }
                    BottomNavItem.FourCut.route -> {
                        // 새로 만든 갤러리 화면 연결
                        FourCutGalleryScreen(
                             savedImages = savedFourCuts,
                            onCreateClick = { isEditorOpen = true }, // 만들기 버튼 클릭
                            onDeleteRequest = { urisToDelete ->
                                savedFourCuts.removeAll(urisToDelete)
                            }
                        )
                    }
                    BottomNavItem.Memory.route -> {
                        Text("추억 화면 준비중", modifier = Modifier.align(Alignment.Center))
                    }
                    BottomNavItem.My.route -> {
                        Text("마이페이지 준비중", modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
    }
}