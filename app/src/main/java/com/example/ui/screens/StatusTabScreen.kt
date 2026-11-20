package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.StatusMediaType
import com.example.data.model.StatusUpdateEntity
import com.example.data.model.UserProfileEntity
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatTimeShort
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun StatusTabScreen(
    currentUser: UserProfileEntity?,
    statuses: List<StatusUpdateEntity>,
    onPostTextStatus: (String, Long) -> Unit,
    onPostMediaStatus: (Uri, Boolean, String) -> Unit,
    onMarkViewed: (String) -> Unit,
    onDeleteStatus: (String) -> Unit
) {
    val context = LocalContext.current
    var showTextComposer by remember { mutableStateOf(false) }
    var pendingMediaUri by remember { mutableStateOf<Pair<Uri, Boolean>?>(null) }
    var activeViewingStatus by remember { mutableStateOf<StatusUpdateEntity?>(null) }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri) ?: ""
            val isVideo = mime.startsWith("video/")
            pendingMediaUri = uri to isVideo
        }
    }

    val myStatuses = remember(statuses, currentUser) {
        statuses.filter { it.ownerId == currentUser?.id }
    }
    val recentUpdates = remember(statuses, currentUser) {
        statuses.filter { it.ownerId != currentUser?.id && !it.isViewed }
    }
    val viewedUpdates = remember(statuses, currentUser) {
        statuses.filter { it.ownerId != currentUser?.id && it.isViewed }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Status & Stories",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Updates expire automatically after 24 hours",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // My Status Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = myStatuses.isNotEmpty()) {
                                    activeViewingStatus = myStatuses.firstOrNull()
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(
                                name = currentUser?.displayName ?: "Me",
                                colorHex = currentUser?.avatarColorHex ?: 0xFF00C853,
                                avatarUri = currentUser?.avatarUri,
                                size = 54.dp,
                                hasUnviewedStatus = myStatuses.isNotEmpty()
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "My Status",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (myStatuses.isEmpty()) {
                                        "Share a text, photo, or video story"
                                    } else {
                                        "${myStatuses.size} active update · Tap to view"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showTextComposer = true },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("add_text_status_button")
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Text Story")
                            }
                            Button(
                                onClick = {
                                    mediaPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("add_media_status_button")
                            ) {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Photo/Video")
                            }
                        }
                    }
                }
            }

            if (recentUpdates.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Recent Updates",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(recentUpdates, key = { it.id }) { status ->
                    StatusRow(
                        status = status,
                        onClick = {
                            onMarkViewed(status.id)
                            activeViewingStatus = status
                        }
                    )
                }
            }

            if (viewedUpdates.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Viewed Updates",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(viewedUpdates, key = { it.id }) { status ->
                    StatusRow(
                        status = status,
                        onClick = { activeViewingStatus = status }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showTextComposer) {
        TextStatusComposerDialog(
            onDismiss = { showTextComposer = false },
            onPost = { text, bgHex ->
                showTextComposer = false
                onPostTextStatus(text, bgHex)
            }
        )
    }

    pendingMediaUri?.let { (uri, isVideo) ->
        var caption by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { pendingMediaUri = null },
            title = { Text(if (isVideo) "Post Video Status" else "Post Photo Status") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Status media preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (isVideo) {
                            Icon(
                                Icons.Filled.PlayCircle,
                                contentDescription = "Video",
                                tint = Color.White,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        label = { Text("Caption (optional)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    pendingMediaUri = null
                    onPostMediaStatus(uri, isVideo, caption)
                }) {
                    Text("Share Story")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingMediaUri = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    activeViewingStatus?.let { viewing ->
        StatusViewerOverlay(
            status = viewing,
            isMine = viewing.ownerId == currentUser?.id,
            onDismiss = { activeViewingStatus = null },
            onDelete = {
                activeViewingStatus = null
                onDeleteStatus(viewing.id)
            }
        )
    }
}

@Composable
private fun StatusRow(
    status: StatusUpdateEntity,
    onClick: () -> Unit
) {
    val hoursLeft = ((status.expiresAtMillis - System.currentTimeMillis()) / 3_600_000L).coerceAtLeast(1L)
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("status_row_${status.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                name = status.ownerName,
                colorHex = status.ownerAvatarColorHex,
                avatarUri = status.ownerAvatarUri,
                size = 52.dp,
                hasUnviewedStatus = !status.isViewed
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(status.ownerName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${formatTimeShort(status.createdAtMillis)} · ${status.type.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${hoursLeft}h left",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun TextStatusComposerDialog(
    onDismiss: () -> Unit,
    onPost: (String, Long) -> Unit
) {
    val colors = listOf(0xFF0B3B24L, 0xFF0F2933L, 0xFF28163AL, 0xFF3E1F1BL, 0xFF0D1B2AL)
    var text by remember { mutableStateOf("") }
    var selectedBg by remember { mutableLongStateOf(colors.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Text Status") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(selectedBg))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = text.ifBlank { "Type your 24h status update..." },
                        color = Color.White.copy(alpha = if (text.isBlank()) 0.55f else 1f),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    colors.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(
                                    width = if (selectedBg == hex) 2.5.dp else 0.dp,
                                    color = if (selectedBg == hex) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedBg = hex }
                        )
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Status text") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("text_status_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onPost(text, selectedBg) },
                enabled = text.isNotBlank(),
                modifier = Modifier.testTag("post_text_status_button")
            ) {
                Text("Post")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun StatusViewerOverlay(
    status: StatusUpdateEntity,
    isMine: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    BackHandler { onDismiss() }

    var progress by remember(status.id) { mutableFloatStateOf(0f) }
    LaunchedEffect(status.id) {
        val steps = 60
        for (i in 1..steps) {
            delay(85)
            progress = i.toFloat() / steps
        }
        onDismiss()
    }

    val mediaType = runCatching { StatusMediaType.valueOf(status.type) }.getOrDefault(StatusMediaType.TEXT)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (mediaType == StatusMediaType.TEXT) {
                    Brush.verticalGradient(listOf(Color(status.backgroundHex), Color(0xFF050A07)))
                } else {
                    Brush.verticalGradient(listOf(Color.Black, Color(0xFF0A0F0D)))
                }
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .clickable { onDismiss() }
    ) {
        if (mediaType != StatusMediaType.TEXT && !status.mediaUri.isNullOrBlank()) {
            val model: Any = if (status.mediaUri.startsWith("/")) File(status.mediaUri) else status.mediaUri
            AsyncImage(
                model = model,
                contentDescription = "Status media",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.25f)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    name = status.ownerName,
                    colorHex = status.ownerAvatarColorHex,
                    avatarUri = status.ownerAvatarUri,
                    size = 40.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(status.ownerName, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        text = formatTimeShort(status.createdAtMillis),
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (isMine) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete status", tint = Color.White)
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }

        if (mediaType == StatusMediaType.TEXT) {
            Text(
                text = status.text,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 28.dp)
            )
        } else if (status.text.isNotBlank()) {
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
            ) {
                Text(
                    text = status.text,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        if (mediaType == StatusMediaType.VIDEO) {
            Icon(
                Icons.Filled.PlayCircle,
                contentDescription = "Video story",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Visibility,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${status.viewCount} views · Expires in 24h",
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
