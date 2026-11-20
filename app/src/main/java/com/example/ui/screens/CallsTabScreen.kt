package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.RingVolume
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CallDirection
import com.example.data.model.CallLogEntity
import com.example.data.model.CallType
import com.example.data.model.ContactEntity
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatDurationTimer
import com.example.ui.components.formatTimeShort
import com.example.ui.theme.CallAcceptGreen
import com.example.ui.theme.CallRejectRed

@Composable
fun CallsTabScreen(
    callLogs: List<CallLogEntity>,
    contacts: List<ContactEntity>,
    onStartDirectCall: (ContactEntity, CallType) -> Unit,
    onStartGroupCall: (String, List<ContactEntity>, CallType) -> Unit,
    onSimulateIncomingCall: (ContactEntity, CallType) -> Unit,
    onDeleteCallLog: (String) -> Unit
) {
    var showGroupCallDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
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
                        text = "Voice & Video Calls",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "HD 1-to-1 and group calls",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Quick Call Controls Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showGroupCallDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("start_group_call_button")
                        ) {
                            Icon(Icons.Filled.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Group Call")
                        }
                        OutlinedButton(
                            onClick = {
                                val caller = contacts.firstOrNull { !it.isBlocked } ?: return@OutlinedButton
                                onSimulateIncomingCall(caller, CallType.VIDEO)
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_incoming_call_button")
                        ) {
                            Icon(Icons.Filled.RingVolume, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Incoming Demo")
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Call History",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (callLogs.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No calls yet. Start a voice or video call from any chat!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(callLogs, key = { it.id }) { log ->
                CallLogRow(
                    log = log,
                    onRedialVoice = {
                        val contact = contacts.find { it.id == log.peerId }
                        if (contact != null) {
                            onStartDirectCall(contact, CallType.VOICE)
                        } else if (log.isGroup) {
                            val groupMembers = contacts.filter { !it.isBlocked }.take(3)
                            onStartGroupCall(log.peerName, groupMembers, CallType.VOICE)
                        }
                    },
                    onRedialVideo = {
                        val contact = contacts.find { it.id == log.peerId }
                        if (contact != null) {
                            onStartDirectCall(contact, CallType.VIDEO)
                        } else if (log.isGroup) {
                            val groupMembers = contacts.filter { !it.isBlocked }.take(3)
                            onStartGroupCall(log.peerName, groupMembers, CallType.VIDEO)
                        }
                    },
                    onDelete = { onDeleteCallLog(log.id) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }

    if (showGroupCallDialog) {
        val selected = remember { mutableStateListOf<ContactEntity>() }
        AlertDialog(
            onDismissRequest = { showGroupCallDialog = false },
            title = { Text("Start Group Call") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Pick contacts to invite:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    contacts.filter { !it.isBlocked }.forEach { contact ->
                        val isChecked = contact in selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) selected.remove(contact) else selected.add(contact)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    if (it) selected.add(contact) else selected.remove(contact)
                                }
                            )
                            Text(contact.name)
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (selected.isNotEmpty()) {
                                showGroupCallDialog = false
                                onStartGroupCall("Group Voice Call", selected.toList(), CallType.VOICE)
                            }
                        },
                        enabled = selected.isNotEmpty()
                    ) {
                        Text("Voice")
                    }
                    Button(
                        onClick = {
                            if (selected.isNotEmpty()) {
                                showGroupCallDialog = false
                                onStartGroupCall("Group Video Call", selected.toList(), CallType.VIDEO)
                            }
                        },
                        enabled = selected.isNotEmpty()
                    ) {
                        Text("Video")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showGroupCallDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CallLogRow(
    log: CallLogEntity,
    onRedialVoice: () -> Unit,
    onRedialVideo: () -> Unit,
    onDelete: () -> Unit
) {
    val direction = runCatching { CallDirection.valueOf(log.direction) }.getOrDefault(CallDirection.OUTGOING)
    val callType = runCatching { CallType.valueOf(log.callType) }.getOrDefault(CallType.VOICE)

    val dirIcon = when (direction) {
        CallDirection.INCOMING -> Icons.AutoMirrored.Filled.CallReceived
        CallDirection.OUTGOING -> Icons.AutoMirrored.Filled.CallMade
        CallDirection.MISSED -> Icons.AutoMirrored.Filled.CallMissed
    }
    val dirColor = if (direction == CallDirection.MISSED) CallRejectRed else CallAcceptGreen

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                name = log.peerName,
                colorHex = log.peerAvatarColorHex,
                size = 48.dp,
                isGroup = log.isGroup
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.peerName,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (direction == CallDirection.MISSED) CallRejectRed else MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = dirIcon,
                        contentDescription = null,
                        tint = dirColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val durText = if (log.durationSec > 0) " · ${formatDurationTimer(log.durationSec)}" else ""
                    Text(
                        text = "${formatTimeShort(log.timestampMillis)}$durText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = if (callType == CallType.VIDEO) onRedialVideo else onRedialVoice) {
                Icon(
                    imageVector = if (callType == CallType.VIDEO) Icons.Filled.Videocam else Icons.Filled.Call,
                    contentDescription = "Redial",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Remove call log",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
