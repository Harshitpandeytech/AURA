package com.aurviz.personalagent.presentation.navigation

sealed class Screen(val route: String) {
    data object Chat : Screen("chat")
    data object Memory : Screen("memory")
    data object Tasks : Screen("tasks")
    data object History : Screen("history")
    data object Settings : Screen("settings")
}
