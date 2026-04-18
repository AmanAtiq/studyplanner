package com.studyassistant.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.navigation.NavController

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun StudyBottomBar(
    navController: NavController,
    currentRoute: String?
) {
    val items = listOf(
        BottomNavItem("home", "Home", Icons.Default.Home),
        BottomNavItem("lectures", "Lectures", Icons.Default.Book),
        BottomNavItem("quizzes", "Marks", Icons.Default.Analytics),
        BottomNavItem("settings", "Settings", Icons.Default.Settings)
    )

    NavigationBar(
        containerColor = Color(0xFFFFF9C4), // Light Yellow (soft) background for bottom bar
        contentColor = Color.Black,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(32.dp))
            .border(1.dp, Color.Black, RoundedCornerShape(32.dp)),
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo("home") { saveState = true }
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { 
                    Text(
                        item.label, 
                        fontWeight = if (currentRoute == item.route) FontWeight.Bold else FontWeight.Normal
                    ) 
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Black,
                    unselectedIconColor = Color.Black,
                    selectedTextColor = Color.Black,
                    unselectedTextColor = Color.Black,
                    indicatorColor = Color.White // Active tab indicator is white
                )
            )
        }
    }
}