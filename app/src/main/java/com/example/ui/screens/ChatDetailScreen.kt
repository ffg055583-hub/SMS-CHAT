package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.CallType
import com.example.data.model.ContactEntity
import com.example.data.model.ConversationEntity
import com.example.data.model.MessageEntity
import com.example.media.VoicePlaybackUiState
import com.example.media.VoiceRecorderUiState
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatDurationTimer
import com.example.ui.components.formatTimeShort
import com.example.ui.theme.CallRejectRed
import com.example.ui.theme.EmeraldVibrant
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    conversation: ConversationEntity?,
    messages: List<MessageEntity>,
    contacts: List<ContactEntity>,
    currentUserId: String,
    isDarkTheme: Boolean,
    showLastSeen: Boolean,
    showReadReceipts: Boolean,
    recorderState: VoiceRecorderUiState,
    playbackState: VoicePlaybackUiState,
    onBack: () -> Unit,
    onMarkRead: () -> Unit,
    onSendText: (String, String?) -> Unit,
    onSendSticker: (String, String) -> Unit,
    onSendMedia: (Uri, Boolean) -> Unit,
    onStartRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onStopAndSendRecording: () -> Unit,
    onToggleVoicePlay: (String, String?) -> Unit,
    onDeleteMessage: (String, Boolean) -> Unit,
    onStartCall: (CallType) -> Unit,
    onUpdateGroupMembers: (String, List<String>) -> Unit,
    onBlockContact: (String, Boolean) -> Unit,
    onReportContact: (String) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val listState = rememberLazyListState()
    var messageInput by remember { mutableStateOf("") }
    var showEmojiDrawer by remember { mutableStateOf(false) }
    var replyToMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var selectedMessageForMenu by remember { mutableStateOf<MessageEntity?>(null) }
    var viewingMediaMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showEditGroupDialog by remember { mutableStateOf(false) }

    val peer = remember(conversation, contacts) {
        conversation?.peerContactId?.let { id -> contacts.find { it.id == id } }
    }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri) ?: ""
            onSendMedia(uri, mime.startsWith("video/"))
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onStartRecording()
        } else {
            Toast.makeText(context, "Microphone permission is needed for voice notes", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(messages.size) {
        onMarkRead()
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val subtitleText = when {
        conversation?.isGroup == true -> {
            val count = conversation.memberIdsCsv.split(",").filter { it.isNotBlank() }.size
            "$count members"
        }
        peer?.isBlocked == true -> "Blocked"
        peer?.isOnline == true -> "Online"
        !showLastSeen -> "SMS CHAT"
        peer != null -> "Last seen ${formatTimeShort(peer.lastSeenMillis)}"
        else -> "Active"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showInfoSheet = true }
                    ) {
                        UserAvatar(
                            name = conversation?.title ?: "Chat",
                            colorHex = conversation?.avatarColorHex ?: 0xFF00C853,
                            avatarUri = conversation?.avatarUri,
                            size = 40.dp,
                            isOnline = peer?.isOnline == true && !peer.isBlocked,
                            isGroup = conversation?.isGroup == true
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = conversation?.title ?: "Chat",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = subtitleText,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (peer?.isOnline == true && !peer.isBlocked) {
                                    EmeraldVibrant
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onStartCall(CallType.VOICE) },
                        enabled = peer?.isBlocked != true,
                        modifier = Modifier.testTag("chat_voice_call_button")
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = "Voice call")
                    }
                    IconButton(
                        onClick = { onStartCall(CallType.VIDEO) },
                        enabled = peer?.isBlocked != true,
                        modifier = Modifier.testTag("chat_video_call_button")
                    ) {
                        Icon(Icons.Filled.Videocam, contentDescription = "Video call")
                    }
                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.testTag("chat_more_button")
                        ) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (conversation?.isGroup == true) "Group Info" else "Contact Info") },
                                leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    showInfoSheet = true
                                }
                            )
                            if (conversation?.isGroup == true) {
                                DropdownMenuItem(
                                    text = { Text("Manage Members") },
                                    leadingIcon = { Icon(Icons.Filled.GroupAdd, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showEditGroupDialog = true
                                    }
                                )
                            } else if (peer != null) {
                                DropdownMenuItem(
                                    text = { Text(if (peer.isBlocked) "Unblock User" else "Block User") },
                                    leadingIcon = { Icon(Icons.Filled.Block, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onBlockContact(peer.id, !peer.isBlocked)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Report & Block") },
                                    leadingIcon = { Icon(Icons.Filled.Report, contentDescription = null, tint = CallRejectRed) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onReportContact(peer.id)
                                        Toast.makeText(context, "User reported and blocked", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_chat_wallpaper),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (isDarkTheme) 0.45f else 0.08f)
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatMessageBubble(
                            message = msg,
                            isMine = msg.senderId == currentUserId,
                            isGroup = conversation?.isGroup == true,
                            isDarkTheme = isDarkTheme,
                            showReadReceipts = showReadReceipts,
                            playbackState = playbackState,
                            onToggleVoicePlay = onToggleVoicePlay,
                            onMediaClick = { viewingMediaMessage = it },
                            onLongPress = { selectedMessageForMenu = it }
                        )
                    }
                }
            }

            AnimatedVisibility(visible = replyToMessage != null) {
                replyToMessage?.let { target ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(28.dp)
                                    .background(EmeraldVibrant, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Replying to ${target.senderName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = target.text.ifBlank { target.type },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { replyToMessage = null }) {
                                Icon(Icons.Filled.Close, contentDescription = "Cancel reply")
                            }
                        }
                    }
                }
            }

            if (peer?.isBlocked == true) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "You blocked ${peer.name}. Unblock to send messages.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(onClick = { onBlockContact(peer.id, false) }) {
                            Text("Unblock")
                        }
                    }
                }
            } else if (recorderState.isRecording) {
                Surface(
                    tonalElevation = 3.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(CallRejectRed)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Recording voice note · ${formatDurationTimer(recorderState.elapsedSec)}",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = onCancelRecording,
                            modifier = Modifier.testTag("cancel_voice_button")
                        ) {
                            Text("Cancel", color = CallRejectRed)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = onStopAndSendRecording,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("send_voice_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send voice note")
                        }
                    }
                }
            } else {
                Surface(
                    tonalElevation = 2.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showEmojiDrawer = !showEmojiDrawer },
                            modifier = Modifier.testTag("toggle_emoji_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEmotions,
                                contentDescription = "Emojis and stickers",
                                tint = if (showEmojiDrawer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedTextField(
                            value = messageInput,
                            onValueChange = { messageInput = it },
                            placeholder = { Text("Message...") },
                            maxLines = 4,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_message_input")
                        )
                        IconButton(
                            onClick = {
                                mediaPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            modifier = Modifier.testTag("attach_media_button")
                        ) {
                            Icon(Icons.Filled.AttachFile, contentDescription = "Attach photo or video")
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                        if (messageInput.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    val quote = replyToMessage?.text
                                    replyToMessage = null
                                    onSendText(messageInput, quote)
                                    messageInput = ""
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .testTag("send_message_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send message",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) {
                                        onStartRecording()
                                    } else {
                                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .testTag("record_voice_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Mic,
                                    contentDescription = "Record voice message",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showEmojiDrawer) {
                EmojiAndStickerDrawer(
                    onEmojiSelected = { emoji -> messageInput += emoji },
                    onStickerSelected = { code, label ->
                        onSendSticker(code, label)
                        showEmojiDrawer = false
                    }
                )
            }
        }
    }

    // Long-press Message Action Dialog
    selectedMessageForMenu?.let { target ->
        AlertDialog(
            onDismissRequest = { selectedMessageForMenu = null },
            title = { Text("Message Options") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = {
                            replyToMessage = target
                            selectedMessageForMenu = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reply", modifier = Modifier.fillMaxWidth())
                    }
                    if (target.text.isNotBlank()) {
                        TextButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Message", target.text))
                                selectedMessageForMenu = null
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Copy Text", modifier = Modifier.fillMaxWidth())
                        }
                    }
                    TextButton(
                        onClick = {
                            onDeleteMessage(target.id, false)
                            selectedMessageForMenu = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete for Me", color = CallRejectRed, modifier = Modifier.fillMaxWidth())
                    }
                    if (target.senderId == currentUserId && !target.isDeletedForAll) {
                        TextButton(
                            onClick = {
                                onDeleteMessage(target.id, true)
                                selectedMessageForMenu = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Delete for Everyone", color = CallRejectRed, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMessageForMenu = null }) { Text("Close") }
            }
        )
    }

    // Fullscreen Photo/Video Viewer
    viewingMediaMessage?.let { mediaMsg ->
        val model: Any? = mediaMsg.mediaUri?.let { if (it.startsWith("/")) File(it) else it }
        AlertDialog(
            onDismissRequest = { viewingMediaMessage = null },
            title = { Text(mediaMsg.senderName) },
            text = {
                AsyncImage(
                    model = model,
                    contentDescription = "Media preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )
            },
            confirmButton = {
                TextButton(onClick = { viewingMediaMessage = null }) { Text("Close") }
            }
        )
    }

    // Contact / Group Info Sheet
    if (showInfoSheet && conversation != null) {
        ModalBottomSheet(onDismissRequest = { showInfoSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                UserAvatar(
                    name = conversation.title,
                    colorHex = conversation.avatarColorHex,
                    avatarUri = conversation.avatarUri,
                    size = 76.dp,
                    isOnline = peer?.isOnline == true,
                    isGroup = conversation.isGroup
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(conversation.title, style = MaterialTheme.typography.headlineSmall)
                if (peer != null) {
                    Text(
                        text = "${peer.phoneNumber} · ${peer.uniqueUserId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(peer.bio, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showInfoSheet = false
                                onBlockContact(peer.id, !peer.isBlocked)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (peer.isBlocked) "Unblock" else "Block")
                        }
                        Button(
                            onClick = {
                                showInfoSheet = false
                                onReportContact(peer.id)
                                Toast.makeText(context, "Reported & blocked", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CallRejectRed),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Report, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Report")
                        }
                    }
                } else if (conversation.isGroup) {
                    val memberIds = conversation.memberIdsCsv.split(",").filter { it.isNotBlank() && it != "me" }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Members (${memberIds.size + 1})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        TextButton(onClick = {
                            showInfoSheet = false
                            showEditGroupDialog = true
                        }) {
                            Icon(Icons.Filled.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Members")
                        }
                    }
                    memberIds.forEach { mId ->
                        val contact = contacts.find { it.id == mId }
                        if (contact != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(
                                    name = contact.name,
                                    colorHex = contact.avatarColorHex,
                                    size = 36.dp,
                                    isOnline = contact.isOnline
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(contact.name, modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = {
                                        val updated = memberIds.filterNot { it == mId }
                                        onUpdateGroupMembers(conversation.title, updated)
                                    }
                                ) {
                                    Icon(Icons.Filled.PersonRemove, contentDescription = "Remove member")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditGroupDialog && conversation?.isGroup == true) {
        val currentIds = conversation.memberIdsCsv.split(",").filter { it.isNotBlank() && it != "me" }
        var titleInput by remember { mutableStateOf(conversation.title) }
        val selected = remember { mutableStateListOf<String>().apply { addAll(currentIds) } }

        AlertDialog(
            onDismissRequest = { showEditGroupDialog = false },
            title = { Text("Manage Group") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Group Name") },
                        singleLine = true
                    )
                    contacts.filter { !it.isBlocked }.forEach { contact ->
                        val checked = contact.id in selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (checked) selected.remove(contact.id) else selected.add(contact.id)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    if (it) selected.add(contact.id) else selected.remove(contact.id)
                                }
                            )
                            Text(contact.name)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showEditGroupDialog = false
                    onUpdateGroupMembers(titleInput.ifBlank { conversation.title }, selected.toList())
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditGroupDialog = false }) { Text("Cancel") }
            }
        )
    }
}
