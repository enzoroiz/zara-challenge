package com.zara.challenge.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.zara.challenge.ui.common.CharacterCarousel
import com.zara.challenge.ui.common.CharacterStatusIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterDetailScreen(
    onBack: () -> Unit,
    onCharacterClick: (Int) -> Unit,
    viewModel: CharacterDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
        when (val current = state) {
            CharacterDetailUiState.Loading -> Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }
            is CharacterDetailUiState.Error -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) { Text(current.message) }
            is CharacterDetailUiState.Success -> CharacterContent(
                current,
                onCharacterClick = onCharacterClick,
                modifier = Modifier.fillMaxSize(),
            )
        }
        TopAppBar(
            modifier = Modifier.align(Alignment.TopCenter),
            title = {},
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
            ),
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            },
            actions = {
                val characterState = state as? CharacterDetailUiState.Success
                if (characterState != null) {
                    IconButton(onClick = viewModel::toggleFavorite) {
                        Icon(
                            imageVector = if (characterState.isFavorite) {
                                Icons.Filled.Favorite
                            } else {
                                Icons.Outlined.FavoriteBorder
                            },
                            contentDescription = if (characterState.isFavorite) {
                                "Remove from favorites"
                            } else {
                                "Add to favorites"
                            },
                            tint = if (characterState.isFavorite) {
                                Color.Red
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun CharacterContent(
    state: CharacterDetailUiState.Success,
    onCharacterClick: (Int) -> Unit,
    modifier: Modifier,
) {
    val character = state.character
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AsyncImage(
            model = character.image,
            contentDescription = character.name,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(
                start = 20.dp,
                top = 4.dp,
                end = 20.dp,
                bottom = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                character.name,
                style = MaterialTheme.typography.headlineLarge,
                color = Color.Black,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CharacterStatusIndicator(character.status)
                Text(
                    character.status,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Black,
                )
            }
            Text(
                "${character.species} - ${character.gender}",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray,
            )
            HorizontalDivider(color = Color(0xFFE5E5E5), thickness = 1.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LocationInfo(
                    title = "ORIGIN",
                    value = character.originName,
                    modifier = Modifier.weight(1f),
                )
                LocationInfo(
                    title = "LOCATION",
                    value = character.locationName,
                    modifier = Modifier.weight(1f),
                )
            }
            HorizontalDivider(color = Color(0xFFE5E5E5), thickness = 1.dp)
            Text(
                "CHARACTER DETAILS",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black,
                fontWeight = FontWeight.Normal,
            )
            DetailRow("TYPE", character.type.ifBlank { "-" })
            DetailRow("EPISODES", character.episodeCount.toString())
            state.actionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.recommendationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.similarCharacters.isNotEmpty()) {
                CharacterCarousel(
                    title = "Characters like you",
                    characters = state.similarCharacters,
                    onCharacterClick = onCharacterClick,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun LocationInfo(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = Color.Black)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
        )
        Text(
            value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Black,
        )
    }
}
