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
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kakao.sdk.common.util.Utility
import com.kakao.sdk.user.UserApiClient
import com.widthus.app.viewmodel.MainViewModel
import kotlinx.coroutines.launch

// ==========================================
// 2. Navigation
// ==========================================
@Composable
fun AppNavigation(viewModel: MainViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    // 뷰모델에서 데이터 가져오기
    val schedules = viewModel.dummySchedules
    val memories = viewModel.dummyMemories
    var currentRoute by remember { mutableStateOf(BottomNavItem.Home.route) }

    // start
    NavHost(navController = navController, startDestination = "photo_flow") {
        composable("onboarding") {
            OnboardingScreen(
                viewModel = viewModel,
                onFinish = {
                    navController.navigate("login")
                }
            )
        }

        composable("login") {
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
                                    navController.navigate("step_input") {
                                        popUpTo("login") { inclusive = true } // 로그인 화면 제거
                                    }
                                } else {
                                    Toast.makeText(context, "서버 로그인 실패", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }

                },
                onGoogleLogin = {
                    navController.navigate("step_input")
                }
            )
        }

        composable("step_input") {
            StepInputScreen(
                viewModel = viewModel,
                onAllFinish = { navController.navigate("onboarding_connect") }
            )
        }

//        composable("profile_photo") {
//            DayUsUploadScreen(
//                viewModel = viewModel,
//                onBack = { if (viewModel.nickname.isNotEmpty()) navController.navigate("connect") }
//            )
//        }

        composable("onboarding_connect") {
            OnboardingConnectScreen(
                viewModel = viewModel,
                onInviteClick = {
                    navController.navigate("invite")
                },
                onEnterCodeClick = {
                    navController.navigate("enter_code")
                },
                onCloseClick = {
                    navController.navigate("connection_pending")
                }
            )
        }

        composable("connection_pending") {
            ConnectionPendingScreen(
                viewModel = viewModel,
                title = "앗!\n아직 커플 연결이 되지 않았어요",
                body = "연결을 완료하고 \n사진으로 일상을 공유해보세요!",
                buttonText = "연결하러 가기",
                onConnectClick = {
                    navController.navigate("onboarding_connect")
                },
                bottomBar = {

                }
            )
        }
        composable("random_question_time") {
            NotificationTimeScreen(
                onBackClick = {

                },
                onFinish = { timeString ->
                    // timeString 예: "08:00 PM"
                    Log.d("SETTING", "선택된 시간: $timeString")

                }
            )
        }


        composable("connection_last_setting") {
            ConnectionPendingScreen(
                viewModel = viewModel,
                title = "기록을 남기기 위한 \n" +
                        "마지막 설정이 남아있어요",
                body = "랜덤 질문 알림 시간과 \n" +
                        "키워드 설정을 완료해주세요.",
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

        composable("connect_confirm") {
            ConnectConfirmScreen(
                viewModel = viewModel,
                onConfirmClick = {
                    navController.navigate("connect_complete")
                },
                onLaterClick = {},
            )
        }

        composable("connect_complete") {
            ConnectCompleteScreen(
                viewModel = viewModel,
                onStartClick = {

                },
            )
        }

        composable("keyword_select") {
            KeywordSelectionScreen(
                onBackClick = {

                },
                onNextClick = { strings ->
                    Log.d("TAG", "selected items : $strings")
                }
            )
        }


        composable("invite") {
            InviteScreen(
                onBack = { if (viewModel.nickname.isNotEmpty()) navController.navigate("onboarding_connect") }
            )
        }

        composable("enter_code") {
            EnterCodeScreen(
                onBack = { if (viewModel.nickname.isNotEmpty()) navController.navigate("onboarding_connect") },
                onConnect = {
                    navController.navigate("connect_confirm")
                }
            )
        }

        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                bottomBar = {
                    MainBottomNavigationBar(
                        currentRoute = currentRoute,
                        onItemSelected = { selectedItem ->
                            currentRoute = selectedItem.route
                        }
                    )
                },
                keywords = listOf("오운완","테스트"),
                notificationTime = "08:00 PM"
            )
        }

        composable("photo_flow") {
            PhotoFlowScreen()
        }

//        composable("home_test") {
//            TestHomeScreen(
//                nickname = viewModel.nickname,
//                schedules = schedules, // 데이터 전달
//                memories = memories,   // 데이터 전달
//                onNavigateToCalendar = { navController.navigate("calendar") }
//            )
//        }

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
