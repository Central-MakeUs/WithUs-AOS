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
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.widthus.app.viewmodel.MainViewModel

// ==========================================
// 2. Navigation
// ==========================================
@Composable
fun AppNavigation(viewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()

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
                    navController.navigate("step_input")
                },
                onGoogleLogin = {
                    navController.navigate("step_input")
                }
            )
        }

        composable("step_input") {
            StepInputScreen(
                viewModel = viewModel,
                onAllFinish = { navController.navigate("connect") }
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
                onBack = { if (viewModel.nickname.isNotEmpty()) navController.navigate("connect") }
            )
        }

        composable("enter_code") {
            EnterCodeScreen(
                onBack = { if (viewModel.nickname.isNotEmpty()) navController.navigate("connect") },
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
