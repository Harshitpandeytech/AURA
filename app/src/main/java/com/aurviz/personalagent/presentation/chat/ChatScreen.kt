package com.aurviz.personalagent.presentation.chat

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurviz.personalagent.domain.model.ChatMessage
import com.aurviz.personalagent.domain.model.MessageRole
import com.aurviz.personalagent.presentation.components.ErrorState
import com.aurviz.personalagent.presentation.components.GlassCard
import com.aurviz.personalagent.presentation.components.LoadingState
import com.aurviz.personalagent.presentation.components.AiOrbiter
import com.aurviz.personalagent.presentation.components.QuickActionChip
import com.aurviz.personalagent.presentation.navigation.auraViewModel
import com.aurviz.personalagent.utils.DateTimeUtils
import com.aurviz.personalagent.utils.UiState
import com.aurviz.personalagent.voice.SpeechRecognizerManager
import com.aurviz.personalagent.voice.SpeechOutput
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel = auraViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var listening by remember { mutableStateOf(false) }
    var audioRms by remember { mutableFloatStateOf(0f) }
    var voiceMode by remember { mutableStateOf(false) }
    var speechEnabled by remember { mutableStateOf(true) }

    val recognizer = remember {
        SpeechRecognizerManager(context, object : SpeechRecognizerManager.Listener {
            override fun onPartialResult(text: String) {
                viewModel.setDraftFromVoice(text)
            }
            override fun onResult(text: String) {
                listening = false
                voiceMode = false
                audioRms = 0f
                if (text.isNotBlank()) {
                    // Auto-forward voice command without manual button click
                    viewModel.sendVoiceMessage(text)
                }
            }

            override fun onError(message: String) {
                listening = false
                voiceMode = false
                audioRms = 0f
            }

            override fun onRmsChanged(rmsdB: Float) {
                audioRms = rmsdB
            }
        })
    }

    DisposableEffect(Unit) {
        onDispose { recognizer.destroy() }
    }

    val lastAssistantMessage = (uiState as? UiState.Success)?.data?.messages
        ?.lastOrNull { it.role == MessageRole.ASSISTANT }
    val speechOutput = remember { SpeechOutput(context) }

    LaunchedEffect(lastAssistantMessage?.id) {
        if (speechEnabled) {
            lastAssistantMessage?.let { speechOutput.speak(it.content) }
        }
    }

    DisposableEffect(Unit) {
        onDispose { speechOutput.shutdown() }
    }

    var showMicPermissionDialog by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            listening = true
            voiceMode = true
            recognizer.start()
        }
    }

    if (showMicPermissionDialog) {
        com.aurviz.personalagent.presentation.components.PermissionDialog(
            title = "Microphone Access Required",
            description = "AURA needs microphone access to listen to your voice commands and convert them to text.",
            confirmButtonText = "Allow",
            onConfirm = {
                showMicPermissionDialog = false
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            onDismiss = { showMicPermissionDialog = false }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AnimatedVisibility(
            visible = !voiceMode,
            enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.96f),
            exit = fadeOut(tween(220)) + scaleOut(targetScale = 1.04f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AiOrbiter(size = 40.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "AURA",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            speechEnabled = !speechEnabled
                            if (!speechEnabled) speechOutput.stop()
                        }
                    ) {
                        Icon(
                            imageVector = if (speechEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = if (speechEnabled) "Turn responses off" else "Turn responses on",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = viewModel::startNewChat) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New chat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                when (val state = uiState) {
                    UiState.Idle, UiState.Loading -> LoadingState("Initializing AURA agent...")
                    is UiState.Error -> ErrorState(state.message, onRetry = viewModel::dismissError)
                    is UiState.Success -> ChatContent(
                        model = state.data,
                        onDraftChange = viewModel::onDraftChange,
                        onSend = viewModel::sendMessage,
                        onQuickAction = { prompt -> viewModel.onDraftChange(prompt) },
                        onConfirm = viewModel::confirmPendingAction,
                        listening = listening,
                        onVoice = {
                            if (listening) {
                                recognizer.stop()
                                listening = false
                                voiceMode = false
                                audioRms = 0f
                            } else if (androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                listening = true
                                voiceMode = true
                                recognizer.start()
                            } else {
                                showMicPermissionDialog = true
                            }
                        },
                        onNewChat = viewModel::startNewChat,
                        modifier = Modifier
                            .weight(1f)
                            .imePadding()
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = voiceMode,
            enter = fadeIn(tween(350)) + scaleIn(initialScale = 0.72f),
            exit = fadeOut(tween(250)) + scaleOut(targetScale = 1.18f)
        ) {
            VoiceOrbiterMode(
                listening = listening,
                onDismiss = {
                    recognizer.stop()
                    listening = false
                    voiceMode = false
                    audioRms = 0f
                }
            )
        }
    }
}

@Composable
private fun VoiceOrbiterMode(
    listening: Boolean,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AiOrbiter(size = 260.dp)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if (listening) "Listening..." else "Tap to return to text",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ChatContent(
    model: ChatUiModel,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onQuickAction: (String) -> Unit,
    onConfirm: () -> Unit,
    listening: Boolean,
    onVoice: () -> Unit,
    onNewChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val isImeVisible = WindowInsets.isImeVisible

    // Fix: Automatically scroll to latest message whenever new messages arrive or keyboard pops up
    LaunchedEffect(model.messages.size, isImeVisible) {
        if (model.messages.isNotEmpty()) {
            listState.animateScrollToItem(model.messages.lastIndex)
        }
    }

    // Scroll to bottom helper button visibility
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0) return@derivedStateOf true
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= totalItems - 1
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Chat message list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (model.messages.isEmpty()) {
                    item {
                        WelcomeCockpit(
                            modifier = Modifier.fillParentMaxHeight(),
                            onQuickAction = onQuickAction
                        )
                    }
                }

                items(model.messages, key = { it.id }) { message ->
                    ModernChatBubble(message = message)
                    if (message.toolCalls.any { it.status == com.aurviz.personalagent.domain.model.ToolCallStatus.AWAITING_CONFIRMATION }) {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Action Confirmation Required",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "Please verify before AURA proceeds with execution.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = onConfirm,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Confirm")
                                }
                            }
                        }
                    }
                }

                // If agent is processing, display active thinking status
                if (model.isProcessing) {
                    item {
                        AgentThinkingIndicator(status = model.executionStatus)
                    }
                }
            }

            // Floating Glassmorphic Input Capsule (Reference Image 2)
            GlassInputCapsule(
                draft = model.draft,
                onDraftChange = onDraftChange,
                onSend = onSend,
                listening = listening,
                onVoice = onVoice,
                onNewChat = onNewChat
            )
        }

        // Stick-to-bottom scroll button (Inspired by conversation.tsx)
        AnimatedVisibility(
            visible = !isAtBottom && model.messages.isNotEmpty(),
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 86.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                    .clickable {
                        scope.launch {
                            if (model.messages.isNotEmpty()) {
                                listState.animateScrollToItem(model.messages.lastIndex)
                            }
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "Scroll to bottom",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Jump to latest",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun WelcomeCockpit(
    modifier: Modifier = Modifier,
    onQuickAction: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AiOrbiter(size = 124.dp)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "AURA",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            "Things I can do",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Actions Row (Reference Image 3)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionChip(
                label = "Ring me in 10 seconds",
                icon = Icons.Default.Schedule,
                onClick = { onQuickAction("Set a timer for 10 seconds and ring me") }
            )
            QuickActionChip(
                label = "Analyze & Summarize",
                icon = Icons.Default.AutoAwesome,
                onClick = { onQuickAction("Analyze my recent unread emails and tasks") }
            )
            QuickActionChip(
                label = "Read Emails",
                icon = Icons.Default.Email,
                onClick = { onQuickAction("Check my inbox for important unread emails") }
            )
            QuickActionChip(
                label = "Schedule Task",
                icon = Icons.Default.Schedule,
                onClick = { onQuickAction("Schedule a reminder for tomorrow at 9 AM") }
            )
            QuickActionChip(
                label = "Search Memory",
                icon = Icons.Default.Memory,
                onClick = { onQuickAction("What memories do you have saved about me?") }
            )
        }
    }
}

