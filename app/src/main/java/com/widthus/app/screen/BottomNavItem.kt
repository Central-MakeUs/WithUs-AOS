package com.widthus.app.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.withus.app.R

sealed class BottomNavItem(val route: String, val label: String, val icon: Int) {
    object Home : BottomNavItem("home", "홈", R.drawable.ic_home) // 아이콘 리소스 확인 필요
    object Memory : BottomNavItem("fourcut", "추억", R.drawable.ic_grid)
    object Gallery : BottomNavItem("memory", "보관", R.drawable.ic_memory)

    object My : BottomNavItem("my", "마이", R.drawable.ic_person)
}

@Composable
fun MainBottomNavigationBar(
    currentRoute: String,
    onItemSelected: (BottomNavItem) -> Unit
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Memory,
        BottomNavItem.Gallery,
        BottomNavItem.My
    )

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemSelected(item) },
                icon = {
                    Icon(
                        painter = painterResource(id = item.icon),
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp),
                        tint = if (isSelected) Color.Black else Color.Gray
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 12.sp,
                        color = if (isSelected) Color.Black else Color.Gray
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent // 클릭 시 생기는 원형 배경 제거
                )
            )
        }
    }
}