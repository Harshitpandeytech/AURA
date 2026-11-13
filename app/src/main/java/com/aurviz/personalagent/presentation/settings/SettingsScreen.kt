package com.aurviz.personalagent.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.app.Activity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurviz.personalagent.presentation.components.AuraAgentState
import com.aurviz.personalagent.presentation.components.ErrorState
import com.aurviz.personalagent.presentation.components.GlassCard
import com.aurviz.personalagent.presentation.components.LoadingState
import com.aurviz.personalagent.presentation.components.MrAuraAvatar
import com.aurviz.personalagent.presentation.navigation.auraViewModel
import com.aurviz.personalagent.utils.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = auraViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }
    var backendEmail by rememberSaveable { mutableStateOf("") }
    var backendPassword by rememberSaveable { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        when (val state = uiState) {
            UiState.Idle, UiState.Loading -> LoadingState("Loading settings")
            is UiState.Error -> ErrorState(state.message, onRetry = viewModel::dismissError)
            is UiState.Success -> SettingsContent(
                model = state.data,
                onClearData = { confirmClear = true },
                onConnectGmail = {
                    viewModel.gmailAuthorizationUri()?.let {
                        context.startActivity(Intent(Intent.ACTION_VIEW, it))
                    }
                },
                backendEmail = backendEmail,
                backendPassword = backendPassword,
                onBackendEmailChange = { backendEmail = it },
                onBackendPasswordChange = { backendPassword = it },
                onBackendAuth = { register ->
                    viewModel.authenticateBackend(backendEmail, backendPassword, register)
                },
                onBackendSignOut = viewModel::signOutBackend
            )
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all data") },
            text = { Text("This deletes saved memories, scheduled tasks, and execution history on this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        viewModel.clearAllData {
                            (context as? Activity)?.recreate()
                        }
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsContent(
    model: SettingsUiModel,
    onClearData: () -> Unit,
    onConnectGmail: () -> Unit,
    backendEmail: String,
    backendPassword: String,
    onBackendEmailChange: (String) -> Unit,
    onBackendPasswordChange: (String) -> Unit,
    onBackendAuth: (Boolean) -> Unit,
    onBackendSignOut: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Mr. Aura Hero Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            borderColor = Color.White.copy(alpha = 0.12f)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MrAuraAvatar(
                    state = AuraAgentState.IDLE,
                    size = 54.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "Mr. Aura",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Personal Autonomous AI Companion",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        SettingsSection(title = "AI Configuration") {
            SettingsItem(
                icon = Icons.Default.Key,
                title = "Gemini API Key",
                subtitle = if (model.apiKeyConfigured) "Configured" else "Not configured",
                onClick = {}
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSection(title = "Production Backend") {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    if (!model.backendConfigured) "Backend URL is not configured in local.properties."
                    else if (model.backendAuthenticated) "Connected to the production backend."
                    else "Sign in before using production AI."
                )
                if (model.backendConfigured && !model.backendAuthenticated) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = backendEmail,
                        onValueChange = onBackendEmailChange,
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = backendPassword,
                        onValueChange = onBackendPasswordChange,
                        label = { Text("Password (12+ characters)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Button(onClick = { onBackendAuth(false) }) { Text("Sign in") }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { onBackendAuth(true) }) { Text("Register") }
                    }
                } else if (model.backendAuthenticated) {
                    TextButton(onClick = onBackendSignOut) { Text("Sign out") }
                }
                model.statusMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSection(title = "Integrations") {
            SettingsItem(
                icon = Icons.Default.Email,
                title = "Gmail Account",
                subtitle = if (model.gmailConnected) "Connected" else "Not connected",
                onClick = onConnectGmail
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSection(title = "Data & Privacy") {
            SettingsItem(
                icon = Icons.Default.DeleteForever,
                title = "Clear All Data",
                subtitle = model.statusMessage ?: "Delete memories, logs, and tasks",
                onClick = onClearData
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSection(title = "About") {
            SettingsItem(
                icon = Icons.Default.Info,
                title = "AURA Personal Agent",
                subtitle = "Version ${model.appVersion}",
                onClick = {}
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        borderColor = Color.White.copy(alpha = 0.12f)
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
