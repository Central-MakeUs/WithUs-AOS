package com.widthus.app.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.widthus.app.viewmodel.MainViewModel
import com.withus.app.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: MainViewModel,
    onFinish: () -> Unit
) {
    val pages = viewModel.onboardingPages
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // 1. 인디케이터 (상단 배치)
            Row(
                Modifier
                    .height(30.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { iteration ->
                    val isSelected = pagerState.currentPage == iteration
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(width = if (isSelected) 24.dp else 8.dp, height = 6.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.Black else Color(0xFFE0E0E0))
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. 메인 페이저
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { pageIndex ->
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 카드 영역 (이미지의 회색 라운드 박스)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.9f)
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color(0xFFF7F7F7)), // 아주 연한 회색 배경
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(modifier = Modifier.height(20.dp))

                            // 상단 캡슐 태그
                            Surface(
                                shape =  RoundedCornerShape(50),
                                color = Color(0xFF333333),
                                modifier = Modifier.padding(bottom = 24.dp)
                            ) {
                                Text(
                                    text = pages[pageIndex].tag, // "오늘의 질문" 등
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            // 제목
                            Text(
                                text = pages[pageIndex].title,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                lineHeight = 34.sp,
                                color = Color.Black
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Image(
                                painter = painterResource(id = pages[pageIndex].imageRes),
                                contentDescription = null,
                                modifier = Modifier
//                                    .fillMaxWidth(0.7f)
//                                    .aspectRatio(0.8f) // 이미지 비율에 맞게 조정
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 상세 설명
                    Text(
                        text = pages[pageIndex].content,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        color = Color(0xFF666666)
                    )
                }
            }

            // 3. 하단 버튼
            val isLastPage = pagerState.currentPage == pages.size - 1

            Button(
                onClick = {
                    if (isLastPage) onFinish()
                    else {
                        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))
            ) {
                Text(
                    text = if (isLastPage) "시작하기" else "다음",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
@Composable
fun LoginScreen(
    onKakaoLogin: () -> Unit,
    onGoogleLogin: () -> Unit,
    onTempLogin: () -> Unit // 이스터에그 동작용 콜백 추가
) {
    // 클릭 횟수를 기억하기 위한 상태
    var clickCount by remember { mutableIntStateOf(0) }
    // 마지막 클릭 시간을 저장 (너무 천천히 누르면 무효화하기 위함)
    var lastClickTime by remember { mutableLongStateOf(0L) }

    Scaffold(containerColor = Color.White) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // 1. 상단 로고 (여기에 이스터에그 심기)
            Text(
                text = "WITHÜS",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = Color.Black,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null // 클릭 시 시각적 효과 제거 (비밀스럽게)
                    ) {
                        val currentTime = System.currentTimeMillis()
                        // 1초 이내에 다시 클릭하면 카운트 증가
                        if (currentTime - lastClickTime < 1000) {
                            clickCount++
                        } else {
                            clickCount = 1
                        }
                        lastClickTime = currentTime

                        if (clickCount >= 3) { // 5번 연속 클릭 시
                            onTempLogin()
                            clickCount = 0 // 초기화
                        }
                    }
            )

            // 중간 빈 공간을 밀어내기 위해 weight 사용
            Spacer(modifier = Modifier.weight(1f))

            Image(
                painter = painterResource(R.drawable.image_login_background),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .aspectRatio(0.8f) // 이미지 비율에 맞게 조정
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Fit
            )

            // 2. 슬로건 영역 (왼쪽 정렬 느낌을 위해 Box나 Column 사용)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "사진으로 이어지는,\n우리 둘만의 기록",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 40.sp,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // 3. 카카오 로그인 버튼
            Button(
                onClick = onKakaoLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFEE500) // 카카오 공식 노란색
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_kakao_logo),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "카카오로 시작하기",
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. 구글 로그인 버튼
            OutlinedButton(
                onClick = onGoogleLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFFF8F8F8)
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google_logo), // 구글 아이콘 필요
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "구글로 시작하기",
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 하단 여백
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}