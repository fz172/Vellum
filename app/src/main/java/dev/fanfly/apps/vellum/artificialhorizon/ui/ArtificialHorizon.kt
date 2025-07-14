package dev.fanfly.apps.vellum.artificialhorizon.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

@Composable
fun ArtificialHorizonDisplay() {
  var pitch by remember { mutableFloatStateOf(0f) }
  var roll by remember { mutableFloatStateOf(0f) }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    ArtificialHorizon(pitch = pitch, roll = roll, modifier = Modifier.size(300.dp))

    Spacer(Modifier.height(16.dp))

    Slider(value = pitch, onValueChange = { pitch = it }, valueRange = -45f..45f)
    Text("Pitch: ${pitch.toInt()}°")

    Slider(value = roll, onValueChange = { roll = it }, valueRange = -90f..90f)
    Text("Roll: ${roll.toInt()}°")
  }
}

@Composable
private fun ArtificialHorizon(
  pitch: Float, // in degrees: +up, -down
  roll: Float,  // in degrees: +right bank, -left bank
  modifier: Modifier = Modifier.size(200.dp),
) {
  Box(modifier = modifier) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(size.width / 2f, size.height / 2f)
      val radius = size.minDimension / 2f

      // Create circular clip path
      val circlePath = Path().apply {
        addOval(Rect(center = center, radius = radius))
      }

      clipPath(circlePath) {
        // Convert pitch to pixel offset
        val pitchOffset = pitch * radius / 45f

        rotate(-roll, pivot = center) {
          // Sky
          drawRect(
            color = Color.Cyan,
            topLeft = Offset(0f, -size.height + center.y + pitchOffset),
            size = Size(size.width, size.height * 2f)
          )

          // Ground
          drawRect(
            color = Color(0xFF8B4513), // brown
            topLeft = Offset(0f, center.y + pitchOffset),
            size = Size(size.width, size.height)
          )

          // Horizon line
          drawLine(
            color = Color.White,
            start = Offset(0f, center.y + pitchOffset),
            end = Offset(size.width, center.y + pitchOffset),
            strokeWidth = 4f
          )
        }
      }

      // Aircraft fixed symbol (static)
      drawLine(
        color = Color.Yellow,
        start = Offset(center.x - 40f, center.y),
        end = Offset(center.x + 40f, center.y),
        strokeWidth = 6f
      )

      // Outer circle border
      drawCircle(
        color = Color.Black,
        radius = radius,
        center = center,
        style = Stroke(width = 6f)
      )
    }
  }
}