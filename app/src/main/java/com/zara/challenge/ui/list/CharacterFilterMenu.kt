package com.zara.challenge.ui.list

import androidx.compose.ui.res.stringResource
import com.zara.challenge.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
                Text(
                    stringResource(R.string.filters_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Black,
                )

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Check, contentDescription = stringResource(R.string.close_filters))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            FilterSection(
                title = stringResource(R.string.filter_gender),
                options = listOf(
                    "female" to R.string.gender_female,
                    "male" to R.string.gender_male,
                    "genderless" to R.string.gender_genderless,
                    "unknown" to R.string.option_unknown,
                ),
                selection = filters.gender,
                onSelected = { selected ->
                    onFilterChanged(filters.copy(gender = selected.takeUnless { it == filters.gender }))
                },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            FilterSection(
                title = stringResource(R.string.filter_status),
                options = listOf(
                    "alive" to R.string.status_alive,
                    "dead" to R.string.status_dead,
                    "unknown" to R.string.option_unknown,
                ),
                selection = filters.status,
                onSelected = { selected ->
                    onFilterChanged(filters.copy(status = selected.takeUnless { it == filters.status }))
                },
            )

            OutlinedButton(
                onClick = { onFilterChanged(CharacterFilters()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                border = BorderStroke(1.dp, Color.Black),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = Color.Black,
                ),
            ) {
                Text(stringResource(R.string.reset_filters))
            }
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    options: List<Pair<String, Int>>,
    selection: String?,
    onSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = Color.Black,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )

        options.forEach { (option, labelRes) ->
            val selected = option == selection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (selected) {
                            Modifier.background(
                                Color(0xFFE5E5E5),
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
                        contentDescription = stringResource(R.string.selected),
                        tint = Color.Black,
                    )
                } else {
                    Spacer(Modifier.padding(start = 24.dp))
                }

                Text(
                    stringResource(labelRes),
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
