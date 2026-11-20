package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.CallType
import com.example.ui.components.SmsChatBrandPill
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatDurationTimer
import com.example.ui.theme.CallAcceptGreen
import com.example.ui.theme.CallRejectRed
import com.example.ui.theme.EmeraldVibrant
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianCard
import com.example.ui.viewmodel.ActiveCallState
import com.example.ui.viewmodel.CallPhase

@Composable
fun CallOverlayScreen(
    callState: ActiveCallState,
    onAccept: () -> Unit,
    onRejectOrEnd: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleCamera: () -> Unit,
    onFlipCamera: () -> Unit
) {
    BackHandler { onRejectOrEnd() }

    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasAudioPermission = result[Manifest.permission.RECORD_AUDIO] ?: hasAudioPermission
        hasCameraPermission = result[Manifest.permission.CAMERA] ?: hasCameraPermission
    }

    LaunchedEffect(callState.callType) {
        val needed = buildList {
            if (!hasAudioPermission) add(Manifest.permission.RECORD_AUDIO)
            if (callState.callType == CallType.VIDEO && !hasCameraPermission) add(Manifest.permission.CAMERA)
        }
        if (needed.isNotEmpty()) {
            permLauncher.launch(needed.toTypedArray())
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "call_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A1D15), ObsidianBlack, Color(0xFF050807))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Video preview backdrop when video call is connected/active & camera enabled
        if (callState.callType == CallType.VIDEO && callState.isCameraOn && hasCameraPermission && !callState.isGroup) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 74.dp, end = 16.dp)
                    .size(width = 116.dp, height = 164.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.5.dp, EmeraldVibrant, RoundedCornerShape(18.dp))
            ) {
                CameraPreviewBox(useFrontCamera = callState.useFrontCamera)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Brand & Status
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SmsChatBrandPill()
                Spacer(modifier = Modifier.height(10.dp))
                val statusLabel = when (callState.phase) {
                    CallPhase.INCOMING_RINGING ->
                        if (callState.callType == CallType.VIDEO) "Incoming Video Call" else "Incoming Voice Call"
                    CallPhase.OUTGOING_DIALING -> "Calling..."
                    CallPhase.OUTGOING_RINGING -> "Ringing..."
                    CallPhase.CONNECTED -> formatDurationTimer(callState.durationSec)
                }
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = EmeraldVibrant
                )
            }

            // Center Caller / Group Grid
            if (callState.isGroup && callState.participantNames.isNotEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = callState.peerName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    ) {
                        items(callState.participantNames) { participant ->
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = ObsidianCard,
                                modifier = Modifier.height(118.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    UserAvatar(
                                        name = participant,
                                        colorHex = 0xFF00C853,
                                        size = 48.dp,
                                        isOnline = true
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = participant,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        if (callState.phase != CallPhase.CONNECTED) {
                            Box(
                                modifier = Modifier
                                    .size(152.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(EmeraldVibrant.copy(alpha = 0.16f))
                            )
                        }
                        UserAvatar(
                            name = callState.peerName,
                            colorHex = callState.peerAvatarColorHex,
                            size = 116.dp,
                            isGroup = callState.isGroup
                        )
                    }
                    Spacer(modifier = Modifier.height(22.dp))
                    Text(
                        text = callState.peerName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (callState.callType == CallType.VIDEO) "HD Video Call" else "HD Voice Call",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Bottom Call Controls
            if (callState.phase == CallPhase.INCOMING_RINGING) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = onRejectOrEnd,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(CallRejectRed)
                                .testTag("reject_call_button")
                        ) {
                            Icon(
                                Icons.Filled.CallEnd,
                                contentDescription = "Decline call",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Decline", color = Color.White)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = onAccept,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(CallAcceptGreen)
                                .testTag("accept_call_button")
                        ) {
                            Icon(
                                Icons.Filled.Call,
                                contentDescription = "Accept call",
                                tint = ObsidianBlack,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Accept", color = Color.White)
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = ObsidianCard.copy(alpha = 0.92f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CallControlButton(
                            icon = if (callState.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                            label = if (callState.isMuted) "Unmute" else "Mute",
                            active = callState.isMuted,
                            onClick = onToggleMute,
                            testTag = "call_mute_button"
                        )
                        CallControlButton(
                            icon = if (callState.isSpeakerOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                            label = "Speaker",
                            active = callState.isSpeakerOn,
                            onClick = onToggleSpeaker,
                            testTag = "call_speaker_button"
                        )
                        if (callState.callType == CallType.VIDEO) {
                            CallControlButton(
                                icon = if (callState.isCameraOn) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                                label = "Video",
                                active = !callState.isCameraOn,
                                onClick = onToggleCamera,
                                testTag = "call_camera_button"
                            )
                            CallControlButton(
                                icon = Icons.Filled.Cameraswitch,
                                label = "Flip",
                                active = false,
                                onClick = onFlipCamera,
                                testTag = "call_flip_button"
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = onRejectOrEnd,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(CallRejectRed)
                                    .testTag("end_call_button")
                            ) {
                                Icon(
                                    Icons.Filled.CallEnd,
                                    contentDescription = "End call",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("End", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallControlButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (active) EmeraldVibrant else Color.White.copy(alpha = 0.14f))
                .testTag(testTag)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) ObsidianBlack else Color.White
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun CameraPreviewBox(useFrontCamera: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    DisposableEffect(useFrontCamera) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        val listener = Runnable {
            runCatching {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val selector = if (useFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
            }
        }
        cameraProviderFuture.addListener(listener, executor)
        onDispose {
            runCatching { cameraProviderFuture.get().unbindAll() }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}
