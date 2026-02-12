package com.widthus.app.screen

import ConnectCompleteScreen
import ConnectConfirmScreen
import ConnectionPendingScreen
import OnboardingConnectScreen
import DayUsScreen
import EnterCodeScreen
import HomeScreen
import InviteScreen
import KeywordSelectionScreen
import NotificationTimeScreen
import StepInputScreen
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kakao.sdk.common.util.Utility
import com.kakao.sdk.user.UserApiClient
import com.widthus.app.model.LocalPartnerNickname
import com.widthus.app.model.LocalUserNickname
import com.widthus.app.model.ProfileLoadResult
import com.widthus.app.viewmodel.AuthViewModel
import com.widthus.app.viewmodel.MainViewModel
import com.withus.app.R
import kotlinx.coroutines.launch
import org.withus.app.debug

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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(viewModel: MainViewModel = hiltViewModel(),
                  authViewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentRoute by remember { mutableStateOf(BottomNavItem.Home.route) }
    // 3. 미디어 매니저 (최상위 공유)
    val mediaManager = rememberImageMediaManager()

    var showTempLoginDialog by remember { mutableStateOf(false) }
    var tempIdInput by remember { mutableStateOf("bin1") }

    CompositionLocalProvider(LocalUserNickname provides authViewModel.currentUserInfo,
        LocalPartnerNickname provides authViewModel.partnerUserInfo
    ) {
        NavHost(
            navController = navController,
//        startDestination = Screen.Onboarding.route
        startDestination = Screen.Login.route
//            startDestination = Screen.Home.route
//        startDestination = Screen.Gallery.route
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
                GalleryScreen()
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    onKakaoLogin = {
                        UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                            if (error != null) {
                                // 에러 메시지 추출
                                val errMsg = error.message ?: error.toString()

                                // 로그는 남기되, 사용자에게는 Toast로 표시
                                Log.e("TAG", "로그인 실패: $errMsg", error)
                                Toast.makeText(context, "로그인 실패: $errMsg", Toast.LENGTH_LONG).show()

                                // 디버그용: 키 해시 출력 (원하면)
                                val keyHash = Utility.getKeyHash(context)
                                Log.d("KeyHash", "현재 기기 키 해시: $keyHash")

                                // 카카오톡이 설치되어 있지 않은 경우, 카카오 계정 로그인으로 폴백
                                // (에러 문자열에 따라 판별하거나, Throwable 타입 체크 가능)
                                val lower = errMsg.lowercase()
                                if (lower.contains("kakaotalk not installed") || lower.contains("notinstalled") ||
                                    lower.contains("notsupported") || error::class.simpleName == "ClientError"
                                ) {
                                    // 안전하게 계정 로그인 시도
                                    UserApiClient.instance.loginWithKakaoAccount(context) { accToken, accError ->
                                        if (accError != null) {
                                            val accMsg = accError.message ?: accError.toString()
                                            Log.e("TAG", "카카오계정 로그인 실패: $accMsg", accError)
                                            Toast.makeText(
                                                context,
                                                "카카오계정 로그인 실패: $accMsg",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else if (accToken != null) {
                                            Log.i("TAG", "카카오계정 로그인 성공 ${accToken.accessToken}")
                                            coroutineScope.launch {
                                                val isSuccess =
                                                    authViewModel.handleKakaoLogin(accToken.accessToken)
                                                if (isSuccess) {

                                                    when (val result =
                                                        authViewModel.getUserProfile()) {
                                                        is ProfileLoadResult.Success -> {
                                                            viewModel.navigateToNextScreenBasedOnStatus(
                                                                navController
                                                            )
                                                        }

                                                        is ProfileLoadResult.Error -> {
                                                            // 에러 메시지 토스트나 스낵바 표시
                                                            Toast.makeText(
                                                                context,
                                                                "로그인 실패: ${result.message}",
                                                                Toast.LENGTH_LONG
                                                            ).show()

                                                        }

                                                        else -> {

                                                        }
                                                    }

                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "서버 로그인 실패",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (token != null) {
                                Log.i("TAG", "로그인 성공 ${token.accessToken}")
                                coroutineScope.launch {
                                    val isSuccess =
                                        authViewModel.handleKakaoLogin(token.accessToken)
                                    if (isSuccess) {

                                        when (val result = authViewModel.getUserProfile()) {
                                            is ProfileLoadResult.Success -> {
                                                viewModel.navigateToNextScreenBasedOnStatus(
                                                    navController
                                                )
                                            }

                                            is ProfileLoadResult.Error -> {
                                                // 에러 메시지 토스트나 스낵바 표시
                                                Toast.makeText(
                                                    context,
                                                    "로그인 실패: ${result.message}",
                                                    Toast.LENGTH_LONG
                                                ).show()

                                            }

                                            else -> {}
                                        }
                                    } else {
                                        Toast.makeText(context, "서버 로그인 실패", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                }
                            }
                        }
                    },
                    onGoogleLogin = {
                        navController.navigate(Screen.StepInput.route)
                    },
                    onTempLogin = {
                        coroutineScope.launch {
                            showTempLoginDialog = true/*
                            val isSuccess = authViewModel.handleTempLogin(
                                "bin1"
                            )
                            if (isSuccess) {
                                when (val result = authViewModel.getUserProfile()) {
                                    is ProfileLoadResult.Success -> {
                                        viewModel.navigateToNextScreenBasedOnStatus(
                                            navController
                                        )
                                    }

                                    is ProfileLoadResult.Error -> {
                                        // 에러 메시지 토스트나 스낵바 표시
                                        Toast.makeText(
                                            context,
                                            "로그인 실패: ${result.message}",
                                            Toast.LENGTH_LONG
                                        ).show()

                                    }

                                    else -> {}
                                }
                            } else {
                                Toast.makeText(context, "서버 로그인 실패", Toast.LENGTH_SHORT)
                                    .show()
                            }

                        */}
                    }
                )
            }

            composable(Screen.StepInput.route) {
                StepInputScreen(
                    viewModel = authViewModel,
                    mediaManager = mediaManager,
                    onAllFinish = {
                        /*navController.navigate(Screen.OnboardingConnect.route)*/
                        coroutineScope.launch {
                            when (authViewModel.uploadProfileAndSave()) {
                                ProfileLoadResult.Success -> {
                                    viewModel.navigateToNextScreenBasedOnStatus(navController)
                                }

                                else -> {
                                    Toast.makeText(context, "업로드 실패", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                )
            }

            composable(Screen.OnboardingConnect.route) {
                OnboardingConnectScreen(
                    nickname = authViewModel.currentUserInfo.nickname.text,
                    onInviteClick = {
                        navController.navigate(Screen.Invite.route) },
                    onEnterCodeClick = { navController.navigate(Screen.EnterCode.route) },
                    onCloseClick = { navController.navigate(Screen.ConnectionPending.route) },
                    topBar = {
                        // 상단 바 영역
                        TopAppBar(
                            title = { }, // 제목은 비워둠
                            actions = {
                                // 오른쪽 버튼들 (actions)
                                IconButton(onClick = {
                                    navController.navigate(Screen.ConnectionPending.route)
                                }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_close),
                                        contentDescription = "닫기",
                                        tint = Color.Black,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }, colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.White
                            )
                        )
                    }
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
                        viewModel.completeOnboarding()
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
                    previewData = viewModel.joinPreviewData,
                    onConfirmClick = {
                        coroutineScope.launch { it
                            viewModel.joinCouple(it).onSuccess {
                                authViewModel.fetchCoupleProfile()
                                navController.navigate(Screen.ConnectComplete.route)
                            }.onFailure {

                            }
                        }
                    },
                    onLaterClick = {
                        navController.popBackStack()
                    },
                )
            }

            composable(Screen.ConnectComplete.route) {
                ConnectCompleteScreen(
                    onStartClick = {
                        navController.navigate(Screen.Home.route)
                    },
                )
            }

            composable(Screen.KeywordSelect.route) {
                KeywordSelectionScreen(
                    viewModel = viewModel,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onNextClick = {
                        navController.popBackStack()
                    },
                )
            }

            composable(Screen.Invite.route) {
                InviteScreen(
                    onBack = {
                        if (authViewModel.currentUserInfo.nickname.text.isNotEmpty()) {
                            navController.navigate(Screen.OnboardingConnect.route)
                        }
                    },
                    viewModel = viewModel
                )
            }

            composable(Screen.EnterCode.route) {
                EnterCodeScreen(
                    onBack = { navController.popBackStack() },
                    onConnect = { code, onResult -> // code: 입력한 코드, onResult: 결과 콜백
                        coroutineScope.launch {
                            viewModel.previewJoinCouple(code)
                                .onSuccess { preview ->
                                    debug("preview : $preview")
                                    // 성공 시
                                    viewModel.joinPreviewData = preview
                                    navController.navigate(Screen.ConnectConfirm.route)

                                    // 성공 콜백 호출 (에러 없음)
                                    onResult(true, null)
                                }
                                .onFailure { error ->
                                    // 실패 시
                                    // 에러 메시지 추출 (예: "자기 자신을 초대할 수 없습니다.")
                                    // error가 커스텀 Exception이라면 message 프로퍼티 사용
                                    val message = error.message ?: "초대코드를 다시 확인해주세요."

                                    // 실패 콜백 호출 (에러 메시지 전달)
                                    onResult(false, message)
                                }
                        }
                    }
                )
            }


            composable(Screen.Home.route) {
                MainScreen(viewModel, navController, authViewModel, mediaManager)
            }

//        composable(Screen.PhotoFlow.route) {
//            PhotoFlowScreen()
//        }


            composable(Screen.DayUs.route) {
                DayUsScreen()
            }
        }

        if (showTempLoginDialog) {
            AlertDialog(
                onDismissRequest = {
                    showTempLoginDialog = false
                    tempIdInput = "" // 닫을 때 초기화
                },
                title = { Text("임시 로그인") },
                text = {
                    Column {
                        Text("사용할 아이디를 입력해주세요.")
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = tempIdInput,
                            onValueChange = { tempIdInput = it },
                            placeholder = { Text("bin1") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (tempIdInput.isNotBlank()) {
                                showTempLoginDialog = false
                                // [핵심] 입력받은 아이디로 로그인 시도
                                coroutineScope.launch {
                                    val isSuccess = authViewModel.handleTempLogin(tempIdInput)
                                    if (isSuccess) {
                                        when (val result = authViewModel.getUserProfile()) {
                                            is ProfileLoadResult.Success -> {
                                                viewModel.navigateToNextScreenBasedOnStatus(navController)
                                            }
                                            is ProfileLoadResult.Error -> {
                                                Toast.makeText(context, "로그인 실패: ${result.message}", Toast.LENGTH_LONG).show()
                                            }
                                            else -> {}
                                        }
                                    } else {
                                        Toast.makeText(context, "서버 로그인 실패", Toast.LENGTH_SHORT).show()
                                    }
                                    tempIdInput = "" // 완료 후 초기화
                                }
                            }
                        }
                    ) {
                        Text("로그인")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showTempLoginDialog = false
                        tempIdInput = ""
                    }) {
                        Text("취소")
                    }
                }
            )
        }
    }
}

// MainScreen.kt

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    navController: NavHostController,
    authViewModel: AuthViewModel,
    mediaManager: ImageMediaManager
) {
    // 1. 네비게이션 상태
    val currentRoute by viewModel.currentBottomRoute.collectAsState()

    // 2. 에디터(네컷 만들기) 화면 표시 여부 - true면 바텀바 숨김
    var isEditorOpen by remember { mutableStateOf(false) }

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
                    onItemSelected = { item -> viewModel.updateRoute(item.route)}
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (currentRoute) {
                    BottomNavItem.Home.route -> {
                        HomeScreen(
                            viewModel = viewModel,
                            mediaManager = mediaManager,
                            onNavigateToKeywordSelect = {
                                navController.navigate(Screen.KeywordSelect.route)
                            }
                        )
                    }

                    BottomNavItem.Memory.route -> {
                        MemoryArchiveScreen(mainViewModel = viewModel, mediaManager = mediaManager)
                    }

                    BottomNavItem.Gallery.route -> {
                        GalleryScreen()
                    }

                    BottomNavItem.My.route -> {
                        MyScreenEntry(
                            viewModel = viewModel,
                            authViewModel = authViewModel,
                            mediaManager = mediaManager,
                            onNavigateToEditKeyword = {
                                navController.navigate(Screen.KeywordSelect.route)
                            },
                            onLogoutSuccess = {
                                navController.navigate(Screen.Login.route)
                            },
                            onWithdraw = {
                                navController.navigate(Screen.Login.route)
                            }
                        )
                    }
                }
            }
        }
    }
}