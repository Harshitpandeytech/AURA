@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.aurviz.personalagent.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aurviz.personalagent.presentation.chat.ChatScreen
import com.aurviz.personalagent.presentation.components.RibbonFieldGradient
import com.aurviz.personalagent.presentation.history.HistoryScreen
import com.aurviz.personalagent.presentation.memory.MemoryScreen
import com.aurviz.personalagent.presentation.settings.SettingsScreen
import com.aurviz.personalagent.presentation.theme.AuraType
import com.aurviz.personalagent.presentation.theme.TextPrimary
import com.aurviz.personalagent.presentation.theme.TextSecondary

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val imeVisible = WindowInsets.isImeVisible

    val bottomNavItems = listOf(
        BottomNavItem(Screen.Chat, "Chat", Icons.Rounded.ChatBubble),
        BottomNavItem(Screen.Memory, "Memory", Icons.Rounded.Memory),
        BottomNavItem(Screen.History, "Activity", Icons.Rounded.History),
        BottomNavItem(Screen.Settings, "Settings", Icons.Rounded.Settings)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        RibbonFieldGradient(modifier = Modifier.fillMaxSize())

        Box(modifier = Modifier.fillMaxSize()) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            // Content Area
            NavHost(
                navController = navController,
                startDestination = Screen.Chat.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (!imeVisible) 76.dp else 0.dp)
            ) {
                composable(Screen.Chat.route) { ChatScreen() }
                composable(Screen.Memory.route) { MemoryScreen() }
                composable(Screen.History.route) { HistoryScreen() }
                composable(Screen.Settings.route) { SettingsScreen() }
            }

            if (!imeVisible) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(26.dp))
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                                )
                                .clickable {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                .padding(vertical = 6.dp)
                                .padding(horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (selected) MaterialTheme.colorScheme.primary else TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = item.label,
                                style = AuraType.nav,
                                color = if (selected) TextPrimary else TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
