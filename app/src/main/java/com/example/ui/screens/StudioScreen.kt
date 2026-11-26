package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AdCampaign
import com.example.data.model.Channel
import com.example.data.model.CopyrightClaim
import com.example.data.model.GlobalCatalog
import com.example.data.model.PayoutRecord
import com.example.data.model.PlatformConfig
import com.example.data.model.RevenueLedgerEntry
import com.example.data.model.UserProfile
import com.example.data.model.Video
import com.example.domain.MonetizationEngine
import com.example.ui.components.formatCompactCount
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.PeacockTeal
import com.example.ui.theme.SaffronPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    user: UserProfile,
    config: PlatformConfig,
    myVideos: List<Video>,
    myChannels: List<Channel>,
    activeChannel: Channel?,
    revenueLedger: List<RevenueLedgerEntry>,
    payouts: List<PayoutRecord>,
    myCampaigns: List<AdCampaign>,
    copyrightClaims: List<CopyrightClaim>,
    onSwitchActiveChannel: (String) -> Unit,
    onOpenCreateChannel: () -> Unit,
    onSettleSampleAdRevenue: (Double, Video?) -> Unit,
    onTriggerPayoutCheck: () -> Unit,
    onRetryPayout: (String) -> Unit,
    onUpdatePaymentSettings: (Map<String, Any>, String) -> Unit,
    onCreateAdCampaign: (String, String, String, String, List<String>, List<String>, Double) -> Unit,
    onToggleCampaignStatus: (String, String) -> Unit,
    onAppealCopyright: (String, String) -> Unit,
    onUpdateVideoMetadata: (String, String, String, String, String, Boolean) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var analyticsRange by remember { mutableStateOf("Daily") }
    var filterByActiveChannel by remember { mutableStateOf(false) }
    var editingVideo by remember { mutableStateOf<Video?>(null) }

    val channelFilteredVideos = remember(myVideos, activeChannel?.id, filterByActiveChannel) {
        if (!filterByActiveChannel || activeChannel == null) myVideos
        else myVideos.filter { it.channelId.isBlank() || it.channelId == activeChannel.id }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Creator Studio", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = "${config.creatorSharePercent.toInt()}% Creator / ${config.platformSharePercent.toInt()}% BharatTube Share • Auto-Payout ₹${config.indiaMinPayoutInr.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = PeacockTeal
                    )
                }
                OutlinedButton(onClick = onOpenCreateChannel) {
                    Icon(Icons.Default.AddCircle, contentDescription = "Create Channel")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Channel")
                }
            }
            if (myChannels.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = !filterByActiveChannel,
                            onClick = { filterByActiveChannel = false },
                            label = { Text("All Channels (${myChannels.size})") }
                        )
                    }
                    items(myChannels, key = { it.id }) { ch ->
                        FilterChip(
                            selected = filterByActiveChannel && activeChannel?.id == ch.id,
                            onClick = {
                                onSwitchActiveChannel(ch.id)
                                filterByActiveChannel = true
                            },
                            label = { Text("${ch.name} (${ch.handle})") }
                        )
                    }
                }
            }
        }

        val tabs = listOf("Analytics", "Earnings & Payouts", "Advertiser", "Copyright")
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { idx, label ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(label, maxLines = 1, style = MaterialTheme.typography.labelLarge) },
                    modifier = Modifier.testTag("studio_tab_$idx")
                )
            }
        }

        when (selectedTab) {
            0 -> AnalyticsTab(
                user = user,
                myVideos = channelFilteredVideos,
                myChannel = activeChannel,
                range = analyticsRange,
                onRangeChange = { analyticsRange = it },
                onEditVideo = { editingVideo = it }
            )
            1 -> EarningsPayoutTab(
                user = user,
                config = config,
                myVideos = myVideos,
                ledger = revenueLedger,
                payouts = payouts,
                onSettleSampleAdRevenue = onSettleSampleAdRevenue,
                onTriggerPayoutCheck = onTriggerPayoutCheck,
                onRetryPayout = onRetryPayout,
                onUpdatePaymentSettings = onUpdatePaymentSettings
            )
            2 -> AdvertiserTab(
                campaigns = myCampaigns,
                onCreateCampaign = onCreateAdCampaign,
                onToggleStatus = onToggleCampaignStatus
            )
            3 -> CopyrightTab(
                claims = copyrightClaims,
                onAppeal = onAppealCopyright
            )
        }
    }

    editingVideo?.let { video ->
        var newTitle by remember(video.id) { mutableStateOf(video.title) }
        var newDesc by remember(video.id) { mutableStateOf(video.description) }
        var newVis by remember(video.id) { mutableStateOf(video.visibility) }
        var newMonetized by remember(video.id) { mutableStateOf(video.monetized) }

        AlertDialog(
            onDismissRequest = { editingVideo = null },
            title = { Text("Edit Video Metadata") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newTitle, onValueChange = { newTitle = it }, label = { Text("Title") })
                    OutlinedTextField(value = newDesc, onValueChange = { newDesc = it }, label = { Text("Description") })
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("PUBLIC", "UNLISTED", "PRIVATE")) { vis ->
                            FilterChip(selected = newVis == vis, onClick = { newVis = vis }, label = { Text(vis) })
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Monetized")
                        Switch(checked = newMonetized, onCheckedChange = { newMonetized = it })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateVideoMetadata(video.id, newTitle, newDesc, newVis, video.category, newMonetized)
                    editingVideo = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingVideo = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AnalyticsTab(
    user: UserProfile,
    myVideos: List<Video>,
    myChannel: Channel?,
    range: String,
    onRangeChange: (String) -> Unit,
    onEditVideo: (Video) -> Unit
) {
    val longVideos = remember(myVideos) { myVideos.filter { it.format == "LONG" } }
    val shorts = remember(myVideos) { myVideos.filter { it.format == "SHORT" } }
    val totalLongViews = longVideos.sumOf { it.viewCount }
    val totalShortsViews = shorts.sumOf { it.viewCount }
    val factor = when (range) {
        "Daily" -> 0.15
        "Weekly" -> 0.55
        else -> 1.0
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Daily", "Weekly", "Monthly")) { item ->
                    FilterChip(
                        selected = range == item,
                        onClick = { onRangeChange(item) },
                        label = { Text(item) }
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                KpiCard(
                    title = "Long Video Views",
                    value = formatCompactCount((totalLongViews * factor).toLong().coerceAtLeast(totalLongViews.coerceAtMost(100))),
                    subtitle = "${longVideos.size} uploads",
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Shorts Views",
                    value = formatCompactCount((totalShortsViews * factor).toLong().coerceAtLeast(totalShortsViews.coerceAtMost(100))),
                    subtitle = "${shorts.size} Shorts",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                KpiCard(
                    title = "Subscribers",
                    value = formatCompactCount(myChannel?.subscriberCount ?: 0L),
                    subtitle = "Avg retention: 64%",
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Watch Time",
                    value = "${((totalLongViews + totalShortsViews) * 0.045 * factor).toInt()} hrs",
                    subtitle = "Top: ${user.countryCode} • Android",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Analytics, contentDescription = "Audience breakdown", tint = PeacockTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Audience & Format Insights ($range)", style = MaterialTheme.typography.titleSmall)
                    }
                    Text(
                        text = "Countries: India 68% • USA 14% • Japan 9% • UK 9%",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Languages: Hindi 44% • English 31% • Tamil/Telugu/Bhojpuri 25%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { Text("Your Videos & Shorts", style = MaterialTheme.typography.titleMedium) }

        if (myVideos.isEmpty()) {
            item {
                Text("No uploads yet. Publish your first video from the Upload tab.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            items(myVideos, key = { it.id }) { vid ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(vid.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${vid.format} • ${vid.visibility} • ${vid.viewCount} views • Earned ${GlobalCatalog.formatCurrencyFromInr(vid.adRevenueInr, user.countryCode)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(onClick = { onEditVideo(vid) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit metadata")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EarningsPayoutTab(
    user: UserProfile,
    config: PlatformConfig,
    myVideos: List<Video>,
    ledger: List<RevenueLedgerEntry>,
    payouts: List<PayoutRecord>,
    onSettleSampleAdRevenue: (Double, Video?) -> Unit,
    onTriggerPayoutCheck: () -> Unit,
    onRetryPayout: (String) -> Unit,
    onUpdatePaymentSettings: (Map<String, Any>, String) -> Unit
) {
    var upiInput by remember(user.upiId) { mutableStateOf(user.upiId) }
    var bankInput by remember(user.bankAccountMasked) { mutableStateOf(user.bankAccountMasked) }
    var kycChecked by remember(user.kycVerified) { mutableStateOf(user.kycVerified) }

    val thresholdInr = if (user.countryCode == "IN") config.indiaMinPayoutInr else 1500.0
    val progress = (user.availableBalanceInr / thresholdInr.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
    val eval = MonetizationEngine.evaluateAutomaticPayout(
        availableBalanceInr = user.availableBalanceInr,
        countryCode = user.countryCode,
        kycVerified = user.kycVerified,
        hasPaymentDetails = user.upiId.isNotBlank() || user.bankAccountMasked.isNotBlank(),
        minorMode = user.minorMode,
        creatorMonetizationEnabled = user.monetizationEnabled,
        config = config
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Balance Summary
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Creator Wallet", tint = GoldHighlight)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("50:50 Creator Revenue Wallet", style = MaterialTheme.typography.titleMedium)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Available Balance", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = GlobalCatalog.formatCurrencyFromInr(user.availableBalanceInr, user.countryCode),
                                style = MaterialTheme.typography.headlineMedium,
                                color = PeacockTeal,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Paid Out", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = GlobalCatalog.formatCurrencyFromInr(user.totalPaidOutInr, user.countryCode),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                    Text(
                        text = "Estimated: ${GlobalCatalog.formatInr(user.estimatedEarningsInr)} • Confirmed: ${GlobalCatalog.formatInr(user.confirmedEarningsInr)} • Pending: ${GlobalCatalog.formatInr(user.pendingBalanceInr)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (progress >= 1f) PeacockTeal else SaffronPrimary
                    )
                    Text(
                        text = "Automatic Payout Threshold (${user.countryCode}): ${GlobalCatalog.formatCurrencyFromInr(thresholdInr, user.countryCode)} • ${eval.reason}",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        // Transparent 50:50 Settlement & Auto-Payout Test Actions
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ad Revenue Settlement (50% Creator / 50% BharatTube)", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "Record a verified ad revenue batch (e.g. ₹2,272.73 gross -> ₹2,000 net -> ₹1,000 Creator / ₹1,000 BharatTube) to verify automatic ₹1,000 India payout trigger.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onSettleSampleAdRevenue(2272.73, myVideos.firstOrNull()) },
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settle_ad_revenue_button")
                        ) {
                            Text("Credit Ad Batch", color = Color.White)
                        }
                        OutlinedButton(
                            onClick = onTriggerPayoutCheck,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("trigger_auto_payout_button")
                        ) {
                            Text("Evaluate Payout")
                        }
                    }
                }
            }
        }

        // KYC & Payment Details Setup
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = "KYC verification", tint = PeacockTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("KYC, UPI & Bank Payout Details", style = MaterialTheme.typography.titleSmall)
                    }
                    OutlinedTextField(
                        value = upiInput,
                        onValueChange = { upiInput = it },
                        label = { Text("UPI ID (India Instant Payout)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_upi_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = bankInput,
                        onValueChange = { bankInput = it },
                        label = { Text("Bank Account (Masked / IFSC)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("KYC Verified (PAN / Tax ID)", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = kycChecked,
                            onCheckedChange = { kycChecked = it },
                            modifier = Modifier.testTag("studio_kyc_switch")
                        )
                    }
                    Button(
                        onClick = {
                            onUpdatePaymentSettings(
                                mapOf(
                                    "upiId" to upiInput.trim(),
                                    "bankAccountMasked" to bankInput.trim(),
                                    "kycVerified" to kycChecked
                                ),
                                "Payout & KYC settings saved"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_payout_settings_button")
                    ) {
                        Text("Save Payout Details")
                    }
                }
            }
        }

        // Payout History
        item { Text("Payout History (${payouts.size})", style = MaterialTheme.typography.titleMedium) }
        items(payouts, key = { it.id }) { payout ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${payout.method} • ${payout.transactionId}", style = MaterialTheme.typography.titleSmall)
                        AssistChip(onClick = {}, label = { Text(payout.status) })
                    }
                    Text(
                        text = "Gross ₹${payout.amountInr} • TDS Tax ₹${payout.taxDeductedInr} • Net ₹${payout.netPayoutInr} (${payout.destinationMasked})",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (payout.status == "FAILED") {
                        TextButton(onClick = { onRetryPayout(payout.id) }) { Text("Retry Failed Payout") }
                    }
                }
            }
        }

        // Revenue Split Ledger
        item { Text("50:50 Revenue Ledger (${ledger.size})", style = MaterialTheme.typography.titleMedium) }
        items(ledger.take(12), key = { it.id }) { entry ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "${entry.format} • Net ${GlobalCatalog.formatInr(entry.netRevenueInr)} (${entry.countryCode})",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Creator ${entry.creatorSharePercent.toInt()}%: ${GlobalCatalog.formatInr(entry.creatorEarnedInr)} | BharatTube ${entry.platformSharePercent.toInt()}%: ${GlobalCatalog.formatInr(entry.platformEarnedInr)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = PeacockTeal
                    )
                }
            }
        }
    }
}

@Composable
private fun AdvertiserTab(
    campaigns: List<AdCampaign>,
    onCreateCampaign: (String, String, String, String, List<String>, List<String>, Double) -> Unit,
    onToggleStatus: (String, String) -> Unit
) {
    var advertiserName by remember { mutableStateOf("") }
    var adTitle by remember { mutableStateOf("") }
    var ctaText by remember { mutableStateOf("Shop Now") }
    var placement by remember { mutableStateOf("PRE_ROLL") }
    var budgetText by remember { mutableStateOf("5000") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Campaign, contentDescription = "Launch Ad Campaign", tint = GoldHighlight)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Advertiser Campaign", style = MaterialTheme.typography.titleMedium)
                    }
                    OutlinedTextField(
                        value = advertiserName,
                        onValueChange = { advertiserName = it },
                        label = { Text("Brand / Advertiser Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = adTitle,
                        onValueChange = { adTitle = it },
                        label = { Text("Campaign Headline") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = ctaText,
                            onValueChange = { ctaText = it },
                            label = { Text("CTA Button") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = budgetText,
                            onValueChange = { budgetText = it },
                            label = { Text("Budget (₹)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("PRE_ROLL", "MID_ROLL", "SHORTS_FEED", "HOME_SPONSORED")) { slot ->
                            FilterChip(
                                selected = placement == slot,
                                onClick = { placement = slot },
                                label = { Text(slot) }
                            )
                        }
                    }
                    Button(
                        onClick = {
                            onCreateCampaign(
                                advertiserName.ifBlank { "BharatBrand" },
                                adTitle.ifBlank { "Discover Made-in-India Innovation" },
                                ctaText.ifBlank { "Learn More" },
                                placement,
                                listOf("IN", "US", "GB"),
                                listOf("en", "hi"),
                                budgetText.toDoubleOrNull() ?: 5000.0
                            )
                            adTitle = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("launch_campaign_button")
                    ) {
                        Text("Launch Campaign", color = Color.White)
                    }
                }
            }
        }

        item { Text("My Campaigns (${campaigns.size})", style = MaterialTheme.typography.titleMedium) }
        items(campaigns, key = { it.id }) { camp ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${camp.title} (${camp.placement})", style = MaterialTheme.typography.titleSmall)
                        AssistChip(onClick = {}, label = { Text(camp.status) })
                    }
                    Text(
                        text = "Impressions: ${camp.impressions} • Valid Views: ${camp.validViews} • Clicks: ${camp.clicks} • Spend: ₹${camp.spentInr} / ₹${camp.budgetInr}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            onToggleStatus(camp.id, if (camp.status == "ACTIVE") "PAUSED" else "ACTIVE")
                        }
                    ) {
                        Text(if (camp.status == "ACTIVE") "Pause Campaign" else "Resume Campaign")
                    }
                }
            }
        }
    }
}

