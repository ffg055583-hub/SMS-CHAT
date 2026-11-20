package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.ThemeMode
import com.example.data.model.CallType
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CallOverlayScreen
import com.example.ui.screens.CallsTabScreen
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.ChatsTabScreen
import com.example.ui.screens.CreateGroupScreen
import com.example.ui.screens.SettingsTabScreen
import com.example.ui.screens.StatusTabScreen
import com.example.ui.theme.SmsChatTheme
import com.example.ui.viewmodel.SmsChatViewModel

private object Routes {
    const val CHATS = "chats"
    const val STATUS = "status"
    const val CALLS = "calls"
    const val SETTINGS = "settings"
    const val CREATE_GROUP = "create_group"
    const val CHAT_DETAIL = "chat_detail/{conversationId}"
    fun chatDetail(id: String) = "chat_detail/$id"
}

private enum class BottomTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    CHATS(Routes.CHATS, "Chats", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
    STATUS(Routes.STATUS, "Status", Icons.Filled.RadioButtonChecked, Icons.Outlined.RadioButtonChecked),
    CALLS(Routes.CALLS, "Calls", Icons.Filled.Call, Icons.Outlined.Call),
    SETTINGS(Routes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as SmsChatApplication
        val factory = SmsChatViewModel.Factory(
            repository = app.repository,
            voiceAudioManager = app.voiceAudioManager,
            callAudioController = app.callAudioController
        )

        setContent {
            val viewModel: SmsChatViewModel = viewModel(factory = factory)
            val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
            val preferences by viewModel.preferences.collectAsStateWithLifecycle()
            val authState by viewModel.authState.collectAsStateWithLifecycle()
            val activeCall by viewModel.activeCall.collectAsStateWithLifecycle()

            val isDark = when (preferences.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            SmsChatTheme(darkTheme = isDark) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (currentUser == null) {
                        AuthScreen(
                            state = authState,
                            onCountryCodeChange = viewModel::updateCountryCode,
                            onPhoneChange = viewModel::updatePhoneNumber,
                            onRequestOtp = viewModel::requestOtp,
                            onOtpInputChange = viewModel::updateOtpInput,
                            onAutofillOtp = viewModel::autofillOtp,
                            onResendOtp = viewModel::resendOtp,
                            onVerifyOtp = viewModel::verifyOtp,
                            onBackToPhone = viewModel::backToPhoneEntry,
                            onNameChange = viewModel::updateRegistrationName,
                            onBioChange = viewModel::updateRegistrationBio,
                            onAvatarSelected = viewModel::updateRegistrationAvatar,
                            onColorSelected = viewModel::updateRegistrationColor,
                            onCompleteRegistration = viewModel::completeRegistration
                        )
                    } else {
                        SmsChatMainApp(
                            viewModel = viewModel,
                            isDarkTheme = isDark
                        )
                    }

                    activeCall?.let { call ->
                        CallOverlayScreen(
                            callState = call,
                            onAccept = viewModel::acceptIncomingCall,
                            onRejectOrEnd = viewModel::rejectOrEndCall,
                            onToggleMute = viewModel::toggleCallMute,
                            onToggleSpeaker = viewModel::toggleCallSpeaker,
                            onToggleCamera = viewModel::toggleCallCamera,
                            onFlipCamera = viewModel::flipCallCamera
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmsChatMainApp(
    viewModel: SmsChatViewModel,
    isDarkTheme: Boolean
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val statuses by viewModel.activeStatuses.collectAsStateWithLifecycle()
    val callLogs by viewModel.callLogs.collectAsStateWithLifecycle()
    val recorderState by viewModel.voiceRecorderState.collectAsStateWithLifecycle()
    val playbackState by viewModel.voicePlaybackState.collectAsStateWithLifecycle()

    val showBottomBar = currentRoute in BottomTab.entries.map { it.route }
    val totalUnread = remember(conversations) { conversations.sumOf { it.unreadCount } }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    BottomTab.entries.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(tab.route) {
                                        popUpTo(Routes.CHATS) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (tab == BottomTab.CHATS && totalUnread > 0) {
                                            Badge { Text(totalUnread.toString()) }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                }
                            },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_${tab.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.CHATS,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Routes.CHATS) {
                ChatsTabScreen(
                    conversations = conversations,
                    contacts = contacts,
                    onOpenConversation = { convId ->
                        navController.navigate(Routes.chatDetail(convId))
                    },
                    onOpenContactChat = { contact ->
                        viewModel.openOrCreateDirectChat(contact) { convId ->
                            navController.navigate(Routes.chatDetail(convId))
                        }
                    },
                    onAddContactAndChat = { name, phone ->
                        viewModel.addContactAndOpenChat(name, phone) { convId ->
                            navController.navigate(Routes.chatDetail(convId))
                        }
                    },
                    onCreateGroupClick = {
                        navController.navigate(Routes.CREATE_GROUP)
                    },
                    onTogglePin = viewModel::togglePinConversation
                )
            }

            composable(Routes.STATUS) {
                StatusTabScreen(
                    currentUser = currentUser,
                    statuses = statuses,
                    onPostTextStatus = viewModel::postTextStatus,
                    onPostMediaStatus = viewModel::postMediaStatus,
                    onMarkViewed = viewModel::markStatusViewed,
                    onDeleteStatus = viewModel::deleteStatus
                )
            }

            composable(Routes.CALLS) {
                CallsTabScreen(
                    callLogs = callLogs,
                    contacts = contacts,
                    onStartDirectCall = { contact, callType ->
                        viewModel.startCall(
                            peerId = contact.id,
                            peerName = contact.name,
                            peerAvatarColorHex = contact.avatarColorHex,
                            callType = callType
                        )
                    },
                    onStartGroupCall = { title, participants, callType ->
                        viewModel.startCall(
                            peerId = "grp_call",
                            peerName = title,
                            peerAvatarColorHex = 0xFF00C853,
                            callType = callType,
                            isGroup = true,
                            participantNames = participants.map { it.name }
                        )
                    },
                    onSimulateIncomingCall = { contact, callType ->
                        viewModel.triggerIncomingCall(
                            peerId = contact.id,
                            peerName = contact.name,
                            peerAvatarColorHex = contact.avatarColorHex,
                            callType = callType
                        )
                    },
                    onDeleteCallLog = viewModel::deleteCallLog
                )
            }

            composable(Routes.SETTINGS) {
                SettingsTabScreen(
                    currentUser = currentUser,
                    preferences = preferences,
                    contacts = contacts,
                    onThemeModeChange = viewModel::setThemeMode,
                    onUpdateProfile = viewModel::updateProfile,
                    onUpdatePrivacy = viewModel::updatePrivacy,
                    onUpdateNotifications = viewModel::updateNotifications,
                    onUnblockContact = { id -> viewModel.setContactBlocked(id, false) },
                    onSignOut = viewModel::signOut
                )
            }

            composable(Routes.CREATE_GROUP) {
                CreateGroupScreen(
                    contacts = contacts,
                    onBack = { navController.popBackStack() },
                    onCreateGroup = { title, memberIds ->
                        viewModel.createGroupChat(title, memberIds) { convId ->
                            navController.popBackStack()
                            navController.navigate(Routes.chatDetail(convId))
                        }
                    }
                )
            }

            composable(
                route = Routes.CHAT_DETAIL,
                arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
            ) { entry ->
                val convId = entry.arguments?.getString("conversationId") ?: return@composable
                val conv by viewModel.observeConversation(convId)
                    .collectAsStateWithLifecycle(initialValue = conversations.find { it.id == convId })
                val messages by viewModel.observeMessages(convId)
                    .collectAsStateWithLifecycle(initialValue = emptyList())

                ChatDetailScreen(
                    conversation = conv,
                    messages = messages,
                    contacts = contacts,
                    currentUserId = currentUser?.id ?: "me",
                    isDarkTheme = isDarkTheme,
                    showLastSeen = preferences.privacy.showLastSeen,
                    showReadReceipts = preferences.privacy.readReceipts,
                    recorderState = recorderState,
                    playbackState = playbackState,
                    onBack = { navController.popBackStack() },
                    onMarkRead = { viewModel.markConversationRead(convId) },
                    onSendText = { text, quote -> viewModel.sendTextMessage(convId, text, quote) },
                    onSendSticker = { code, label -> viewModel.sendStickerMessage(convId, code, label) },
                    onSendMedia = { uri, isVideo -> viewModel.sendMediaMessage(convId, uri, isVideo) },
                    onStartRecording = viewModel::startVoiceRecording,
                    onCancelRecording = viewModel::cancelVoiceRecording,
                    onStopAndSendRecording = { viewModel.stopAndSendVoiceNote(convId) },
                    onToggleVoicePlay = viewModel::toggleVoicePlayback,
                    onDeleteMessage = viewModel::deleteMessage,
                    onStartCall = { callType ->
                        val c = conv ?: return@ChatDetailScreen
                        val memberNames = if (c.isGroup) {
                            c.memberIdsCsv.split(",")
                                .filter { it.isNotBlank() && it != "me" }
                                .mapNotNull { id -> contacts.find { it.id == id }?.name }
                        } else emptyList()
                        viewModel.startCall(
                            peerId = c.peerContactId ?: c.id,
                            peerName = c.title,
                            peerAvatarColorHex = c.avatarColorHex,
                            callType = callType,
                            isGroup = c.isGroup,
                            participantNames = memberNames
                        )
                    },
                    onUpdateGroupMembers = { newTitle, memberIds ->
                        viewModel.updateGroupMembers(convId, newTitle, memberIds)
                    },
                    onBlockContact = viewModel::setContactBlocked,
                    onReportContact = viewModel::reportAndBlockContact
                )
            }
        }
    }
}
