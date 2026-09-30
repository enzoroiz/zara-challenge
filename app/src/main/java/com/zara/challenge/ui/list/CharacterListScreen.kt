package com.zara.challenge.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zara.challenge.ui.common.CharacterRow
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterListScreen(
    onCharacterClick: (Int) -> Unit,
    viewModel: CharacterListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }
    var menuWidthPx by remember { mutableStateOf(0) }
    val menuProgress by animateFloatAsState(
        targetValue = if (showFilters) 1f else 0f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "filterMenuProgress",
    )
    val activeFilterCount = listOf(state.filters.status, state.filters.gender)
        .count { it != null }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Rick & Morty") },
                    actions = {
                        IconButton(onClick = { showFilters = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = "Filter characters",
                                tint = if (activeFilterCount > 0) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    },
                )
            },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChanged,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search characters") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                )
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        state.isLoading && state.characters.isEmpty() -> LoadingBox()
                        state.error != null && state.characters.isEmpty() -> ErrorBox(state.error!!, viewModel::retry)
                        state.characters.isEmpty() -> EmptyBox()
                        else -> Column(Modifier.fillMaxSize()) {
                            if (state.error != null) {
                                Text(
                                    state.error!!,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(state.characters, key = { it.id }) { character ->
                                    CharacterRow(
                                        character = character,
                                        onClick = { onCharacterClick(character.id) },
                                        isFavorite = character.id in state.favoriteIds,
                                        onFavoriteClick = { viewModel.onFavoriteClick(character) },
                                    )
                                }
                                if (!state.endReached) {
                                    item {
                                        Box(
                                            Modifier.fillMaxWidth().padding(20.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (state.isLoadingMore) CircularProgressIndicator()
                                            else TextButton(onClick = viewModel::loadMore) { Text("Load more") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (showFilters || menuProgress > 0f) {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.32f * menuProgress)),
                )
                Row(Modifier.fillMaxSize()) {
                    CharacterFilterMenu(
                        filters = state.filters,
                        onFilterChanged = viewModel::onFiltersChanged,
                        onDismiss = { showFilters = false },
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.85f)
                            .widthIn(max = 360.dp)
                            .onSizeChanged { menuWidthPx = it.width }
                            .offset {
                                IntOffset(
                                    x = (-menuWidthPx * (1f - menuProgress)).roundToInt(),
                                    y = 0,
                                )
                            },
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(enabled = showFilters) { showFilters = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingBox() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    CircularProgressIndicator()
}

@Composable
private fun EmptyBox() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Text("No characters found")
}

@Composable
private fun ErrorBox(message: String, retry: () -> Unit) =
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message)
        IconButton(onClick = retry) { Text("Retry") }
    }