@Composable
private fun CopyrightTab(
    claims: List<CopyrightClaim>,
    onAppeal: (String, String) -> Unit
) {
    var appealTarget by remember { mutableStateOf<CopyrightClaim?>(null) }
    var appealNote by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Copyright, contentDescription = "Copyright Protection", tint = GoldHighlight)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Audio/video fingerprint similarity triggers an early review warning only. Submit a license or fair-use appeal anytime.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        if (claims.isEmpty()) {
            item { Text("All your uploads are clear with zero copyright warnings.", style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(claims, key = { it.id }) { claim ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(claim.videoTitle, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "${claim.matchType} • ${claim.similarityPercent}% similarity • Status: ${claim.status}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GoldHighlight
                        )
                        if (claim.status == "WARNING") {
                            Button(onClick = { appealTarget = claim }) { Text("Submit Appeal") }
                        }
                    }
                }
            }
        }
    }

    appealTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { appealTarget = null },
            title = { Text("Appeal Copyright Warning") },
            text = {
                OutlinedTextField(
                    value = appealNote,
                    onValueChange = { appealNote = it },
                    label = { Text("License ID or fair-use explanation") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onAppeal(target.id, appealNote.ifBlank { "Original creator work / licensed audio" })
                    appealNote = ""
                    appealTarget = null
                }) {
                    Text("Submit Appeal")
                }
            },
            dismissButton = {
                TextButton(onClick = { appealTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun KpiCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = PeacockTeal)
        }
    }
}
