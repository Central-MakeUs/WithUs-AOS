package com.widthus.app.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// --- 라우트 정의 ---
sealed class MyRoute(val route: String) {
    object Main : MyRoute("my_main")
    object Account : MyRoute("account")
    object DeleteWarning : MyRoute("delete_warning")
    object DeleteReason : MyRoute("delete_reason")
    object Disconnect : MyRoute("disconnect")
}

@Composable
fun MyScreenEntry() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = MyRoute.Main.route) {
        // 1. 마이 메인 화면
        composable(MyRoute.Main.route) {
            MyMainScreen(
                onNavigateToAccount = { navController.navigate(MyRoute.Account.route) },
                onNavigateToDisconnect = { navController.navigate(MyRoute.Disconnect.route) }
            )
        }
        // 2. 계정 관리 화면
        composable(MyRoute.Account.route) {
            AccountManagementScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDelete = { navController.navigate(MyRoute.DeleteWarning.route) }
            )
        }
        // 3. 회원 탈퇴 (경고)
        composable(MyRoute.DeleteWarning.route) {
            DeleteAccountWarningScreen(
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(MyRoute.DeleteReason.route) }
            )
        }
        // 4. 회원 탈퇴 (사유 선택)
        composable(MyRoute.DeleteReason.route) {
            DeleteAccountReasonScreen(
                onBack = { navController.popBackStack() }, // 유지하기
                onConfirmDelete = {
                    /* 실제 탈퇴 로직 후 로그인 화면 등으로 이동 */
                }
            )
        }
        // 5. 연결 해제
        composable(MyRoute.Disconnect.route) {
            DisconnectScreen(
                onBack = { navController.popBackStack() }, // 유지하기
                onConfirmDisconnect = { /* 연결 해제 로직 */ }
            )
        }
    }
}

// =================================================================
// 1. 마이 메인 화면 (MyMainScreen)
// =================================================================
@Composable
fun MyMainScreen(
    onNavigateToAccount: () -> Unit,
    onNavigateToDisconnect: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item {
            // 헤더
            Text(
                text = "마이",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            // 프로필 섹션
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 프로필 이미지 (Placeholder)
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("jpg", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("2024년 10월 6일 가입", fontSize = 14.sp, color = Color.Gray)
                }
                OutlinedButton(
                    onClick = { /* 프로필 편집 */ },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("프로필 편집", color = Color.Gray)
                }
            }

            Divider(thickness = 8.dp, color = Color(0xFFF9F9F9))
        }

        // 설정 섹션
        item {
            SectionHeader("설정")
            MyListItem("알림")
            MyListItem("일상 키워드 관리")
            MyListItem("계정 관리", onClick = onNavigateToAccount)
            Divider(thickness = 8.dp, color = Color(0xFFF9F9F9))
        }

        // 정보 섹션
        item {
            SectionHeader("정보")
            MyListItem("커플 연결 정보", onClick = onNavigateToDisconnect)
            MyListItem("카카오 채널 문의하기")
            MyListItem("앱 리뷰 남기기")
            MyListItem("이용 약관")
            MyListItem("개인정보 처리방침")
        }
    }
}

// =================================================================
// 2. 계정 관리 화면 (AccountManagementScreen)
// =================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManagementScreen(onBack: () -> Unit, onNavigateToDelete: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("계정 관리", fontWeight = FontWeight.Bold) },
                navigationIcon = { BackButton(onBack) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            MyListItem("로그아웃")
            MyListItem("회원탈퇴", onClick = onNavigateToDelete)
        }
    }
}

