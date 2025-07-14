package dev.fanfly.apps.vellum.root

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.fanfly.apps.vellum.pfd.PrimaryDisplay
import dev.fanfly.apps.vellum.root.nav.NavScreen

@Composable
fun VallumApp() {

  val navController = rememberNavController()

  NavHost(
    navController,
    startDestination = NavScreen.PFD.routing
  ) {
    composable(NavScreen.PFD.routing) {
      PrimaryDisplay()
    }
  }
}