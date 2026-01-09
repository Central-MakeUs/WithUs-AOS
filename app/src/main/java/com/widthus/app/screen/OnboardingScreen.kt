package com.widthus.app.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 상단 이미지 영역
            Spacer(modifier = Modifier.height(155.dp))

            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(Color(0xFFE6E6E6)),
                contentAlignment = Alignment.Center
            ) {
                /* ... 이미지 로직 생략 ... */
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 1. 텍스트 페이저 (필요한 만큼만 높이 차지)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { pageIndex ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = pages[pageIndex].title,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp,
                        color = Color.Black,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = pages[pageIndex].description,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        color = Color(0xFF212121),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ⭐ 핵심: 여기서 나머지 모든 빈 공간을 다 차지해서 아래 컴포넌트들을 바닥으로 밀어냅니다.
            Spacer(modifier = Modifier.weight(1f))

            // 2. 인디케이터 (바닥 근처)
            Row(
                Modifier
                    .height(30.dp) // 높이를 줄여서 버튼과 더 가깝게 배치 가능
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { iteration ->
                    val isSelected = pagerState.currentPage == iteration
                    val width = if (isSelected) 32.dp else 10.dp
                    val color = if (isSelected) Color.Black else Color.LightGray

                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(width = width, height = 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp)) // 인디케이터와 버튼 사이 간격

            // 3. 하단 버튼
            val isLastPage = pagerState.currentPage == pages.size - 1

            // 마지막 페이지가 아닐 때도 '다음' 버튼을 보여주려면 if(isLastPage)를 제거하세요.
            // 현재는 마지막 페이지에서만 버튼이 나타납니다.
            if (isLastPage) {
                Button(
                    onClick = {
                        if (isLastPage) onFinish()
                        else {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLastPage) Color.Black else Color(0xFFF0F0F0)
                    )
                ) {
                    Text(
                        text = if (isLastPage) "시작하기" else "다음",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLastPage) Color.White else Color.Gray
                    )
                }
            }

            // 바닥에서의 최종 마진 (기기 하단 바와의 간격)
            Spacer(modifier = Modifier.height(46.dp))
        }
    }
}

@Composable
fun LoginScreen(
    onKakaoLogin: () -> Unit,
    onGoogleLogin: () -> Unit
) {
    Scaffold(
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. 상단 로고 영역
            Spacer(modifier = Modifier.height(80.dp))

            Text(
                text = "WITHÜS",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = Color.Black
            )

            // 중간 빈 공간을 밀어내기 위해 weight 사용
            Spacer(modifier = Modifier.weight(1f))

            // 2. 슬로건 영역 (왼쪽 정렬 느낌을 위해 Box나 Column 사용)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "사진으로 이어지는,\n우리 둘만의 기록",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
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
                    Icon(
                        painter = painterResource(id = R.drawable.ic_kakao_logo), // 카카오 아이콘 필요
                        contentDescription = null,
                        tint = Color.Black,
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