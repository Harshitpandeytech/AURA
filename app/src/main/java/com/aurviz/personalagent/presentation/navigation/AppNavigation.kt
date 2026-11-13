package com.aurviz.personalagent.presentation.navigation

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aurviz.personalagent.presentation.chat.ChatScreen
import com.aurviz.personalagent.presentation.components.MeadowBackdrop
import com.aurviz.personalagent.presentation.history.HistoryScreen
import com.aurviz.personalagent.presentation.memory.MemoryScreen
import com.aurviz.personalagent.presentation.settings.SettingsScreen
import com.aurviz.personalagent.presentation.tasks.TasksScreen

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    val bottomNavItems = listOf(
        BottomNavItem(Screen.Chat, "Chat", Icons.Default.Chat),
        BottomNavItem(Screen.Memory, "Memory", Icons.Default.Memory),
        BottomNavItem(Screen.History, "Activity", Icons.Default.History),
        BottomNavItem(Screen.Settings, "Settings", Icons.Default.Settings)
    )

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 0.dp,
                modifier = Modifier.border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.08f)
                )
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = selected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        ),
                        onClick = {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            MeadowBackdrop()
            NavHost(
                navController = navController,
                startDestination = Screen.Chat.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Chat.route) { ChatScreen() }
                composable(Screen.Memory.route) { MemoryScreen() }
                composable(Screen.Tasks.route) { TasksScreen() }
                composable(Screen.History.route) { HistoryScreen() }
                composable(Screen.Settings.route) { SettingsScreen() }
            }
        }
    }
}
