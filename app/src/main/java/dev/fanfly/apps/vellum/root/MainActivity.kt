package dev.fanfly.apps.vellum.root

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Scaffold
import dagger.hilt.android.AndroidEntryPoint
import dev.fanfly.apps.vellum.theme.VellumTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      VellumTheme {
        Scaffold { VallumApp() }
      }
    }
  }
}