// =================================================================
// 3. 회원 탈퇴 - 경고 (DeleteAccountWarningScreen)
// =================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAccountWarningScreen(onBack: () -> Unit, onNext: () -> Unit) {
    var isChecked by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("회원 탈퇴", fontWeight = FontWeight.Bold) },
                navigationIcon = { BackButton(onBack) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("정말 WITHUS를 떠나시나요?", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            // 이미지 플레이스홀더
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFEEEEEE))
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(32.dp))

            // 경고 문구
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("회원 탈퇴 전 꼭 확인해 주세요!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))

            BulletText("연결된 상대가 있는 경우,\n마이>연결 정보>연결 해제하기를 해야 탈퇴가 가능해요.")
            BulletText("탈퇴한 뒤 재가입하는 경우,\n이전 계정 데이터는 복원되지 않아요.")
            BulletText("탈퇴는 즉시 처리되며 철회할 수 없어요.")

            Spacer(modifier = Modifier.weight(1f))

            // 체크박스 영역
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                    .clickable { isChecked = !isChecked }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { isChecked = it },
                    colors = CheckboxDefaults.colors(checkedColor = Color.Black)
                )
                Text(
                    "유의사항을 모두 확인하였으며, 회원탈퇴 시 활동 내역의 소멸 및 데이터 복원 불가에 동의합니다.",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
            }
            if (!isChecked) {
                Text(
                    "유의사항에 동의하셔야 합니다.",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // 버튼
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                ) {
                    Text("유지하기")
                }
                Button(
                    onClick = onNext,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if(isChecked) Color.Black else Color.LightGray
                    ),
                    enabled = isChecked
                ) {
                    Text("다음")
                }
            }
        }
    }
}

// =================================================================
// 4. 회원 탈퇴 - 사유 (DeleteAccountReasonScreen)
// =================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAccountReasonScreen(onBack: () -> Unit, onConfirmDelete: () -> Unit) {
    val reasons = listOf(
        "앱을 자주 사용하지 않아요",
        "사용 방법이 복잡하거나 불편했어요",
        "연인과 헤어졌어요",
        "제가 필요로 하는 기능이 부족했어요",
        "기타"
    )
    var selectedReason by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("회원 탈퇴", fontWeight = FontWeight.Bold) },
                navigationIcon = { BackButton(onBack) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("떠나는 이유를 선택해 주세요", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                "서비스를 이용하면서 느낀 점을 공유해 주시면\n더 나은 서비스를 제공할 수 있도록 노력할게요",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            reasons.forEach { reason ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
                        .selectable(
                            selected = (selectedReason == reason),
                            onClick = { selectedReason = reason },
                            role = Role.RadioButton
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (selectedReason == reason),
                        onClick = null,
                        colors = RadioButtonDefaults.colors(selectedColor = Color.Black)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(reason, fontSize = 15.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                ) {
                    Text("유지하기")
                }
                Button(
                    onClick = onConfirmDelete,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222))
                ) {
                    Text("탈퇴하기")
                }
            }
        }
    }
}

// =================================================================
// 5. 연결 해제 (DisconnectScreen)
// =================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisconnectScreen(onBack: () -> Unit, onConfirmDisconnect: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("연결 해제", fontWeight = FontWeight.Bold) },
                navigationIcon = { BackButton(onBack) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                "쏘피님과 연결을 해제할까요?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(24.dp))

            // 이미지 플레이스홀더
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFEEEEEE))
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(32.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("연결 해제 전 꼭 확인해 주세요", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))

            BulletText("한 사람만 연결을 해제한 후 동일한 상대방과 다시 연결하는 경우, 데이터를 복구할 수 있어요.")
            BulletText("상대방도 연결을 해제하는 경우 데이터 복구가 불가능해요.")
            BulletText("연결을 해제한 후 새로운 사용자와 연결하는 경우, 데이터 복구가 불가능해요")

            Spacer(modifier = Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                ) {
                    Text("유지하기")
                }
                Button(
                    onClick = onConfirmDisconnect,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222))
                ) {
                    Text("연결 해제하기")
                }
            }
        }
    }
}

// =================================================================
// 🧩 공통 컴포넌트
// =================================================================

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun MyListItem(title: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 16.sp)
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color.Gray
        )
    }
}

@Composable
fun BackButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, // 또는 ArrowBackIosNew
            contentDescription = "뒤로가기",
            tint = Color.Black
        )
    }
}

@Composable
fun BulletText(text: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text("•", modifier = Modifier.padding(end = 8.dp), fontWeight = FontWeight.Bold)
        Text(text, fontSize = 14.sp, color = Color.DarkGray, lineHeight = 20.sp)
    }
}