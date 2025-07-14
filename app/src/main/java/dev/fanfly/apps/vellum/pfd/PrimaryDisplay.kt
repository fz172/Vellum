package dev.fanfly.apps.vellum.pfd

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PrimaryDisplay() {
  var pitch by remember { mutableFloatStateOf(0f) }
  var roll by remember { mutableFloatStateOf(0f) }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    ArtificialHorizon(pitch = pitch, roll = roll, modifier = Modifier.size(500.dp))

    Spacer(Modifier.height(16.dp))

    Slider(value = pitch, onValueChange = { pitch = it }, valueRange = -45f..45f)
    Text("Pitch: ${pitch.toInt()}°")

    Slider(value = roll, onValueChange = { roll = it }, valueRange = -90f..90f)
    Text("Roll: ${roll.toInt()}°")
  }
}