@Composable
private fun AgentThinkingIndicator(status: ExecutionStatus?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        AiOrbiter(size = 32.dp)
        Spacer(modifier = Modifier.width(10.dp))
        GlassCard(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(vertical = 2.dp),
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Revolving circular indicator with glowing primary tone
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = status?.label ?: "AURA is reasoning...",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ModernChatBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(if (isUser) 1f else 0.94f)
        ) {
            if (!isUser) {
                AiOrbiter(size = 30.dp)
                Spacer(modifier = Modifier.width(8.dp))
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    }
                ),
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isUser) 18.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 18.dp
                ),
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = if (isUser) 0.25f else 0.12f),
                                Color.White.copy(alpha = 0.04f)
                            )
                        ),
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        )
                    )
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = message.content,
                        color = if (isUser) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = DateTimeUtils.formatTime(message.timestamp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = (if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

// Minimalist Frosted Glass Input Capsule (Reference Image 2)
@Composable
private fun GlassInputCapsule(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    listening: Boolean,
    onVoice: () -> Unit,
    onNewChat: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        GlassCard(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(28.dp),
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            borderColor = Color.White.copy(alpha = 0.18f),
            borderWidth = 1.2.dp
        ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp)) {
            // Text Input Field
            TextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = {
                    Text(
                        if (listening) "Listening... speak now" else "Ask me anything...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                maxLines = 4
            )

            // Bottom Actions inside Capsule (Reference Image 2)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Action Buttons
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Actions",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(onClick = onNewChat)
                )

                Spacer(modifier = Modifier.width(14.dp))

                val micScale by animateFloatAsState(
                    targetValue = if (listening) 1.12f else 1f,
                    animationSpec = tween(250),
                    label = "voice_button_scale"
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .scale(micScale)
                        .clip(CircleShape)
                        .background(
                            if (listening) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                        )
                        .clickable(onClick = onVoice),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice input",
                        tint = if (listening) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Elevating Send Button (Aquarium upward arrow style)
                val canSend = draft.isNotBlank()
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (canSend) Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                )
                            )
                            else Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = if (canSend) Color.White.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f),
                            shape = CircleShape
                        )
                        .clickable(enabled = canSend, onClick = onSend),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Send",
                        tint = if (canSend) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}
}
