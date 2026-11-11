package com.aurviz.personalagent

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.aurviz.personalagent.presentation.navigation.AppNavigation
import com.aurviz.personalagent.presentation.theme.AuraTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleOAuthIntent(intent)
        setContent {
            AuraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleOAuthIntent(intent)
    }

    private fun handleOAuthIntent(intent: Intent?) {
        val code = intent?.data?.getQueryParameter("code") ?: return
        val state = intent.data?.getQueryParameter("state")
        val auraApplication = application as AURAApplication
        lifecycleScope.launch {
            runCatching { auraApplication.appContainer.gmailOAuthManager.exchangeCode(code, state) }
                .onFailure { android.util.Log.e("AURA", "Gmail authorization failed", it) }
        }
    }
}
