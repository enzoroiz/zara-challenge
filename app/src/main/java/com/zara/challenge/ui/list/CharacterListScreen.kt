package com.zara.challenge.ui.list

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.zara.challenge.R
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
import androidx.compose.material.icons.filled.Close
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
import com.zara.challenge.domain.model.Character
import com.zara.challenge.ui.common.CharacterCard
import com.zara.challenge.ui.theme.ZaraChallengeTheme
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterListScreen(
    onCharacterClick: (Int) -> Unit,
    viewModel: CharacterListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(viewModel, snackbarHostState, context) {
        viewModel.errorEvents.collect { message ->
            snackbarHostState.showSnackbar(message.asString(context))
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
                    title = { Text(stringResource(R.string.app_title)) },
                    actions = {
                        IconButton(onClick = { showFilters = true }) {
                            Box {
                                Icon(
                                    imageVector = Icons.Outlined.Tune,
                                    contentDescription = if (activeFilterCount > 0) {
                                        stringResource(R.string.filter_characters_active, activeFilterCount)
                                    } else {
                                        stringResource(R.string.filter_characters)
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
            CharacterListContent(
                state = state,
                onQueryChanged = viewModel::onQueryChanged,
                onCharacterClick = onCharacterClick,
                onFavoriteClick = viewModel::onFavoriteClick,
                onLoadMore = viewModel::loadMore,
                onRetry = viewModel::retry,
                modifier = Modifier.padding(padding),
            )
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
private fun CharacterListContent(
    state: CharacterListUiState,
    onQueryChanged: (String) -> Unit,
    onCharacterClick: (Int) -> Unit,
    onFavoriteClick: (Character) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text(stringResource(R.string.search_characters)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChanged("") }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.clear_search),
                        )
                    }
                }
            },
            singleLine = true,
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading -> LoadingBox()
                state.hasLoadError && state.characters.isEmpty() ->
                    ErrorBox(onRetry)
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
                                onFavoriteClick = { onFavoriteClick(character) },
                            )
                        }
                        if (!state.endReached) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(
                                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    OutlinedButton(
                                        onClick = onLoadMore,
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
                                            Text(stringResource(R.string.load_more))
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

@Composable
private fun LoadingBox() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    CircularProgressIndicator()
}

@Composable
private fun EmptyBox() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Text(stringResource(R.string.no_characters))
}

@Composable
private fun ErrorBox(retry: () -> Unit) =
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.load_characters_error))
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
            Text(stringResource(R.string.retry))
        }
    }

@Preview(showBackground = true)
@Composable
private fun CharacterListContentSuccessPreview() {
    val characters = listOf(
        Character(1, "Rick Sanchez", "Alive", "Human", "", "Male", "Earth (C-137)", "Citadel of Ricks", "", 51, "2017-11-04"),
        Character(2, "Morty Smith", "Alive", "Human", "", "Male", "Earth (C-137)", "Earth (Replacement Dimension)", "", 51, "2017-11-04"),
        Character(3, "Summer Smith", "Alive", "Human", "", "Female", "Earth (Replacement Dimension)", "Earth (Replacement Dimension)", "", 42, "2017-11-04"),
        Character(4, "Birdperson", "Dead", "Bird-Person", "", "Male", "Bird World", "Bird World", "", 10, "2017-11-04"),
    )
    ZaraChallengeTheme {
        CharacterListContent(
            state = CharacterListUiState(
                characters = characters,
                isLoading = false,
                favoriteIds = setOf(1),
            ),
            onQueryChanged = {},
            onCharacterClick = {},
            onFavoriteClick = {},
            onLoadMore = {},
            onRetry = {},
        )
    }
}
