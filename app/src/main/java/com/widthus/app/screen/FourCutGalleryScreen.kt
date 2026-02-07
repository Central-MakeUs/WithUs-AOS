package com.widthus.app.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.withus.app.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FourCutGalleryScreen(
    savedImages: List<android.net.Uri>,
    onCreateClick: () -> Unit,
    onDeleteRequest: (List<android.net.Uri>) -> Unit
) {
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedUris = remember { mutableStateListOf<android.net.Uri>() }
    var showBottomSheet by remember { mutableStateOf(false) }

    // === 하단 메뉴 시트 (디자인 수정됨) ===
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor = Color.White,
            dragHandle = null, // 상단 드래그 핸들바 제거 (디자인 깔끔하게)
            // 시스템 네비게이션 바 패딩 처리
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 16.dp)
            ) {
                // 1. 전체 삭제
                TextButton(
                    onClick = {
                        onDeleteRequest(savedImages)
                        showBottomSheet = false
                        isSelectionMode = false
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = androidx.compose.ui.graphics.RectangleShape
                ) {
                    Text("전체 삭제", color = Color.Red, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                }

                // 구분선 (얇게)
                Divider(color = Color(0xFFF5F5F5), thickness = 1.dp)

                // 2. 사진 선택
                TextButton(
                    onClick = {
                        isSelectionMode = true
                        selectedUris.clear()
                        showBottomSheet = false
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = androidx.compose.ui.graphics.RectangleShape
                ) {
                    Text("사진 선택", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                }

                // 3. 두꺼운 구분선 (취소 버튼 위)
                Divider(thickness = 8.dp, color = Color(0xFFF5F5F5))

                // 4. 취소
                TextButton(
                    onClick = { showBottomSheet = false },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = androidx.compose.ui.graphics.RectangleShape
                ) {
                    Text("취소", color = Color.Gray, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            // ✅ [수정 1] CenterAlignedTopAppBar -> TopAppBar (좌측 정렬)
            TopAppBar(
                title = {
                    Text(
                        text = if (isSelectionMode) "사진 선택" else "네컷",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp // 글자 크기 살짝 키움
                    )
                },
                navigationIcon = {
                    // 선택 모드일 때 '취소' 버튼 (X 아이콘 대신 텍스트로도 가능하나, 일단 아이콘 유지)
                    if (isSelectionMode) {
                        IconButton(onClick = {
                            isSelectionMode = false
                            selectedUris.clear()
                        }) {
                            Icon(painterResource(R.drawable.ic_close), contentDescription = "취소", tint = Color.Black)
                        }
                    }
                },
                actions = {
                    // 선택 모드가 아닐 때 '더보기(...)' 메뉴
                    if (!isSelectionMode) {
                        IconButton(onClick = { showBottomSheet = true }) {
                            Icon(Icons.Default.MoreHoriz, contentDescription = "더보기", tint = Color.Black)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            if (!isSelectionMode && savedImages.isNotEmpty()) {
                FloatingActionButton(
                    onClick = onCreateClick,
                    containerColor = Color(0xFF222222),
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "추가")
                }
            }
        },
        bottomBar = {
            if (isSelectionMode) {
                // 삭제 버튼 영역
                Column(
                    modifier = Modifier
                        .background(Color.White)
                        .navigationBarsPadding() // 하단 네비게이션 바 고려
                ) {
                    Divider(color = Color(0xFFEEEEEE))
                    Button(
                        onClick = {
                            onDeleteRequest(selectedUris.toList())
                            isSelectionMode = false
                            selectedUris.clear()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedUris.isNotEmpty()) Color.Red else Color(0xFFE0E0E0),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp),
                        enabled = selectedUris.isNotEmpty()
                    ) {
                        Text("삭제하기 (${selectedUris.size})", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.White)
        ) {
            if (savedImages.isEmpty()) {
                EmptyFourCutView(onCreateClick)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3), // 여기를 3으로 변경
                    contentPadding = PaddingValues(1.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(savedImages) { uri ->
                        FourCutGridItem(
                            uri = uri,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedUris.contains(uri),
                            onToggleSelect = {
                                if (selectedUris.contains(uri)) selectedUris.remove(uri)
                                else selectedUris.add(uri)
                            }
                        )
                    }
                }
            }
        }
    }
}

// (EmptyFourCutView와 FourCutGridItem은 기존 코드와 동일하므로 생략하거나 그대로 두시면 됩니다.)
// 만약 FourCutGridItem이 없다면 아래 코드를 파일 하단에 추가해주세요.

@Composable
fun FourCutGridItem(
    uri: android.net.Uri,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(3f / 4f)
            .clickable(enabled = isSelectionMode) { onToggleSelect() }
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isSelected) Color.Black.copy(alpha = 0.3f) else Color.Transparent)
            )

            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopEnd) // 우측 상단
                    .size(20.dp) // 체크박스 크기 약간 줄임 (3열이라서)
                    .background(
                        color = if (isSelected) Color(0xFF222222) else Color.Transparent,
                        shape = CircleShape
                    )
                    .border(1.5.dp, if(isSelected) Color(0xFF222222) else Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// 텅 빈 화면 컴포저블
@Composable
fun EmptyFourCutView(onCreateClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "앗!\n아직 둘만의 네컷이 없어요.",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 회색 빈 박스 (이미지 대체)
        Box(
            modifier = Modifier
                .size(200.dp, 240.dp) // 비율 대략 맞춤
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFEEEEEE))
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "마음에 드는 순간을 모아\n둘만의 인생 네컷을 만들어 보세요.",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onCreateClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.padding(horizontal = 40.dp).height(48.dp)
        ) {
            Text("네컷 만들기 ->", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
