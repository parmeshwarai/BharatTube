package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.Channel
import com.example.data.model.GlobalCatalog
import com.example.data.model.LiveStream
import com.example.data.model.PlatformConfig
import com.example.data.model.SearchHistoryEntry
import com.example.data.model.Video
import com.example.domain.RecommendationEngine
import com.example.ui.components.VideoFeedCard
import com.example.ui.components.formatCompactCount
import com.example.ui.theme.PeacockTeal
import com.example.ui.theme.SaffronPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    config: PlatformConfig,
    videos: List<Video>,
    channels: List<Channel>,
    liveStreams: List<LiveStream>,
    searchHistory: List<SearchHistoryEntry>,
    onBack: () -> Unit,
    onSaveSearch: (String) -> Unit,
    onClearHistory: () -> Unit,
    onOpenVideo: (String) -> Unit,
    onOpenShorts: () -> Unit,
    onOpenChannel: (String) -> Unit,
    onOpenLive: (String) -> Unit,
    onNotify: (String) -> Unit
) {
    BackHandler { onBack() }

    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var countryFilter by remember { mutableStateOf("ALL") }
    var languageFilter by remember { mutableStateOf("ALL") }
    var sortMode by remember { mutableStateOf("POPULAR") }
    var selectedTab by remember { mutableIntStateOf(0) }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull().orEmpty()
            if (spoken.isNotBlank()) {
                query = spoken
                onSaveSearch(spoken)
            }
        }
    }

    val filteredVideos = remember(videos, query, selectedCategory, countryFilter, languageFilter, sortMode) {
        RecommendationEngine.filterSearch(videos, query, selectedCategory, countryFilter, languageFilter, sortMode)
    }
    val longResults = remember(filteredVideos) { filteredVideos.filter { it.format == "LONG" } }
    val shortResults = remember(filteredVideos) { filteredVideos.filter { it.format == "SHORT" } }
    val channelResults = remember(channels, query, countryFilter, languageFilter) {
        val q = query.trim().lowercase()
        channels.filter { ch ->
            (countryFilter == "ALL" || ch.countryCode.equals(countryFilter, true)) &&
                (languageFilter == "ALL" || ch.languageCode.equals(languageFilter, true)) &&
                (q.isEmpty() || ch.name.lowercase().contains(q) || ch.handle.lowercase().contains(q))
        }
    }
    val liveResults = remember(liveStreams, query) {
        val q = query.trim().lowercase()
        liveStreams.filter { s ->
            q.isEmpty() || s.title.lowercase().contains(q) || s.creatorName.lowercase().contains(q)
        }
    }

    val suggestions = remember(videos, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            listOf("#BharatTech", "#IndieMusic", "4K Travel", "Hindi Podcast", "Creator Monetization")
        } else {
            (videos.map { it.title } + videos.flatMap { it.hashtags })
                .distinct()
                .filter { it.lowercase().contains(q) }
                .take(5)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Search BharatTube") },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("search_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        if (!config.searchEnabled) {
            Text(
                text = "Search is temporarily disabled by platform administrator.",
                modifier = Modifier.padding(20.dp),
                style = MaterialTheme.typography.titleMedium
            )
            return
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search videos, Shorts, creators, #hashtags...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    Row {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear query")
                            }
                        }
                        IconButton(
                            onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search BharatTube")
                                }
                                try {
                                    voiceLauncher.launch(intent)
                                } catch (_: Exception) {
                                    onNotify("Voice recognition app is unavailable on this device.")
                                }
                            },
                            modifier = Modifier.testTag("voice_search_button")
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice search", tint = SaffronPrimary)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_query_input")
            )

            // Search Suggestions
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(suggestions) { suggestion ->
                    AssistChip(
                        onClick = {
                            query = suggestion
                            onSaveSearch(suggestion)
                        },
                        label = { Text(suggestion) }
                    )
                }
            }

            // Sort & Region Filters
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = sortMode == "POPULAR",
                        onClick = { sortMode = "POPULAR" },
                        label = { Text("Popular") }
                    )
                }
                item {
                    FilterChip(
                        selected = sortMode == "VIEWS",
                        onClick = { sortMode = "VIEWS" },
                        label = { Text("Most Viewed") }
                    )
                }
                item {
                    FilterChip(
                        selected = sortMode == "LATEST",
                        onClick = { sortMode = "LATEST" },
                        label = { Text("Upload Date") }
                    )
                }
                item {
                    FilterChip(
                        selected = countryFilter == "IN",
                        onClick = { countryFilter = if (countryFilter == "IN") "ALL" else "IN" },
                        label = { Text("India Only") }
                    )
                }
                item {
                    FilterChip(
                        selected = languageFilter == "hi",
                        onClick = { languageFilter = if (languageFilter == "hi") "ALL" else "hi" },
                        label = { Text("हिन्दी") }
                    )
                }
                items(GlobalCatalog.categories.take(6)) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }
        }

        val tabs = listOf(
            "Videos (${longResults.size})",
            "Shorts (${shortResults.size})",
            "Channels (${channelResults.size})",
            "Live (${liveResults.size})"
        )
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = {
                        selectedTab = index
                        if (query.isNotBlank()) onSaveSearch(query)
                    },
                    text = { Text(title, maxLines = 1) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (query.isBlank() && searchHistory.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent Searches", style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = onClearHistory) { Text("Clear") }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        searchHistory.take(5).forEach { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { query = entry.query }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.History, contentDescription = "History", tint = PeacockTeal)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(entry.query, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    if (longResults.isEmpty()) {
                        item { Text("No matching long videos found.", style = MaterialTheme.typography.bodyMedium) }
                    } else {
                        items(longResults, key = { it.id }) { vid ->
                            VideoFeedCard(video = vid, onClick = {
                                if (query.isNotBlank()) onSaveSearch(query)
                                onOpenVideo(vid.id)
                            })
                        }
                    }
                }
                1 -> {
                    if (shortResults.isEmpty()) {
                        item { Text("No matching Shorts found.", style = MaterialTheme.typography.bodyMedium) }
                    } else {
                        items(shortResults, key = { it.id }) { shortVid ->
                            VideoFeedCard(video = shortVid, onClick = {
                                if (query.isNotBlank()) onSaveSearch(query)
                                onOpenShorts()
                            })
                        }
                    }
                }
                2 -> {
                    items(channelResults, key = { it.id }) { ch ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenChannel(ch.id) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(ch.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${ch.handle} • ${formatCompactCount(ch.subscriberCount)} subscribers • ${ch.countryCode}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                3 -> {
                    items(liveResults, key = { it.id }) { stream ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenLive(stream.id) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LiveTv, contentDescription = "Live stream", tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(stream.title, style = MaterialTheme.typography.titleSmall)
                                    Text("${stream.creatorName} • ${stream.viewerCount} watching", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
