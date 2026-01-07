package com.cmc.demoapp.screen

import CalendarHomeScreen
import ConnectMainScreen
import DayUsScreen
import EnterCodeScreen
import HomeScreen
import InviteScreen
import StepInputScreen
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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
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
import com.cmc.demoapp.screen.DayUsUploadScreen
import com.cmc.demoapp.screen.OnboardingScreen
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

    NavHost(navController = navController, startDestination = "step_input") {
        composable("onboarding") {
            OnboardingScreen(
                viewModel = viewModel,
                onFinish = { navController.navigate("step_input")
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

        composable("connect") {
            ConnectMainScreen(viewModel = viewModel,
                onInviteClick = {
                    navController.navigate("invite")
                },
                onEnterCodeClick = {
                    navController.navigate("enter_code")
                },
                onLaterClick = {},
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
