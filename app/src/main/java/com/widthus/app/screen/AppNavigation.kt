package com.widthus.app.screen

import CalendarHomeScreen
import ConnectCompleteScreen
import ConnectConfirmScreen
import ConnectionPendingScreen
import OnboardingConnectScreen
import DayUsScreen
import EnterCodeScreen
import HomeScreen
import InviteScreen
import StepInputScreen
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kakao.sdk.common.util.Utility
import com.kakao.sdk.user.UserApiClient
import com.widthus.app.DemoApplication
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

    NavHost(navController = navController, startDestination = "onboarding") {
        composable("onboarding") {
            OnboardingScreen(
                viewModel = viewModel,
                onFinish = { navController.navigate("login")
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
                        }
                        else if (token != null) {
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
            OnboardingConnectScreen(viewModel = viewModel,
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
            ConnectionPendingScreen(viewModel = viewModel,
                onConnectClick = {
                    navController.navigate("onboarding_connect")
                },
            )
        }

        composable("connect_confirm") {
            ConnectConfirmScreen(viewModel = viewModel,
                onConfirmClick = {
                    navController.navigate("connect_complete")
                },
                onLaterClick = {},
            )
        }

        composable("connect_complete") {
            ConnectCompleteScreen(viewModel = viewModel,
                onStartClick = {

                },
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
