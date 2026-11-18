package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.AnimeSeries
import com.example.model.CuratedQuotesCatalog
import com.example.model.QuoteHistoryEntry
import com.example.model.UiState
import com.example.share.QuoteShareHelper
import com.example.ui.components.QuoteCardItem
import com.example.ui.components.VicdaruBrandHeader
import com.example.viewmodel.VicdaruMainViewModel
import kotlinx.coroutines.launch

private enum class DateRangeFilter(val label: String, val days: Int?) {
    ALL("Todo", null),
    TODAY("Hoy", 1),
    WEEK("7 días", 7),
    MONTH("Este mes", 30)
}

@Composable
fun ArchiveSearchScreen(
    viewModel: VicdaruMainViewModel,
    quotesState: UiState<List<QuoteHistoryEntry>>,
    isOffline: Boolean = false,
    pendingCacheSyncCount: Int = 0,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var selectedAnime by remember { mutableStateOf<AnimeSeries?>(null) }
    var selectedMood by remember { mutableStateOf<String?>(null) }
    var dateRange by remember { mutableStateOf(DateRangeFilter.ALL) }

    val allQuotes = (quotesState as? UiState.Success)?.data.orEmpty()
    val filteredQuotes by remember(allQuotes, query, favoritesOnly, selectedAnime, selectedMood, dateRange) {
        derivedStateOf {
            val nowMs = System.currentTimeMillis()
            val q = query.trim().lowercase()
            allQuotes.filter { item ->
                val matchesQuery = q.isEmpty() ||
                    item.quoteText.lowercase().contains(q) ||
                    item.characterName.lowercase().contains(q) ||
                    AnimeSeries.fromKey(item.animeKey).displayName.lowercase().contains(q)
                val matchesFav = !favoritesOnly || item.favorite
                val matchesAnime = selectedAnime == null || item.animeKey == selectedAnime!!.key
                val matchesMood = selectedMood == null || item.moodTag.equals(selectedMood, ignoreCase = true)
                val matchesDate = dateRange.days == null || run {
                    val sec = item.createdAt?.seconds ?: (nowMs / 1000L)
                    (nowMs / 1000L - sec) <= (dateRange.days!!.toLong() * 86400L)
                }
                matchesQuery && matchesFav && matchesAnime && matchesMood && matchesDate
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            VicdaruBrandHeader(
                title = "Archivo & Favoritos",
                subtitle = "${allQuotes.size} citas en caché local • ${allQuotes.count { it.favorite }} favoritas"
            )
        }

        if (isOffline || pendingCacheSyncCount > 0) {
            item {
                val pendingSuffix = if (pendingCacheSyncCount > 0) {
                    " ($pendingCacheSyncCount por sincronizar)"
                } else {
                    ""
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth().testTag("offline_cache_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isOffline) Icons.Filled.CloudOff else Icons.Filled.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = if (isOffline) {
                                "Sin conexión: consultando tus citas y favoritos desde la caché local$pendingSuffix."
                            } else {
                                "Sincronizando $pendingCacheSyncCount cambios locales con la nube..."
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Buscar cita, personaje o anime...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = {
                            viewModel.haptics.subtleTap()
                            query = ""
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "Limpiar búsqueda")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("archive_search_input")
            )
        }

        // Favorites + Date range row
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = favoritesOnly,
                        onClick = {
                            val next = !favoritesOnly
                            viewModel.haptics.toggleSwitch(next)
                            favoritesOnly = next
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Favoritos") },
                        modifier = Modifier.testTag("filter_favorites_chip")
                    )
                }
                items(DateRangeFilter.entries) { range ->
                    FilterChip(
                        selected = dateRange == range,
                        onClick = {
                            viewModel.haptics.subtleTap()
                            dateRange = range
                        },
                        label = { Text(range.label) }
                    )
                }
            }
        }

        // Anime series filter chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedAnime == null,
                        onClick = {
                            viewModel.haptics.subtleTap()
                            selectedAnime = null
                        },
                        label = { Text("Todos los animes") }
                    )
                }
                items(AnimeSeries.entries) { series ->
                    FilterChip(
                        selected = selectedAnime == series,
                        onClick = {
                            viewModel.haptics.subtleTap()
                            selectedAnime = if (selectedAnime == series) null else series
                        },
                        label = { Text(series.displayName) }
                    )
                }
            }
        }

        // Mood tags filter chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CuratedQuotesCatalog.allMoodTags) { mood ->
                    FilterChip(
                        selected = selectedMood == mood,
                        onClick = {
                            viewModel.haptics.subtleTap()
                            selectedMood = if (selectedMood == mood) null else mood
                        },
                        label = { Text(mood) }
                    )
                }
            }
        }

        when (quotesState) {
            is UiState.Loading -> {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Text(quotesState.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is UiState.Success -> {
                if (filteredQuotes.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.HistoryEdu,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = "No hay citas con esos filtros. Prueba limpiar la búsqueda o pedir una nueva dosis anime.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(filteredQuotes, key = { it.id }) { quote ->
                        QuoteCardItem(
                            entry = quote,
                            isHero = false,
                            showArchiveDetails = true,
                            onStatusMessage = { msg -> viewModel.postMessage(msg) },
                            onToggleFavorite = { viewModel.toggleQuoteFavorite(quote) },
                            onShare = {
                                scope.launch { QuoteShareHelper.shareQuoteCard(context, quote) }
                            }
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}
