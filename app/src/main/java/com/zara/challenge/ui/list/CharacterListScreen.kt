package com.zara.challenge.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zara.challenge.ui.common.CharacterRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterListScreen(
    onCharacterClick: (Int) -> Unit,
    viewModel: CharacterListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }
    val activeFilterCount = listOf(state.filters.status, state.filters.species, state.filters.gender)
        .count { it != null }

    if (showFilters) {
        CharacterFilterDialog(
            filters = state.filters,
            onApply = {
                viewModel.onFiltersChanged(it)
                showFilters = false
            },
            onDismiss = { showFilters = false },
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Rick & Morty") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChanged,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search characters") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
            )
            TextButton(
                onClick = { showFilters = true },
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Text(if (activeFilterCount == 0) "Filters" else "Filters ($activeFilterCount)")
            }
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
