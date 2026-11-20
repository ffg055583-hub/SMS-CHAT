package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MessageStatus
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldVibrant
import com.example.ui.theme.ReadTickEmerald
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UserAvatar(
    name: String,
    colorHex: Long,
    avatarUri: String? = null,
    size: Dp = 48.dp,
    isOnline: Boolean = false,
    isGroup: Boolean = false,
    hasUnviewedStatus: Boolean = false,
    modifier: Modifier = Modifier
) {
    val baseColor = Color(colorHex)
    val darkerColor = Color(
        red = (baseColor.red * 0.65f).coerceIn(0f, 1f),
        green = (baseColor.green * 0.65f).coerceIn(0f, 1f),
        blue = (baseColor.blue * 0.65f).coerceIn(0f, 1f),
        alpha = 1f
    )
    val initials = name.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "S" }

    Box(modifier = modifier.size(size)) {
        val ringModifier = if (hasUnviewedStatus) {
            Modifier
                .fillMaxSize()
                .border(2.5.dp, EmeraldVibrant, CircleShape)
                .padding(3.dp)
        } else {
            Modifier.fillMaxSize()
        }

        Box(
            modifier = ringModifier
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(baseColor, darkerColor))),
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUri.isNullOrBlank()) {
                val model: Any = if (avatarUri.startsWith("/")) File(avatarUri) else avatarUri
                AsyncImage(
                    model = model,
                    contentDescription = "$name avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (isGroup) {
                Icon(
                    imageVector = Icons.Filled.Group,
                    contentDescription = "$name group avatar",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.5f)
                )
            } else {
                Text(
                    text = initials,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.36f).sp
                )
            }
        }

        if (isOnline && !isGroup) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size((size.value * 0.28f).dp.coerceAtLeast(11.dp))
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(EmeraldVibrant)
            )
        }
    }
}

@Composable
fun ReceiptIcon(
    status: String,
    tintDefault: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier
) {
    val parsed = runCatching { MessageStatus.valueOf(status) }.getOrDefault(MessageStatus.SENT)
    when (parsed) {
        MessageStatus.SENT -> Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = "Sent",
            tint = tintDefault,
            modifier = modifier.size(15.dp)
        )
        MessageStatus.DELIVERED -> Icon(
            imageVector = Icons.Filled.DoneAll,
            contentDescription = "Delivered",
            tint = tintDefault,
            modifier = modifier.size(15.dp)
        )
        MessageStatus.READ -> Icon(
            imageVector = Icons.Filled.DoneAll,
            contentDescription = "Read",
            tint = ReadTickEmerald,
            modifier = modifier.size(15.dp)
        )
    }
}

@Composable
fun SmsChatBrandPill(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                Brush.horizontalGradient(listOf(EmeraldVibrant, EmeraldDeep))
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(Color(0xFF070B09))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "SMS CHAT",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF070B09)
        )
    }
}

fun formatTimeShort(millis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - millis
    return when {
        diff < 24 * 3_600_000L -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))
        diff < 48 * 3_600_000L -> "Yesterday"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(millis))
    }
}

fun formatDurationTimer(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}
