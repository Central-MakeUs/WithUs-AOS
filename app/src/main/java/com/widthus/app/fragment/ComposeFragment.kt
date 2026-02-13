package com.widthus.app.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView // ComposeView 임포트
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.widthus.app.screen.AppNavigation
import dagger.hilt.android.AndroidEntryPoint
import org.withus.app.remote.NetworkLoadingManager
import javax.inject.Inject


@AndroidEntryPoint
class ComposeFragment : Fragment() {

    @Inject
    lateinit var networkLoadingManager: NetworkLoadingManager

    @OptIn(ExperimentalGetImage::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {

            setContent {
                val myColorScheme = lightColorScheme(
                    surface = Color(0xFFF8F8F8),
                    background = Color(0xFFF8F8F8),
                    surfaceVariant = Color(0xFFF8F8F8) // 카드 등의 배경색
                )
                MaterialTheme(colorScheme = myColorScheme) {

                    // LoadingManager의 상태 관찰
                    val isLoading by networkLoadingManager.isLoading.collectAsState()

                    // 최상위 레이아웃
                    Box(modifier = Modifier.fillMaxSize()) {
                        // 1. 실제 앱의 메인 컨텐츠 (네비게이션)
                        AppNavigation()

                        // 2. 전역 로딩 오버레이
                        if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f))
                                    .pointerInput(Unit) {}, // 터치 이벤트 전파 방지
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color.Red,
                                    strokeWidth = 4.dp
                                )
                            }
                        }
                    }

                }
                }
        }
    }

    companion object {
        fun newInstance() = ComposeFragment()
    }
}