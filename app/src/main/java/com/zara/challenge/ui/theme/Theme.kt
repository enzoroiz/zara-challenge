package com.zara.challenge.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.zara.challenge.R

val InterFont = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

private val ZaraChallengeTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = InterFont),
        displayMedium = displayMedium.copy(fontFamily = InterFont),
        displaySmall = displaySmall.copy(fontFamily = InterFont),
        headlineLarge = headlineLarge.copy(fontFamily = InterFont),
        headlineMedium = headlineMedium.copy(fontFamily = InterFont),
        headlineSmall = headlineSmall.copy(fontFamily = InterFont),
        titleLarge = titleLarge.copy(fontFamily = InterFont),
        titleMedium = titleMedium.copy(fontFamily = InterFont),
        titleSmall = titleSmall.copy(fontFamily = InterFont),
        bodyLarge = bodyLarge.copy(fontFamily = InterFont),
        bodyMedium = bodyMedium.copy(fontFamily = InterFont),
        bodySmall = bodySmall.copy(fontFamily = InterFont),
        labelLarge = labelLarge.copy(fontFamily = InterFont),
        labelMedium = labelMedium.copy(fontFamily = InterFont),
        labelSmall = labelSmall.copy(fontFamily = InterFont)
    )
}

private val ZaraChallengeColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E5E5),
    onPrimaryContainer = Color.Black,
    secondary = Color.Black,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5E5E5),
    onSecondaryContainer = Color.Black,
    tertiary = Color.Black,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE5E5E5),
    onTertiaryContainer = Color.Black,
    surfaceTint = Color.Black,
)

@Composable
fun ZaraChallengeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ZaraChallengeColorScheme,
        typography = ZaraChallengeTypography,
        content = content
    )
}
