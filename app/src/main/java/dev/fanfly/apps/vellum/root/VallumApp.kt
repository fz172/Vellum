package dev.fanfly.apps.vellum.root

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.fanfly.apps.vellum.artificialhorizon.ui.ArtificialHorizonDisplay
import dev.fanfly.apps.vellum.nav.NavScreen

@Composable
fun VallumApp() {

  val navController = rememberNavController()

  NavHost(
    navController,
    startDestination = NavScreen.ArtificialHorizon.routing
  ) {
    composable(NavScreen.ArtificialHorizon.routing) {
      ArtificialHorizonDisplay()
    }
  }
}