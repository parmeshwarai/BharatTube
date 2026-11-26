package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.firebase.BharatTubeRepository
import com.example.data.model.Channel
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.DeviceSession
import com.example.data.model.GlobalCatalog
import com.example.data.model.LiveChatMessage
import com.example.data.model.LiveStream
import com.example.data.model.NotificationItem
import com.example.data.model.PlatformConfig
import com.example.data.model.Playlist
import com.example.data.model.SubscriptionItem
import com.example.data.model.SupportTicket
import com.example.data.model.UserProfile
import com.example.data.model.Video
import com.example.data.model.WatchHistoryItem
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.PeacockTeal
import com.example.ui.theme.SaffronPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityHubScreen(
    user: UserProfile,
    config: PlatformConfig,
    isAdmin: Boolean,
    myChannels: List<Channel>,
    activeChannel: Channel?,
    myVideos: List<Video>,
    subscriptions: List<SubscriptionItem>,
    watchHistory: List<WatchHistoryItem>,
    playlists: List<Playlist>,
    liveStreams: List<LiveStream>,
    liveMessages: List<LiveChatMessage>,
    chats: List<ChatThread>,
    chatMessages: List<ChatMessage>,
    notifications: List<NotificationItem>,
    sessions: List<DeviceSession>,
    supportTickets: List<SupportTicket>,
    onOpenVideo: (String) -> Unit,
    onOpenAdmin: () -> Unit,
    onSwitchChannel: (String) -> Unit,
    onOpenCreateChannel: () -> Unit,
    onOpenCustomizeChannel: (Channel) -> Unit,
    onVerifyOwnership: (String) -> Unit,
    onCreatePlaylist: (String, String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onClearHistory: () -> Unit,
    onUpdateUserSettings: (Map<String, Any>, String?) -> Unit,
    onStartLive: (String, String) -> Unit,
    onSelectLive: (String?) -> Unit,
    onSendLiveMessage: (String, String) -> Unit,
    onBlockLiveUser: (String, String) -> Unit,
    onEndLive: (String) -> Unit,
    onCreateChat: (String, Boolean) -> Unit,
    onSelectChat: (String?) -> Unit,
    onSendChatMessage: (ChatThread, String, String, String) -> Unit,
    onMarkNotificationRead: (String) -> Unit,
    onSubmitTicket: (String, String, String) -> Unit,
    onSignOut: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Channel", "Library", "Live & Chat", "Alerts", "Help", "Settings")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Community & Profile", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "${user.displayName} • ${user.bharatTubeId.ifBlank { user.handle }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PeacockTeal
                )
            }
            if (isAdmin) {
                Button(
                    onClick = onOpenAdmin,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldHighlight),
                    modifier = Modifier.testTag("hub_open_admin_button")
                ) {
                    Icon(Icons.Default.Shield, contentDescription = "Admin", tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Admin", color = Color.Black)
                }
            }
        }

        PrimaryScrollableTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { idx, label ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(label) },
                    modifier = Modifier.testTag("hub_tab_$idx")
                )
            }
        }

        when (selectedTab) {
            0 -> ChannelManagementSection(
                user = user,
                myChannels = myChannels,
                activeChannel = activeChannel,
                myVideos = myVideos,
                maxChannels = BharatTubeRepository.MAX_CHANNELS_PER_ACCOUNT,
                onSwitchChannel = onSwitchChannel,
                onCreateChannel = onOpenCreateChannel,
                onCustomizeChannel = onOpenCustomizeChannel,
                onVerifyOwnership = onVerifyOwnership,
                onOpenVideo = onOpenVideo
            )
            1 -> LibrarySection(user, subscriptions, watchHistory, playlists, onOpenVideo, onCreatePlaylist, onDeletePlaylist, onClearHistory, onUpdateUserSettings)
            2 -> LiveAndChatSection(
                config = config,
                user = user,
                liveStreams = liveStreams,
                liveMessages = liveMessages,
                chats = chats,
                chatMessages = chatMessages,
                onStartLive = onStartLive,
                onSelectLive = onSelectLive,
                onSendLiveMessage = onSendLiveMessage,
                onBlockLiveUser = onBlockLiveUser,
                onEndLive = onEndLive,
                onCreateChat = onCreateChat,
                onSelectChat = onSelectChat,
                onSendChatMessage = onSendChatMessage
            )
            3 -> NotificationsSection(user, notifications, onMarkNotificationRead, onUpdateUserSettings)
            4 -> HelpAndSupportSection(supportTickets, onSubmitTicket)
            5 -> SettingsAndSafetySection(user, sessions, onUpdateUserSettings, onSubmitTicket, onSignOut)
        }
    }
}

