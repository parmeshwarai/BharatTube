package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.Channel
import com.example.data.model.GlobalCatalog
import com.example.data.model.UserProfile
import com.example.ui.components.BharatTubeAvatar
import com.example.ui.components.channelAccentColor
import com.example.ui.components.resolveThumbnailDrawable
import com.example.ui.theme.PeacockTeal

private val channelThemes = listOf("SAFFRON", "PEACOCK", "GOLD", "OBSIDIAN")
private val bannerPresets = listOf(
    "img_channel_banner" to "Studio Banner",
    "img_hero_bharattube" to "Global Stage",
    "img_thumb_tech" to "Tech Horizon",
    "img_thumb_music" to "Music Neon"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChannelScreen(
    user: UserProfile,
    ownedChannelCount: Int,
    maxChannels: Int,
    editingChannel: Channel?,
    handleCheckState: Pair<String, Boolean?>,
    onCheckHandle: (String) -> Unit,
    onResetHandleCheck: () -> Unit,
    onCreateChannel: (
        name: String,
        handle: String,
        description: String,
        category: String,
        avatarUrl: String,
        avatarKey: String,
        bannerUrl: String,
        bannerKey: String,
        socialLinks: List<String>,
        appearanceTheme: String,
        countryCode: String,
        languageCode: String
    ) -> Unit,
    onUpdateChannel: (
        channelId: String,
        name: String,
        newHandle: String,
        oldHandle: String,
        description: String,
        category: String,
        avatarUrl: String,
        bannerUrl: String,
        socialLinks: List<String>,
        appearanceTheme: String,
        countryCode: String,
        languageCode: String
    ) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val isEdit = editingChannel != null
    var name by remember(editingChannel?.id) {
        mutableStateOf(editingChannel?.name ?: if (ownedChannelCount == 0) user.displayName else "")
    }
    var handle by remember(editingChannel?.id) {
        mutableStateOf(
            editingChannel?.handle ?: if (ownedChannelCount == 0) user.handle else "@"
        )
    }
    var description by remember(editingChannel?.id) {
        mutableStateOf(editingChannel?.description ?: "")
    }
    var category by remember(editingChannel?.id) {
        mutableStateOf(editingChannel?.category?.ifBlank { "Technology" } ?: "Technology")
    }
    var avatarUrl by remember(editingChannel?.id) {
        mutableStateOf(editingChannel?.avatarUrl ?: user.photoUrl)
    }
    var avatarKey by remember(editingChannel?.id) {
        mutableStateOf(editingChannel?.avatarKey?.ifBlank { "img_hero_bharattube" } ?: "img_hero_bharattube")
    }
    var bannerUrl by remember(editingChannel?.id) {
        mutableStateOf(editingChannel?.bannerUrl ?: "")
    }
    var bannerKey by remember(editingChannel?.id) {
        mutableStateOf(editingChannel?.bannerKey?.ifBlank { "img_channel_banner" } ?: "img_channel_banner")
    }
    var socialLinksText by remember(editingChannel?.id) {
        mutableStateOf(editingChannel?.socialLinks?.joinToString("\n") ?: "")
    }
    var appearanceTheme by remember(editingChannel?.id) {
        mutableStateOf(editingChannel?.appearanceTheme?.ifBlank { "SAFFRON" } ?: "SAFFRON")
    }
    var countryCode by remember(editingChannel?.id) {
        mutableStateOf(editingChannel?.countryCode?.ifBlank { user.countryCode } ?: user.countryCode)
    }
    var languageCode by remember(editingChannel?.id) {
        mutableStateOf(editingChannel?.languageCode?.ifBlank { user.languageCode } ?: user.languageCode)
    }

    LaunchedEffect(Unit) { onResetHandleCheck() }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) avatarUrl = uri.toString()
    }
    val bannerPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) bannerUrl = uri.toString()
    }

    val accent = channelAccentColor(appearanceTheme)
    val channelCategories = remember { GlobalCatalog.categories.filter { it != "All" } }
    val normalizedCurrentHandle = handle.trim().removePrefix("@").lowercase()
    val normalizedCheckTarget = handleCheckState.first.trim().removePrefix("@").lowercase()
    val isSameAsOwnOld = isEdit && normalizedCurrentHandle == editingChannel?.handle?.removePrefix("@")?.lowercase()
    val handleAvailability = if (isSameAsOwnOld) true
    else if (normalizedCurrentHandle.isNotBlank() && normalizedCurrentHandle == normalizedCheckTarget) handleCheckState.second
    else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text(if (isEdit) "Customize Channel" else "Create Your Channel") },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("create_channel_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Channel Appearance Preview Card
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.border(1.dp, accent.copy(alpha = 0.55f), MaterialTheme.shapes.large)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(122.dp)
                        ) {
                            if (bannerUrl.isNotBlank()) {
                                AsyncImage(
                                    model = bannerUrl,
                                    contentDescription = "Channel banner preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = resolveThumbnailDrawable(bannerKey)),
                                    contentDescription = "Channel banner preview",
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
                                    text = "$category • $appearanceTheme",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BharatTubeAvatar(
                                name = name.ifBlank { "Channel" },
                                photoUrl = avatarUrl,
                                fallbackKey = if (avatarUrl.isBlank()) avatarKey else "",
                                accentColor = accent,
                                size = 56.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name.ifBlank { "Your Channel Name" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                val formattedHandle = if (handle.startsWith("@")) handle else "@$handle"
                                Text(
                                    text = "$formattedHandle • ${GlobalCatalog.countryByCode(countryCode).name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = accent
                                )
                                Text(
                                    text = "BharatTube ID: ${user.bharatTubeId.ifBlank { user.userId.take(10) }} • Channels: $ownedChannelCount/$maxChannels",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Pick Avatar & Banner
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_channel_avatar_button")
                    ) {
                        Icon(Icons.Default.Image, contentDescription = "Pick avatar")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Profile Picture")
                    }
                    OutlinedButton(
                        onClick = {
                            bannerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_channel_banner_button")
                    ) {
                        Icon(Icons.Default.Image, contentDescription = "Pick banner")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Channel Banner")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Or choose a studio banner preset:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(bannerPresets) { (key, label) ->
                        FilterChip(
                            selected = bannerUrl.isBlank() && bannerKey == key,
                            onClick = {
                                bannerUrl = ""
                                bannerKey = key
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }

            // Channel Name & Unique Handle
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Channel Name") },
                    placeholder = { Text("e.g., Bharat Tech Diaries") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_channel_name_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = handle,
                        onValueChange = { input ->
                            val clean = input.replace(" ", "")
                            handle = if (clean.startsWith("@") || clean.isEmpty()) clean else "@$clean"
                            onResetHandleCheck()
                        },
                        label = { Text("Unique Channel Handle (@username)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("create_channel_handle_input")
                    )
                    OutlinedButton(
                        onClick = { onCheckHandle(handle) },
                        modifier = Modifier.testTag("check_handle_button")
                    ) {
                        Text("Check")
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                when (handleAvailability) {
                    true -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Available", tint = PeacockTeal, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Handle is available and ready to claim!", style = MaterialTheme.typography.labelMedium, color = PeacockTeal)
                    }
                    false -> Text(
                        text = "Handle unavailable or too short (min 2 letters/numbers). Try another unique @handle.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    null -> Text(
                        text = "Handles are globally unique across BharatTube. Tap Check to verify availability.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Description
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Channel Description") },
                    placeholder = { Text("Describe your videos, upload schedule, and community...") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_channel_desc_input")
                )
            }

            // Category Selector
            item {
                Text("Channel Category", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(channelCategories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) },
                            modifier = Modifier.testTag("channel_cat_$cat")
                        )
                    }
                }
            }

            // Social Links
            item {
                OutlinedTextField(
                    value = socialLinksText,
                    onValueChange = { socialLinksText = it },
                    label = { Text("Social Links (one per line or comma-separated, max 5)") },
                    placeholder = { Text("https://instagram.com/yourhandle\nhttps://x.com/yourhandle") },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = "Social links") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_channel_links_input")
                )
            }

            // Appearance Theme
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = "Appearance theme", tint = accent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Channel Appearance Theme", style = MaterialTheme.typography.titleSmall)
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(channelThemes) { themeKey ->
                        FilterChip(
                            selected = appearanceTheme == themeKey,
                            onClick = { appearanceTheme = themeKey },
                            label = { Text(themeKey) },
                            modifier = Modifier.testTag("channel_theme_$themeKey")
                        )
                    }
                }
            }

            // Country & Language
            item {
                Text("Channel Primary Country & Language", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(GlobalCatalog.countries) { c ->
                        FilterChip(
                            selected = countryCode == c.code,
                            onClick = { countryCode = c.code },
                            label = { Text(c.name) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(GlobalCatalog.starterLanguages) { lang ->
                        FilterChip(
                            selected = languageCode == lang.code,
                            onClick = { languageCode = lang.code },
                            label = { Text(lang.nativeName) }
                        )
                    }
                }
            }

            // Ownership & Policy Note
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = "Ownership protection", tint = PeacockTeal)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Channel ownership is bound to your Google OAuth account (${user.email.ifBlank { user.userId }}). Up to $maxChannels channels are permitted per BharatTube account.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Submit Button
            item {
                val parsedLinks = socialLinksText
                    .split(Regex("[,\\n]+"))
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .take(5)

                Button(
                    onClick = {
                        if (isEdit && editingChannel != null) {
                            onUpdateChannel(
                                editingChannel.id,
                                name,
                                handle,
                                editingChannel.handle,
                                description,
                                category,
                                avatarUrl,
                                bannerUrl,
                                parsedLinks,
                                appearanceTheme,
                                countryCode,
                                languageCode
                            )
                        } else {
                            onCreateChannel(
                                name,
                                handle,
                                description,
                                category,
                                avatarUrl,
                                avatarKey,
                                bannerUrl,
                                bannerKey,
                                parsedLinks,
                                appearanceTheme,
                                countryCode,
                                languageCode
                            )
                        }
                    },
                    enabled = name.isNotBlank() && handle.removePrefix("@").length >= 2,
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("create_channel_submit_button")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEdit) "Save Channel Customization" else "Create Your Channel",
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
