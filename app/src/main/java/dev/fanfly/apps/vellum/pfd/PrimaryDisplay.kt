package dev.fanfly.apps.vellum.pfd

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.fanfly.apps.vellum.adhars.AdharsViewModel

@Composable
fun PrimaryDisplay(adharsViewModel: AdharsViewModel = hiltViewModel()) {

  val adharsData = adharsViewModel.adharsData.collectAsState()

  Box(
      modifier =
          Modifier.fillMaxSize()
              // Apply safe drawing insets to the Box to keep content
              // from being obscured by system UI.
              .windowInsetsPadding(WindowInsets.safeDrawing)) {
        ArtificialHorizon(
            pitch = adharsData.value.pitch,
            roll = adharsData.value.roll,
            modifier = Modifier.fillMaxSize(),
        )

        // The Button is aligned to the top end of the Box.
        Button(
            // This will call a method on your ViewModel to reset the sensor offsets.
            onClick = { adharsViewModel.calibrate() },
            modifier =
                Modifier.align(Alignment.TopEnd)
                    .padding(16.dp) // Add padding to avoid touching the screen edges.
            ) {
              Text("Calibrate")
            }
      }
}
