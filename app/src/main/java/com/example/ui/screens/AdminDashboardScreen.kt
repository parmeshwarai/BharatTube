package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminAuditLog
import com.example.data.model.CopyrightClaim
import com.example.data.model.GlobalCatalog
import com.example.data.model.PayoutRecord
import com.example.data.model.PlatformConfig
import com.example.data.model.RevenueLedgerEntry
import com.example.data.model.SupportTicket
import com.example.data.model.UserProfile
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.PeacockTeal
import com.example.ui.theme.SaffronPrimary
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    isAdmin: Boolean,
    adminEmail: String,
    config: PlatformConfig,
    allUsers: List<UserProfile>,
    allClaims: List<CopyrightClaim>,
    allTickets: List<SupportTicket>,
    allLedger: List<RevenueLedgerEntry>,
    allPayouts: List<PayoutRecord>,
    auditLogs: List<AdminAuditLog>,
    onBack: () -> Unit,
    onSaveConfig: (PlatformConfig, String) -> Unit,
    onModerateUser: (String, String, Boolean, Boolean, Boolean) -> Unit,
    onResolveCopyright: (String, String) -> Unit,
    onResolveTicket: (String, String) -> Unit
) {
    BackHandler { onBack() }

    if (!isAdmin) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Super Admin Access Required", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Signed-in account ($adminEmail) does not hold verified Super Admin authority.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onBack) { Text("Back") }
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Controls", "Monetization & Ads", "Users & Safety", "Copyright & Help", "Audit & KPIs")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("BharatTube Super Admin") },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        PrimaryScrollableTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { idx, label ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(label) },
                    modifier = Modifier.testTag("admin_tab_$idx")
                )
            }
        }

        when (selectedTab) {
            0 -> AdminGlobalControlsTab(config, onSaveConfig)
            1 -> AdminMonetizationAdsTab(config, allLedger, allPayouts, onSaveConfig)
            2 -> AdminUsersTab(allUsers, onModerateUser)
            3 -> AdminClaimsTicketsTab(allClaims, allTickets, onResolveCopyright, onResolveTicket)
            4 -> AdminAuditKpiTab(allUsers, allLedger, allPayouts, auditLogs)
        }
    }
}

