package com.zara.challenge.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zara.challenge.domain.model.Character

@Composable
fun CharacterRow(
    character: Character,
    onClick: () -> Unit,
    isFavorite: Boolean? = null,
    onFavoriteClick: (() -> Unit)? = null,
) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = character.image,
                contentDescription = character.name,
                modifier = Modifier.padding(end = 12.dp).size(80.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(character.name, style = MaterialTheme.typography.titleMedium)
                Text("${character.status} · ${character.species}", style = MaterialTheme.typography.bodyMedium)
                Text(character.locationName, style = MaterialTheme.typography.bodySmall)
            }
            if (onFavoriteClick != null) {
                TextButton(onClick = onFavoriteClick) {
                    Text(if (isFavorite == true) "★" else "☆")
                }
            }
        }
    }
}
