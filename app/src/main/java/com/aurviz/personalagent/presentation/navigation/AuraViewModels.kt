package com.aurviz.personalagent.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aurviz.personalagent.AURAApplication

@Composable
inline fun <reified VM : ViewModel> auraViewModel(): VM {
    val application = LocalContext.current.applicationContext as AURAApplication
    return viewModel(factory = application.appContainer.viewModelFactory)
}
