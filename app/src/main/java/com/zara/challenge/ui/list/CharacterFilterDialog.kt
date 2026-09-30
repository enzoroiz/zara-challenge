package com.zara.challenge.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zara.challenge.domain.model.CharacterFilters

@Composable
fun CharacterFilterDialog(
    filters: CharacterFilters,
    onApply: (CharacterFilters) -> Unit,
    onDismiss: () -> Unit,
) {
    var status by remember(filters) { mutableStateOf(filters.status) }
    var species by remember(filters) { mutableStateOf(filters.species.orEmpty()) }
    var gender by remember(filters) { mutableStateOf(filters.gender) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter characters") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterDropdown("Status", status, listOf(null, "alive", "dead", "unknown")) { status = it }
                OutlinedTextField(
                    value = species,
                    onValueChange = { species = it },
                    label = { Text("Species") },
                    placeholder = { Text("For example, Human") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FilterDropdown(
                    "Gender",
                    gender,
                    listOf(null, "female", "male", "genderless", "unknown"),
                ) { gender = it }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(
                        CharacterFilters(
                            status = status,
                            species = species.trim().takeIf(String::isNotEmpty),
                            gender = gender,
                        ),
                    )
                },
            ) { Text("Apply") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    onApply(CharacterFilters())
                }) { Text("Clear") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun FilterDropdown(
    label: String,
    selection: String?,
    options: List<String?>,
    onSelected: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label)
        androidx.compose.foundation.layout.Box {
            TextButton(onClick = { expanded = true }) {
                Text(selection?.replaceFirstChar { it.uppercase() } ?: "Any")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option?.replaceFirstChar { it.uppercase() } ?: "Any") },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
