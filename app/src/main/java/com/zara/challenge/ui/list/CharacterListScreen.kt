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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.zara.challenge.domain.model.CharacterFilters
import com.zara.challenge.ui.common.CharacterCard
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterListScreen(
    onCharacterClick: (Int) -> Unit,
    viewModel: CharacterListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.errorEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
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
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("RICK & MORTY") },
                    actions = {
                        IconButton(onClick = { showFilters = true }) {
                            Box {
                                Icon(
                                    imageVector = Icons.Outlined.Tune,
                                    contentDescription = if (activeFilterCount > 0) {
                                        "Filter characters, $activeFilterCount active"
                                    } else {
                                        "Filter characters"
                                    },
                                    tint = Color.Black,
                                )
                                if (activeFilterCount > 0) {
                                    Box(
                                        Modifier
                                            .align(Alignment.TopStart)
                                            .offset(x = (-2).dp, y = (-2).dp)
                                            .size(9.dp)
                                            .border(
                                                width = 1.5.dp,
                                                color = MaterialTheme.colorScheme.surface,
                                                shape = CircleShape,
                                            )
                                            .background(
                                                color = MaterialTheme.colorScheme.error,
                                                shape = CircleShape,
                                            ),
                                    )
                                }
                            }
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
                        state.isLoading -> LoadingBox()
                        state.hasLoadError && state.characters.isEmpty() ->
                            ErrorBox { viewModel.retry() }
                        state.characters.isEmpty() -> EmptyBox()
                        else -> Column(Modifier.fillMaxSize()) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(state.characters, key = { it.id }) { character ->
                                    CharacterCard(
                                        character = character,
                                        onClick = { onCharacterClick(character.id) },
                                        modifier = Modifier.fillMaxWidth(),
                                        isFavorite = character.id in state.favoriteIds,
                                        onFavoriteClick = { viewModel.onFavoriteClick(character) },
                                    )
                                }
                                if (!state.endReached) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        Box(
                                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            OutlinedButton(
                                                onClick = viewModel::loadMore,
                                                modifier = Modifier.fillMaxWidth(),
                                                enabled = !state.isLoadingMore,
                                                border = BorderStroke(1.dp, Color.Black),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.surface,
                                                    contentColor = Color.Black,
                                                ),
                                            ) {
                                                if (state.isLoadingMore) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(20.dp),
                                                        color = Color.Black,
                                                        strokeWidth = 2.dp,
                                                    )
                                                } else {
                                                    Text("LOAD MORE")
                                                }
                                            }
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
private fun ErrorBox(retry: () -> Unit) =
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Unable to load characters")
        OutlinedButton(
            onClick = { retry() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            border = BorderStroke(1.dp, Color.Black),
            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Color.Black,
            ),
        ) {
            Text("RETRY")
        }
    }
