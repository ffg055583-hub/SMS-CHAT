package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MessageEntity
import com.example.data.model.MessageType
import com.example.media.VoicePlaybackUiState
import com.example.ui.components.ReceiptIcon
import com.example.ui.components.formatDurationTimer
import com.example.ui.components.formatTimeShort
import com.example.ui.theme.EmeraldVibrant
import com.example.ui.theme.IncomingBubbleDark
import com.example.ui.theme.IncomingBubbleLight
import com.example.ui.theme.OutgoingBubbleDark
import com.example.ui.theme.OutgoingBubbleDarkEnd
import com.example.ui.theme.OutgoingBubbleLight
import com.example.ui.theme.OutgoingBubbleLightEnd
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageBubble(
    message: MessageEntity,
    isMine: Boolean,
    isGroup: Boolean,
    isDarkTheme: Boolean,
    showReadReceipts: Boolean,
    playbackState: VoicePlaybackUiState,
    onToggleVoicePlay: (String, String?) -> Unit,
    onMediaClick: (MessageEntity) -> Unit,
    onLongPress: (MessageEntity) -> Unit
) {
    val msgType = runCatching { MessageType.valueOf(message.type) }.getOrDefault(MessageType.TEXT)

    if (msgType == MessageType.SYSTEM) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
        }
        return
    }

    val bubbleShape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (isMine) 18.dp else 4.dp,
        bottomEnd = if (isMine) 4.dp else 18.dp
    )

    val bubbleBrush = when {
        isMine && isDarkTheme -> Brush.linearGradient(listOf(OutgoingBubbleDark, OutgoingBubbleDarkEnd))
        isMine && !isDarkTheme -> Brush.linearGradient(listOf(OutgoingBubbleLight, OutgoingBubbleLightEnd))
        !isMine && isDarkTheme -> Brush.linearGradient(listOf(IncomingBubbleDark, IncomingBubbleDark))
        else -> Brush.linearGradient(listOf(IncomingBubbleLight, IncomingBubbleLight))
    }

    val textColor = if (isDarkTheme) Color(0xFFF2FBF6) else Color(0xFF0B1410)
    val metaColor = textColor.copy(alpha = 0.65f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        if (msgType == MessageType.STICKER && !message.isDeletedForAll) {
            Column(
                horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { onLongPress(message) }
                )
            ) {
                if (isGroup && !isMine) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldVibrant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = message.stickerCode ?: "✨",
                            fontSize = 52.sp
                        )
                        if (message.text.isNotBlank()) {
                            Text(
                                text = message.text,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                ) {
                    Text(
                        text = formatTimeShort(message.timestampMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isMine) {
                        Spacer(modifier = Modifier.width(4.dp))
                        val effectiveStatus = if (!showReadReceipts && message.status == "READ") "DELIVERED" else message.status
                        ReceiptIcon(status = effectiveStatus)
                    }
                }
            }
            return
        }

        Box(
            modifier = Modifier
                .widthIn(min = 90.dp, max = 300.dp)
                .clip(bubbleShape)
                .background(bubbleBrush)
                .combinedClickable(
                    onClick = {
                        if ((msgType == MessageType.IMAGE || msgType == MessageType.VIDEO) && !message.isDeletedForAll) {
                            onMediaClick(message)
                        }
                    },
                    onLongClick = { onLongPress(message) }
                )
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Column {
                if (isGroup && !isMine) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldVibrant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                if (!message.replyToText.isNullOrBlank() && !message.isDeletedForAll) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = if (isDarkTheme) 0.25f else 0.08f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(16.dp)
                                    .background(EmeraldVibrant, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = message.replyToText,
                                style = MaterialTheme.typography.bodySmall,
                                color = metaColor,
                                maxLines = 1
                            )
                        }
                    }
                }

                if (message.isDeletedForAll) {
                    Text(
                        text = "This message was deleted",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = metaColor
                    )
                } else {
                    when (msgType) {
                        MessageType.IMAGE, MessageType.VIDEO -> {
                            val model: Any? = message.mediaUri?.let {
                                if (it.startsWith("/")) File(it) else it
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = model,
                                    contentDescription = if (msgType == MessageType.VIDEO) "Video attachment" else "Photo attachment",
                                    modifier = Modifier.fillMaxWidth(),
                                    contentScale = ContentScale.Crop
                                )
                                if (msgType == MessageType.VIDEO) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayCircle,
                                        contentDescription = "Play video",
                                        tint = Color.White,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                            }
                            if (message.text.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = message.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = textColor
                                )
                            }
                        }

                        MessageType.VOICE -> {
                            val isPlaying = playbackState.playingMessageId == message.id
                            val progress = if (isPlaying) playbackState.progress else 0f
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.width(220.dp)
                            ) {
                                IconButton(
                                    onClick = { onToggleVoicePlay(message.id, message.mediaUri) },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldVibrant)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause voice note" else "Play voice note",
                                        tint = Color(0xFF070B09)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(50)),
                                        color = EmeraldVibrant,
                                        trackColor = textColor.copy(alpha = 0.2f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = formatDurationTimer(message.voiceDurationSec),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = metaColor
                                    )
                                }
                            }
                        }

                        else -> {
                            Text(
                                text = message.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = textColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTimeShort(message.timestampMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = metaColor,
                        fontSize = 10.sp
                    )
                    if (isMine && !message.isDeletedForAll) {
                        Spacer(modifier = Modifier.width(4.dp))
                        val effectiveStatus = if (!showReadReceipts && message.status == "READ") "DELIVERED" else message.status
                        ReceiptIcon(status = effectiveStatus, tintDefault = metaColor)
                    }
                }
            }
        }
    }
}

@Composable
fun EmojiAndStickerDrawer(
    onEmojiSelected: (String) -> Unit,
    onStickerSelected: (String, String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val emojis = remember {
        listOf(
            "😀", "😂", "😍", "🥳", "😎", "🤩", "🔥", "💚",
            "👍", "🙌", "👏", "🎉", "✨", "🚀", "☕", "🎧",
            "💬", "📞", "🎥", "⚡", "🌟", "💯", "🤝", "🫶"
        )
    }
    val stickers = remember {
        listOf(
            "🚀" to "On my way!",
            "💚" to "SMS Love",
            "🔥" to "So good!",
            "🎉" to "Celebrate",
            "😎" to "Cool vibes",
            "⚡" to "Instant ping",
            "☕" to "Coffee time",
            "🙌" to "Awesome!"
        )
    }

    Surface(
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
    ) {
        Column {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Emojis") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Stickers") }
                )
            }

            if (selectedTab == 0) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(emojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onEmojiSelected(emoji) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 24.sp)
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(stickers) { (code, label) ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { onStickerSelected(code, label) }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                            ) {
                                Text(text = code, fontSize = 28.sp)
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
