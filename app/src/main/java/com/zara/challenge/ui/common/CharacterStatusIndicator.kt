package com.zara.challenge.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CharacterStatusIndicator(status: String) {
    when (status.trim().lowercase()) {
        "alive" -> Box(
            Modifier
                .size(8.dp)
                .background(Color(0xFF2E7D32), CircleShape),
        )
        "dead" -> Text(
            text = "×",
            color = Color.Black,
            fontWeight = FontWeight.Bold,
        )
        else -> Box(
            modifier = Modifier.size(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(Color.Gray, CircleShape),
            )
        }
    }
}
