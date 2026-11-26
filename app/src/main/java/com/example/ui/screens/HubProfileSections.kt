package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.Channel
import com.example.data.model.DeviceSession
import com.example.data.model.GlobalCatalog
import com.example.data.model.UserProfile
import com.example.data.model.Video
import com.example.ui.components.BharatTubeAvatar
import com.example.ui.components.VideoFeedCard
import com.example.ui.components.channelAccentColor
import com.example.ui.components.formatCompactCount
import com.example.ui.components.resolveThumbnailDrawable
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.PeacockTeal
import com.example.ui.theme.SaffronPrimary

@Composable
fun ChannelManagementSection(
    user: UserProfile,
    myChannels: List<Channel>,
    activeChannel: Channel?,
    myVideos: List<Video>,
    maxChannels: Int,
    onSwitchChannel: (String) -> Unit,
    onCreateChannel: () -> Unit,
    onCustomizeChannel: (Channel) -> Unit,
    onVerifyOwnership: (String) -> Unit,
    onOpenVideo: (String) -> Unit
) {
    var filterToActive by remember { mutableStateOf(true) }
    val shownVideos = remember(myVideos, activeChannel?.id, filterToActive) {
        if (!filterToActive || activeChannel == null) myVideos
        else myVideos.filter { it.channelId.isBlank() || it.channelId == activeChannel.id }
    }
    val accent = channelAccentColor(activeChannel?.appearanceTheme ?: "SAFFRON")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Multi-Channel Switcher & Create CTA
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("My Channels (${myChannels.size}/$maxChannels)", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Manage multiple channels under BharatTube ID ${user.bharatTubeId.ifBlank { user.userId.take(10) }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (myChannels.size < maxChannels) {
                            Button(
                                onClick = onCreateChannel,
                                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                                modifier = Modifier.testTag("hub_create_channel_button")
                            ) {
                                Icon(Icons.Default.AddCircle, contentDescription = "Create Channel", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("New", color = Color.White)
                            }
                        }
                    }

                    if (myChannels.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(myChannels, key = { it.id }) { ch ->
                                val selected = ch.id == activeChannel?.id
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSwitchChannel(ch.id) },
                                    leadingIcon = if (selected) {
                                        { Icon(Icons.Default.Check, contentDescription = "Active channel", modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    label = { Text("${ch.name} (${ch.handle})") },
                                    modifier = Modifier.testTag("switch_channel_${ch.id}")
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Channel Showcase Card
        item {
            val ch = activeChannel
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.border(1.dp, accent.copy(alpha = 0.5f), MaterialTheme.shapes.large)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(132.dp)
                    ) {
                        val bannerUrl = ch?.bannerUrl.orEmpty()
                        if (bannerUrl.isNotBlank()) {
                            AsyncImage(
                                model = bannerUrl,
                                contentDescription = "Channel banner",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Image(
                                painter = painterResource(id = resolveThumbnailDrawable(ch?.bannerKey ?: "img_channel_banner")),
                                contentDescription = "Channel banner",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                                    )
                                )
                        )
                        Surface(
                            color = accent,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "${ch?.category ?: "Technology"} • ${ch?.appearanceTheme ?: "SAFFRON"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                BharatTubeAvatar(
                                    name = ch?.name ?: user.displayName,
                                    photoUrl = ch?.avatarUrl?.ifBlank { user.photoUrl } ?: user.photoUrl,
                                    fallbackKey = ch?.avatarKey ?: "img_hero_bharattube",
                                    accentColor = accent,
                                    size = 52.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(ch?.name ?: user.displayName, style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        text = "${ch?.handle ?: user.handle} • ${formatCompactCount(ch?.subscriberCount ?: 0L)} subscribers • ${formatCompactCount(ch?.totalViews ?: 0L)} views",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = accent
                                    )
                                }
                            }
                            if (ch != null) {
                                OutlinedButton(
                                    onClick = { onCustomizeChannel(ch) },
                                    modifier = Modifier.testTag("customize_active_channel_button")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Customize", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit")
                                }
                            }
                        }

                        Text(ch?.description?.ifBlank { user.bio } ?: user.bio, style = MaterialTheme.typography.bodyMedium)

                        // Ownership Verification Badge / Trigger
                        if (ch != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        Icons.Default.VerifiedUser,
                                        contentDescription = "Ownership status",
                                        tint = if (ch.ownershipVerified) PeacockTeal else GoldHighlight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (ch.ownershipVerified) {
                                            ch.ownershipProofLabel.ifBlank { "Google OAuth Verified Owner" }
                                        } else {
                                            "Channel ownership unverified"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (ch.ownershipVerified) PeacockTeal else GoldHighlight,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (!ch.ownershipVerified) {
                                    TextButton(onClick = { onVerifyOwnership(ch.id) }) {
                                        Text("Verify Ownership")
                                    }
                                }
                            }
                        }

                        // Social Links
                        if (ch != null && ch.socialLinks.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(ch.socialLinks) { link ->
                                    AssistChip(
                                        onClick = {},
                                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = "Social link", modifier = Modifier.size(14.dp)) },
                                        label = { Text(link.removePrefix("https://").removePrefix("http://").take(28)) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Videos & Shorts (${shownVideos.size})", style = MaterialTheme.typography.titleMedium)
                FilterChip(
                    selected = filterToActive,
                    onClick = { filterToActive = !filterToActive },
                    label = { Text(if (filterToActive) "Active Channel" else "All Channels") }
                )
            }
        }
        items(shownVideos, key = { it.id }) { vid ->
            VideoFeedCard(video = vid, onClick = { onOpenVideo(vid.id) })
        }
    }
}

@Composable
fun SettingsAndSafetySection(
    user: UserProfile,
    sessions: List<DeviceSession>,
    onUpdateSettings: (Map<String, Any>, String?) -> Unit,
    onSubmitTicket: (String, String, String) -> Unit,
    onSignOut: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var editName by remember(user.displayName) { mutableStateOf(user.displayName) }
    var editBio by remember(user.bio) { mutableStateOf(user.bio) }
    var editPhone by remember(user.phone) { mutableStateOf(user.phone) }
    var editPhotoUrl by remember(user.photoUrl) { mutableStateOf(user.photoUrl) }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            editPhotoUrl = uri.toString()
            onUpdateSettings(mapOf("photoUrl" to uri.toString()), "BharatTube profile photo updated")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // BharatTube Account & Linked Google Identity Card
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BharatTubeAvatar(
                            name = user.displayName,
                            photoUrl = editPhotoUrl,
                            size = 54.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("BharatTube Account & Google Link", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "ID: ${user.bharatTubeId.ifBlank { user.userId.take(12) }}",
                                style = MaterialTheme.typography.labelLarge,
                                color = PeacockTeal
                            )
                            Text(
                                text = "Google Linked: ${user.email.ifBlank { "OAuth Session" }} (1:1 duplicate-protected)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Image, contentDescription = "Change profile photo")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Change Profile Photo")
                    }

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("BharatTube Profile Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("Profile Bio") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Recovery Mobile Number (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                onUpdateSettings(
                                    mapOf(
                                        "displayName" to editName.trim().take(60).ifBlank { user.displayName },
                                        "bio" to editBio.trim().take(300),
                                        "phone" to editPhone.trim().take(24)
                                    ),
                                    "Account profile saved!"
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save Profile")
                        }
                        OutlinedButton(
                            onClick = { showRecoveryDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.LockReset, contentDescription = "Account recovery", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Recovery")
                        }
                    }
                }
            }
        }

        // 20+ Languages Selector
        item {
            Text("Global Language (${GlobalCatalog.starterLanguages.size} Languages)", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(GlobalCatalog.starterLanguages) { lang ->
                    FilterChip(
                        selected = user.languageCode == lang.code,
                        onClick = {
                            onUpdateSettings(mapOf("languageCode" to lang.code), "Language switched to ${lang.nativeName}")
                        },
                        label = { Text(lang.nativeName) },
                        modifier = Modifier.testTag("lang_chip_${lang.code}")
                    )
                }
            }
        }

        // Country & Currency
        item {
            Text("Country & Local Currency", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(GlobalCatalog.countries) { c ->
                    FilterChip(
                        selected = user.countryCode == c.code,
                        onClick = {
                            onUpdateSettings(mapOf("countryCode" to c.code), "Country set to ${c.name} (${c.currencySymbol})")
                        },
                        label = { Text("${c.name} (${c.currencySymbol})") },
                        modifier = Modifier.testTag("country_chip_${c.code}")
                    )
                }
            }
        }

        // Theme, Accessibility & Font Size
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Appearance & Accessibility", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("DARK", "LIGHT", "SYSTEM").forEach { mode ->
                            FilterChip(
                                selected = user.darkThemeMode == mode,
                                onClick = { onUpdateSettings(mapOf("darkThemeMode" to mode), "Theme set to $mode") },
                                label = { Text(mode) },
                                modifier = Modifier.testTag("theme_chip_$mode")
                            )
                        }
                    }
                    Text("Font Size Scale: ${String.format("%.2f", user.fontScaleMultiplier)}x", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = user.fontScaleMultiplier.toFloat().coerceIn(0.85f, 1.35f),
                        onValueChange = { scale ->
                            onUpdateSettings(mapOf("fontScaleMultiplier" to scale.toDouble()), null)
                        },
                        valueRange = 0.85f..1.35f
                    )
                    HubToggleRow("High Contrast Mode", user.highContrastMode) {
                        onUpdateSettings(mapOf("highContrastMode" to it), "High contrast updated")
                    }
                }
            }
        }

        // Safety, Privacy & 2FA
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Privacy, Security & 2FA Controls", style = MaterialTheme.typography.titleSmall)
                    HubToggleRow("Two-Factor Authentication (2FA)", user.twoFactorEnabled) {
                        onUpdateSettings(mapOf("twoFactorEnabled" to it), "2FA preference saved")
                    }
                    HubToggleRow("Restricted Content Filter", user.restrictedMode) {
                        onUpdateSettings(mapOf("restrictedMode" to it), "Restricted mode updated")
                    }
                    HubToggleRow("Minor Safety Mode", user.minorMode) {
                        onUpdateSettings(mapOf("minorMode" to it), "Minor safety mode updated")
                    }
                }
            }
        }

        // Login History & Multi-Device Sessions
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Devices, contentDescription = "Active sessions", tint = PeacockTeal)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Login History & Active Sessions (${sessions.size})", style = MaterialTheme.typography.titleSmall)
            }
        }
        items(sessions, key = { it.id }) { sess ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("${sess.deviceName} (${sess.platform})", style = MaterialTheme.typography.titleSmall)
                    Text("${sess.locationLabel} • ${if (sess.active) "Active Now" else "Ended"}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Sign Out & Account Deletion Request
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onSignOut,
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign Out")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sign Out", color = Color.White)
                }
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Delete Account")
                }
            }
        }
    }

    if (showRecoveryDialog) {
        var recoveryNote by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showRecoveryDialog = false },
            title = { Text("Account & Channel Recovery") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Your account (${user.bharatTubeId}) is protected by Google OAuth (${user.email}). Submit a verified recovery or channel-ownership review request below.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = recoveryNote,
                        onValueChange = { recoveryNote = it },
                        label = { Text("Recovery details / backup contact") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSubmitTicket(
                        "ACCOUNT_RECOVERY",
                        "Account Recovery Verification (${user.bharatTubeId})",
                        recoveryNote.ifBlank { "Google OAuth account recovery check requested for ${user.email}" }
                    )
                    showRecoveryDialog = false
                }) { Text("Submit Request") }
            },
            dismissButton = {
                TextButton(onClick = { showRecoveryDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Request Account Deletion") },
            text = { Text("This submits an irreversible account & data deletion request for your channel, videos, and wallet audit records.") },
            confirmButton = {
                Button(onClick = {
                    onSubmitTicket("ACCOUNT_DELETION", "Delete My BharatTube Account", "User requested verified account deletion.")
                    confirmDelete = false
                }) { Text("Confirm Request") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun HubToggleRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}
