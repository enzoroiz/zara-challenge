package com.zara.challenge.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zara.challenge.domain.model.CharacterFilters

@Composable
fun CharacterFilterMenu(
    filters: CharacterFilters,
    onFilterChanged: (CharacterFilters) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Filters", style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close filters")
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            FilterSection(
                title = "Gender",
                options = listOf("female", "male", "genderless", "unknown"),
                selection = filters.gender,
                onSelected = { selected ->
                    onFilterChanged(filters.copy(gender = selected.takeUnless { it == filters.gender }))
                },
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            FilterSection(
                title = "Status",
                options = listOf("alive", "dead", "unknown"),
                selection = filters.status,
                onSelected = { selected ->
                    onFilterChanged(filters.copy(status = selected.takeUnless { it == filters.status }))
                },
            )
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    options: List<String>,
    selection: String?,
    onSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        options.forEach { option ->
            val selected = option == selection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (selected) {
                            Modifier.background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                RoundedCornerShape(12.dp),
                            )
                        } else {
                            Modifier
                        },
                    )
                    .clickable { onSelected(option) }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Spacer(Modifier.padding(start = 24.dp))
                }
                Text(
                    option.replaceFirstChar { it.uppercase() },
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    style = if (selected) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
