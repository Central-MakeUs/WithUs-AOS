package com.cmc.demoapp.screen

import AppBottomNavigation
import ProfileCircleItem
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmc.demoapp.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayUsUploadScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    // 갤러리 선택 Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.updateSelectedImage(uri)
        showSheet = false
    }

    // 카메라 촬영 Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        // 실제 구현시에는 Bitmap을 Uri로 변환하거나 저장 로직이 필요합니다.
        // 여기서는 흐름만 잡기 위해 로그 처리
        showSheet = false
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "DAYUS", fontSize = 20.sp, fontWeight = FontWeight.Black)
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.clickable { onBack() }
                )
            }
        },
        bottomBar = { AppBottomNavigation() }, // 기존 공통 하단바
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues).fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. 프로필 영역
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ProfileCircleItem(text = "나", isActive = true)
                ProfileCircleItem(text = "이미지", isActive = false)
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 2. 타이틀
            Text(
                text = "오늘",
                modifier = Modifier.fillMaxWidth(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. 사진 등록 카드 영역 (이미지 image_2071d5.jpg 참조)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFF5F5F5))
                    .clickable { showSheet = true }, // 카드 클릭 시에도 업로드 메뉴 호출
                contentAlignment = Alignment.Center
            ) {
                if (viewModel.selectedImageUri != null) {
                    // 사진이 등록되었을 때 보여주는 뷰
                    AsyncImage(
                        model = viewModel.selectedImageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    // 사진이 없을 때 (+) 버튼
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(Color.Black, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "당신의 하루를 상대방에게 보내고\n상대방의 하루를 확인해보세요",
                            textAlign = TextAlign.Center,
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 중앙 카메라 실행 버튼
            IconButton(
                onClick = { showSheet = true },
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.Black, CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Camera", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
    }

    // 사진 선택 방식 팝업 (Bottom Sheet)
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp, start = 20.dp, end = 20.dp)
            ) {
                Text(
                    "오늘의 하루 등록하기",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                ListItem(
                    headlineContent = { Text("사진 촬영하기") },
                    leadingContent = { Icon(Icons.Default.Add, contentDescription = null) },
                    modifier = Modifier.clickable { cameraLauncher.launch() }
                )
                ListItem(
                    headlineContent = { Text("갤러리에서 선택하기") },
                    leadingContent = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    modifier = Modifier.clickable { galleryLauncher.launch("image/*") }
                )
            }
        }
    }
}