@Composable
private fun LibrarySection(
    user: UserProfile,
    subscriptions: List<SubscriptionItem>,
    watchHistory: List<WatchHistoryItem>,
    playlists: List<Playlist>,
    onOpenVideo: (String) -> Unit,
    onCreatePlaylist: (String, String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onClearHistory: () -> Unit,
    onUpdateUserSettings: (Map<String, Any>, String?) -> Unit
) {
    var showCreatePlaylist by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Subscriptions (${subscriptions.count { it.subscribed }})", style = MaterialTheme.typography.titleMedium)
            }
            if (subscriptions.none { it.subscribed }) {
                Text("Subscribe to creators while watching videos or Shorts.", style = MaterialTheme.typography.bodySmall)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(subscriptions.filter { it.subscribed }) { sub ->
                        AssistChip(onClick = {}, label = { Text(sub.channelName) })
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Playlists & Watch Later (${playlists.size})", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = { showCreatePlaylist = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Create playlist")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New")
                }
            }
        }
        items(playlists, key = { it.id }) { pl ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(pl.title, style = MaterialTheme.typography.titleSmall)
                        Text("${pl.privacy} • ${pl.videoIds.size} videos", style = MaterialTheme.typography.bodySmall)
                    }
                    if (pl.id != "watch_later") {
                        IconButton(onClick = { onDeletePlaylist(pl.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete playlist")
                        }
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Watch History (${watchHistory.size})", style = MaterialTheme.typography.titleMedium)
                Row {
                    TextButton(onClick = {
                        onUpdateUserSettings(mapOf("watchHistoryPaused" to !user.watchHistoryPaused), "Watch history preference updated")
                    }) {
                        Text(if (user.watchHistoryPaused) "Resume" else "Pause")
                    }
                    TextButton(onClick = onClearHistory) { Text("Clear") }
                }
            }
        }
        items(watchHistory, key = { it.id }) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenVideo(item.videoId) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall)
                    Text("${item.creatorName} • ${item.positionMs / 1000}s watched", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    if (showCreatePlaylist) {
        var title by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var privacy by remember { mutableStateOf("PRIVATE") }
        AlertDialog(
            onDismissRequest = { showCreatePlaylist = false },
            title = { Text("Create Playlist") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("PRIVATE", "PUBLIC").forEach { p ->
                            FilterChip(selected = privacy == p, onClick = { privacy = p }, label = { Text(p) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank()) onCreatePlaylist(title, desc, privacy)
                    showCreatePlaylist = false
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylist = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun LiveAndChatSection(
    config: PlatformConfig,
    user: UserProfile,
    liveStreams: List<LiveStream>,
    liveMessages: List<LiveChatMessage>,
    chats: List<ChatThread>,
    chatMessages: List<ChatMessage>,
    onStartLive: (String, String) -> Unit,
    onSelectLive: (String?) -> Unit,
    onSendLiveMessage: (String, String) -> Unit,
    onBlockLiveUser: (String, String) -> Unit,
    onEndLive: (String) -> Unit,
    onCreateChat: (String, Boolean) -> Unit,
    onSelectChat: (String?) -> Unit,
    onSendChatMessage: (ChatThread, String, String, String) -> Unit
) {
    var activeLiveId by remember { mutableStateOf<String?>(null) }
    var activeChatId by remember { mutableStateOf<String?>(null) }
    var liveTitle by remember { mutableStateOf("") }
    var chatTitle by remember { mutableStateOf("") }
    var msgInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LiveTv, contentDescription = "Go Live", tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Live Broadcast", style = MaterialTheme.typography.titleMedium)
                    }
                    OutlinedTextField(
                        value = liveTitle,
                        onValueChange = { liveTitle = it },
                        label = { Text("Live Stream Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onStartLive(liveTitle.ifBlank { "${user.displayName} Live Q&A" }, "Live on BharatTube")
                            liveTitle = ""
                        },
                        enabled = config.liveStreamingEnabled && !user.minorMode,
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        modifier = Modifier.testTag("go_live_button")
                    ) {
                        Text("Go Live Now", color = Color.White)
                    }
                }
            }
        }

        items(liveStreams, key = { it.id }) { stream ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        activeLiveId = stream.id
                        onSelectLive(stream.id)
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("LIVE: ${stream.title}", style = MaterialTheme.typography.titleSmall)
                        Text("${stream.viewerCount} watching", color = PeacockTeal, style = MaterialTheme.typography.labelSmall)
                    }
                    if (activeLiveId == stream.id) {
                        liveMessages.takeLast(6).forEach { m ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${m.senderName}: ${m.text}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                if (stream.creatorId == user.userId && m.userId != user.userId) {
                                    IconButton(onClick = { onBlockLiveUser(stream.id, m.userId) }) {
                                        Icon(Icons.Default.Block, contentDescription = "Block user", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = msgInput,
                                onValueChange = { msgInput = it },
                                placeholder = { Text("Say something live...") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            IconButton(onClick = {
                                onSendLiveMessage(stream.id, msgInput)
                                msgInput = ""
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send live chat")
                            }
                        }
                        if (stream.creatorId == user.userId) {
                            OutlinedButton(onClick = { onEndLive(stream.id) }) { Text("End Broadcast") }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text("Social & Community Chats", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = chatTitle,
                    onValueChange = { chatTitle = it },
                    label = { Text("New 1:1 or Group Chat") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(
                    onClick = {
                        onCreateChat(chatTitle.ifBlank { "Creator Circle" }, true)
                        chatTitle = ""
                    },
                    enabled = config.chatEnabled && !user.minorMode
                ) {
                    Text("Create")
                }
            }
        }

        items(chats, key = { it.id }) { thread ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        activeChatId = thread.id
                        onSelectChat(thread.id)
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(thread.title, style = MaterialTheme.typography.titleSmall)
                    Text(thread.lastMessage.ifBlank { "Tap to open chat" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (activeChatId == thread.id) {
                        chatMessages.takeLast(8).forEach { msg ->
                            Text(
                                text = "[${msg.messageType}] ${msg.senderName}: ${msg.text}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = msgInput,
                                onValueChange = { msgInput = it },
                                placeholder = { Text("Message...") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            IconButton(onClick = {
                                onSendChatMessage(thread, "Voice Note (0:08)", "VOICE_NOTE", "voice_clip_08s.m4a")
                            }) {
                                Icon(Icons.Default.Mic, contentDescription = "Send voice note", tint = PeacockTeal)
                            }
                            IconButton(onClick = {
                                onSendChatMessage(thread, msgInput, "TEXT", "")
                                msgInput = ""
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationsSection(
    user: UserProfile,
    notifications: List<NotificationItem>,
    onMarkRead: (String) -> Unit,
    onUpdateSettings: (Map<String, Any>, String?) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Notification Preferences", style = MaterialTheme.typography.titleSmall)
                    HubToggleRow("New Uploads & Subscriptions", user.notifUploads) {
                        onUpdateSettings(mapOf("notifUploads" to it), null)
                    }
                    HubToggleRow("Comments & Replies", user.notifComments) {
                        onUpdateSettings(mapOf("notifComments" to it), null)
                    }
                    HubToggleRow("Earnings & Automatic Payouts", user.notifPayouts) {
                        onUpdateSettings(mapOf("notifPayouts" to it), null)
                    }
                    HubToggleRow("Security & Login Alerts", user.notifSecurity) {
                        onUpdateSettings(mapOf("notifSecurity" to it), null)
                    }
                }
            }
        }
        items(notifications, key = { it.id }) { notif ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMarkRead(notif.id) },
                colors = CardDefaults.cardColors(
                    containerColor = if (notif.read) MaterialTheme.colorScheme.surfaceContainer
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("${notif.category} • ${notif.title}", style = MaterialTheme.typography.titleSmall)
                    Text(notif.body, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun HelpAndSupportSection(
    tickets: List<SupportTicket>,
    onSubmitTicket: (String, String, String) -> Unit
) {
    var category by remember { mutableStateOf("MONETIZATION") }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Help, contentDescription = "Help Center", tint = PeacockTeal)
                Spacer(modifier = Modifier.width(8.dp))
                Text("FAQs & Creator Guides", style = MaterialTheme.typography.titleMedium)
            }
        }
        items(GlobalCatalog.faqs, key = { it.id }) { faq ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${faq.category}: ${faq.question}", style = MaterialTheme.typography.titleSmall)
                    Text(faq.answer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Submit Support / Account Recovery Ticket", style = MaterialTheme.typography.titleSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("MONETIZATION", "COPYRIGHT", "ACCOUNT_RECOVERY", "BUG_REPORT")) { cat ->
                            FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                        }
                    }
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Details") }, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            if (subject.isNotBlank()) {
                                onSubmitTicket(category, subject, description)
                                subject = ""
                                description = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Submit Ticket")
                    }
                }
            }
        }

        items(tickets, key = { it.id }) { t ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("${t.category} • ${t.subject} [${t.status}]", style = MaterialTheme.typography.titleSmall)
                    if (t.resolutionNote.isNotBlank()) {
                        Text("Admin Note: ${t.resolutionNote}", style = MaterialTheme.typography.bodySmall, color = PeacockTeal)
                    }
                }
            }
        }
    }
}
