package com.zara.challenge.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zara.challenge.ui.detail.CharacterDetailScreen
import com.zara.challenge.ui.favorites.FavoritesScreen
import com.zara.challenge.ui.list.CharacterListScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val topLevelRoutes = listOf("characters", "favorites")

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentRoute in topLevelRoutes) {
                NavigationBar {
                    topLevelRoutes.forEach { route ->
                        val label = if (route == "characters") "Characters" else "Favourites"
                        NavigationBarItem(
                            selected = currentRoute == route,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo("characters") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                if (route == "characters") {
                                    Text("C")
                                } else {
                                    Icon(
                                        imageVector = if (currentRoute == route) {
                                            Icons.Filled.Favorite
                                        } else {
                                            Icons.Outlined.FavoriteBorder
                                        },
                                        contentDescription = null,
                                    )
                                }
                            },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = Color.Black,
                                unselectedIconColor = Color.Black,
                                unselectedTextColor = Color.Black,
                                indicatorColor = Color(0xFFE5E5E5),
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "characters",
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            composable("characters") {
                CharacterListScreen(onCharacterClick = { navController.navigate("character/$it") })
            }
            composable("favorites") {
                FavoritesScreen(
                    onBack = { navController.popBackStack() },
                    onCharacterClick = { navController.navigate("character/$it") },
                )
            }
            composable(
                route = "character/{characterId}",
                arguments = listOf(navArgument("characterId") { type = NavType.IntType }),
            ) {
                CharacterDetailScreen(
                    onBack = { navController.popBackStack() },
                    onCharacterClick = { navController.navigate("character/$it") },
                )
            }
        }
    }
}