@Composable
private fun AdminGlobalControlsTab(
    config: PlatformConfig,
    onSaveConfig: (PlatformConfig, String) -> Unit
) {
    var draft by remember(config) { mutableStateOf(config) }
    var announcement by remember(config.announcementText) { mutableStateOf(config.announcementText) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Platform Announcement & Maintenance", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = announcement,
                        onValueChange = { announcement = it },
                        label = { Text("Global Announcement Banner") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    AdminSwitch("Maintenance Mode (Non-Admins Blocked)", draft.maintenanceMode) {
                        draft = draft.copy(maintenanceMode = it)
                    }
                    AdminSwitch("User Registration Enabled", draft.registrationEnabled) {
                        draft = draft.copy(registrationEnabled = it)
                    }
                    AdminSwitch("Uploads Enabled", draft.uploadsEnabled) {
                        draft = draft.copy(uploadsEnabled = it)
                    }
                    AdminSwitch("Long Videos Enabled", draft.longVideosEnabled) {
                        draft = draft.copy(longVideosEnabled = it)
                    }
                    AdminSwitch("Shorts & Reels Enabled", draft.shortsEnabled) {
                        draft = draft.copy(shortsEnabled = it)
                    }
                    AdminSwitch("Live Streaming Enabled", draft.liveStreamingEnabled) {
                        draft = draft.copy(liveStreamingEnabled = it)
                    }
                    AdminSwitch("Comments Enabled", draft.commentsEnabled) {
                        draft = draft.copy(commentsEnabled = it)
                    }
                    AdminSwitch("Likes Enabled", draft.likesEnabled) {
                        draft = draft.copy(likesEnabled = it)
                    }
                    AdminSwitch("Subscriptions & Follow Enabled", draft.subscriptionsEnabled) {
                        draft = draft.copy(subscriptionsEnabled = it, followingEnabled = it)
                    }
                    AdminSwitch("Chat Enabled", draft.chatEnabled) {
                        draft = draft.copy(chatEnabled = it)
                    }
                    AdminSwitch("Search Enabled", draft.searchEnabled) {
                        draft = draft.copy(searchEnabled = it)
                    }
                    AdminSwitch("Notifications Enabled", draft.notificationsEnabled) {
                        draft = draft.copy(notificationsEnabled = it)
                    }
                    AdminSwitch("Downloads & Community Posts", draft.downloadsEnabled) {
                        draft = draft.copy(downloadsEnabled = it, communityPostsEnabled = it)
                    }
                }
            }
        }

        item {
            Text("Enable / Disable Countries", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(GlobalCatalog.countries) { c ->
                    val enabled = c.code !in draft.disabledCountries
                    FilterChip(
                        selected = enabled,
                        onClick = {
                            val next = if (enabled) draft.disabledCountries + c.code else draft.disabledCountries - c.code
                            draft = draft.copy(disabledCountries = next.distinct())
                        },
                        label = { Text(c.code) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text("Enable / Disable Languages", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(GlobalCatalog.starterLanguages) { l ->
                    val enabled = l.code !in draft.disabledLanguages
                    FilterChip(
                        selected = enabled,
                        onClick = {
                            val next = if (enabled) draft.disabledLanguages + l.code else draft.disabledLanguages - l.code
                            draft = draft.copy(disabledLanguages = next.distinct())
                        },
                        label = { Text(l.nativeName) }
                    )
                }
            }
        }

        item {
            Button(
                onClick = {
                    onSaveConfig(draft.copy(announcementText = announcement), "Updated global feature & region switches")
                },
                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_save_controls_button")
            ) {
                Text("Save Global Controls & Write Audit Log", color = Color.White)
            }
        }
    }
}

@Composable
private fun AdminMonetizationAdsTab(
    config: PlatformConfig,
    allLedger: List<RevenueLedgerEntry>,
    allPayouts: List<PayoutRecord>,
    onSaveConfig: (PlatformConfig, String) -> Unit
) {
    var draft by remember(config) { mutableStateOf(config) }
    var creatorPercent by remember(config.creatorSharePercent) { mutableDoubleStateOf(config.creatorSharePercent) }
    val platformPercent = (100.0 - creatorPercent).coerceIn(0.0, 100.0)
    var minPayoutText by remember(config.indiaMinPayoutInr) { mutableStateOf(config.indiaMinPayoutInr.toInt().toString()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Revenue Split Governance (Must Equal 100%)", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "Creator Share: ${creatorPercent.roundToInt()}%  |  BharatTube Share: ${platformPercent.roundToInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = PeacockTeal
                    )
                    Slider(
                        value = creatorPercent.toFloat(),
                        onValueChange = { creatorPercent = it.roundToInt().toDouble() },
                        valueRange = 10f..90f,
                        modifier = Modifier.testTag("admin_revenue_split_slider")
                    )
                    OutlinedTextField(
                        value = minPayoutText,
                        onValueChange = { minPayoutText = it },
                        label = { Text("India Minimum Auto-Payout (₹)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    AdminSwitch("Platform Monetization Enabled", draft.monetizationEnabled) {
                        draft = draft.copy(monetizationEnabled = it)
                    }
                    AdminSwitch("Automatic Payouts Enabled", draft.autoPayoutEnabled) {
                        draft = draft.copy(autoPayoutEnabled = it)
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Global Advertisement Controls", style = MaterialTheme.typography.titleSmall)
                    AdminSwitch("Master Ads Enabled", draft.adsEnabled) { draft = draft.copy(adsEnabled = it) }
                    AdminSwitch("Pre-Roll Video Ads", draft.preRollAdsEnabled) { draft = draft.copy(preRollAdsEnabled = it) }
                    AdminSwitch("Mid-Roll Video Ads", draft.midRollAdsEnabled) { draft = draft.copy(midRollAdsEnabled = it) }
                    AdminSwitch("Shorts Feed Ads", draft.shortsAdsEnabled) { draft = draft.copy(shortsAdsEnabled = it) }
                    AdminSwitch("Home Sponsored Ads", draft.sponsoredHomeAdsEnabled) { draft = draft.copy(sponsoredHomeAdsEnabled = it) }
                    Text("Shorts Ad Frequency: every ${draft.adFrequency} items", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = draft.adFrequency.toFloat().coerceIn(1f, 8f),
                        onValueChange = { draft = draft.copy(adFrequency = it.roundToInt().toLong()) },
                        valueRange = 1f..8f
                    )
                    Text("Country Ad Disable Toggles", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(GlobalCatalog.countries.take(6)) { c ->
                            val adsOn = c.code !in draft.adDisabledCountries
                            FilterChip(
                                selected = adsOn,
                                onClick = {
                                    val next = if (adsOn) draft.adDisabledCountries + c.code else draft.adDisabledCountries - c.code
                                    draft = draft.copy(adDisabledCountries = next.distinct())
                                },
                                label = { Text("${c.code} Ads") }
                            )
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    val cPct = creatorPercent.roundToInt().toDouble()
                    val pPct = 100.0 - cPct
                    val minPayout = minPayoutText.toDoubleOrNull()?.coerceAtLeast(100.0) ?: 1000.0
                    onSaveConfig(
                        draft.copy(
                            creatorSharePercent = cPct,
                            platformSharePercent = pPct,
                            indiaMinPayoutInr = minPayout
                        ),
                        "Updated revenue split ${cPct.toInt()}:${pPct.toInt()}, minPayout=₹$minPayout, adsEnabled=${draft.adsEnabled}"
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_save_monetization_button")
            ) {
                Text("Save Monetization & Ad Rules", color = Color.White)
            }
        }

        item {
            Text(
                text = "Ledger Events: ${allLedger.size} • Payout Records: ${allPayouts.size}",
                style = MaterialTheme.typography.labelLarge,
                color = GoldHighlight
            )
        }
    }
}

@Composable
private fun AdminUsersTab(
    users: List<UserProfile>,
    onModerateUser: (String, String, Boolean, Boolean, Boolean) -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(users, search) {
        val q = search.trim().lowercase()
        users.filter {
            q.isEmpty() || it.displayName.lowercase().contains(q) || it.email.lowercase().contains(q) || it.userId.contains(q)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Search users by name, email or ID") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        items(filtered, key = { it.id }) { u ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${u.displayName} (${u.countryCode})", style = MaterialTheme.typography.titleSmall)
                        AssistChip(onClick = {}, label = { Text(u.accountStatus) })
                    }
                    Text("${u.email} • Balance ₹${u.availableBalanceInr}", style = MaterialTheme.typography.bodySmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            OutlinedButton(onClick = { onModerateUser(u.userId, "ACTIVE", false, false, true) }) {
                                Text("Activate")
                            }
                        }
                        item {
                            OutlinedButton(onClick = { onModerateUser(u.userId, "SUSPENDED", true, true, false) }) {
                                Text("Suspend")
                            }
                        }
                        item {
                            OutlinedButton(onClick = { onModerateUser(u.userId, "BANNED_PERM", true, true, false) }) {
                                Text("Ban")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminClaimsTicketsTab(
    claims: List<CopyrightClaim>,
    tickets: List<SupportTicket>,
    onResolveClaim: (String, String) -> Unit,
    onResolveTicket: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("Copyright Claims (${claims.size})", style = MaterialTheme.typography.titleMedium) }
        items(claims, key = { it.id }) { claim ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${claim.videoTitle} • ${claim.similarityPercent}% (${claim.status})", style = MaterialTheme.typography.titleSmall)
                    if (claim.appealNote.isNotBlank()) {
                        Text("Appeal: ${claim.appealNote}", style = MaterialTheme.typography.bodySmall, color = PeacockTeal)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onResolveClaim(claim.id, "DISMISSED") }) { Text("Dismiss") }
                        Button(onClick = { onResolveClaim(claim.id, "UPHELD") }) { Text("Uphold Strike") }
                    }
                }
            }
        }

        item { Text("Support & Safety Tickets (${tickets.size})", style = MaterialTheme.typography.titleMedium) }
        items(tickets, key = { it.id }) { t ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${t.category}: ${t.subject} [${t.status}]", style = MaterialTheme.typography.titleSmall)
                    Text(t.description, style = MaterialTheme.typography.bodySmall)
                    if (t.status != "RESOLVED") {
                        Button(onClick = { onResolveTicket(t.id, "Reviewed & actioned by BharatTube Super Admin") }) {
                            Text("Mark Resolved")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminAuditKpiTab(
    users: List<UserProfile>,
    ledger: List<RevenueLedgerEntry>,
    payouts: List<PayoutRecord>,
    auditLogs: List<AdminAuditLog>
) {
    val totalPlatformRevenue = ledger.sumOf { it.platformEarnedInr }
    val totalCreatorRevenue = ledger.sumOf { it.creatorEarnedInr }
    val totalPaid = payouts.sumOf { it.netPayoutInr }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("BharatTube Platform Financial Summary", style = MaterialTheme.typography.titleMedium)
                    Text("Total Users: ${users.size} • Ledger Entries: ${ledger.size}", style = MaterialTheme.typography.bodySmall)
                    Text("Creator Earned: ₹${String.format("%.2f", totalCreatorRevenue)} | BharatTube Earned: ₹${String.format("%.2f", totalPlatformRevenue)}", style = MaterialTheme.typography.bodyMedium, color = PeacockTeal)
                    Text("Total Payouts Dispatched: ₹${String.format("%.2f", totalPaid)}", style = MaterialTheme.typography.bodySmall, color = GoldHighlight)
                }
            }
        }
        item { Text("Immutable Admin Audit Logs (${auditLogs.size})", style = MaterialTheme.typography.titleMedium) }
        items(auditLogs, key = { it.id }) { log ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("${log.action} -> ${log.targetType}/${log.targetId}", style = MaterialTheme.typography.titleSmall)
                    Text("By ${log.adminEmail}: ${log.details}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun AdminSwitch(label: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChanged)
    }
}
