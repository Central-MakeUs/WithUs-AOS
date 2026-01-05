package com.cmc.demoapp.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.ui.platform.ComposeView // ComposeView 임포트
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.cmc.demoapp.screen.AppNavigation

class ComposeFragment : Fragment() {

    @OptIn(ExperimentalGetImage::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppNavigation()
            }
        }
    }

    companion object {
        fun newInstance() = ComposeFragment()
    }
}