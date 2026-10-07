package com.zara.challenge.ui.detail

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.zara.challenge.R
import androidx.compose.foundation.background
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.tooling.preview.Preview
import com.zara.challenge.ui.theme.ZaraChallengeTheme
import coil3.compose.AsyncImage
import com.zara.challenge.domain.model.Character
import com.zara.challenge.ui.common.CharacterCarousel
import com.zara.challenge.ui.common.CharacterStatusIndicator
import com.zara.challenge.ui.common.ImagePlaceholder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterDetailScreen(
    onBack: () -> Unit,
    onCharacterClick: (Int) -> Unit,
    viewModel: CharacterDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(viewModel, snackbarHostState, context) {
        viewModel.errorEvents.collect { message ->
            snackbarHostState.showSnackbar(message.asString(context))
        }
    }
    LaunchedEffect(state) {
        if (state == CharacterDetailUiState.Unavailable) {
            launch {
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.character_unavailable),
                    duration = SnackbarDuration.Indefinite,
                )
            }
            delay(3.seconds)
            onBack()
        }
    }
    Box(Modifier.fillMaxSize()) {
        when (val current = state) {
            CharacterDetailUiState.Loading -> Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }
            CharacterDetailUiState.Unavailable -> CharacterUnavailableContent(
                modifier = Modifier.fillMaxSize(),
            )
            is CharacterDetailUiState.Success -> CharacterContent(
                current,
                onCharacterClick = onCharacterClick,
                onFavoriteClick = viewModel::toggleFavorite,
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
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
                                stringResource(R.string.remove_from_favorites)
                            } else {
                                stringResource(R.string.add_to_favorites)
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
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun CharacterUnavailableContent(modifier: Modifier) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color.LightGray),
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(
                start = 20.dp,
                top = 4.dp,
                end = 20.dp,
                bottom = 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("-", style = MaterialTheme.typography.headlineLarge, color = Color.Black)
            Text("-", style = MaterialTheme.typography.titleLarge, color = Color.Black)
            Text("-", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
            HorizontalDivider(color = Color(0xFFE5E5E5), thickness = 1.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LocationInfo(title = stringResource(R.string.detail_origin), value = "-", modifier = Modifier.weight(1f))
                LocationInfo(title = stringResource(R.string.detail_location), value = "-", modifier = Modifier.weight(1f))
            }
            HorizontalDivider(color = Color(0xFFE5E5E5), thickness = 1.dp)
            Text(
                stringResource(R.string.detail_section_title),
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black,
                fontWeight = FontWeight.Normal,
            )
            DetailRow(stringResource(R.string.detail_type), "-")
            DetailRow(stringResource(R.string.detail_episodes), "-")
        }
    }
}

@Composable
private fun CharacterContent(
    state: CharacterDetailUiState.Success,
    onCharacterClick: (Int) -> Unit,
    onFavoriteClick: (Character) -> Unit,
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
            placeholder = ImagePlaceholder,
            error = ImagePlaceholder,
            fallback = ImagePlaceholder,
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
                    title = stringResource(R.string.detail_origin),
                    value = character.originName,
                    modifier = Modifier.weight(1f),
                )
                LocationInfo(
                    title = stringResource(R.string.detail_location),
                    value = character.locationName,
                    modifier = Modifier.weight(1f),
                )
            }
            HorizontalDivider(color = Color(0xFFE5E5E5), thickness = 1.dp)
            Text(
                stringResource(R.string.detail_section_title),
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black,
                fontWeight = FontWeight.Normal,
            )
            DetailRow(stringResource(R.string.detail_type), character.type.ifBlank { "-" })
            DetailRow(stringResource(R.string.detail_episodes), character.episodeCount.toString())
            if (state.similarCharacters.isNotEmpty()) {
                CharacterCarousel(
                    title = stringResource(R.string.similar_characters),
                    characters = state.similarCharacters,
                    onCharacterClick = onCharacterClick,
                    favoriteIds = state.favoriteIds,
                    onFavoriteClick = onFavoriteClick,
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

@Preview(showBackground = true)
@Composable
private fun CharacterContentPreview() {
    fun character(id: Int, name: String) = Character(
        id = id,
        name = name,
        status = "Alive",
        species = "Human",
        type = "",
        gender = "Male",
        originName = "Earth (C-137)",
        locationName = "Citadel of Ricks",
        image = "",
        episodeCount = 51,
        created = "2017-11-04",
    )
    ZaraChallengeTheme {
        CharacterContent(
            state = CharacterDetailUiState.Success(
                character = character(1, "Rick Sanchez"),
                similarCharacters = listOf(character(2, "Rick Prime"), character(3, "Rick Jr.")),
                favoriteIds = setOf(1),
                isFavorite = true,
            ),
            onCharacterClick = {},
            onFavoriteClick = {},
            modifier = Modifier,
        )
    }
}
