package dev.fanfly.apps.vellum.pfd.classic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
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
fun ArtificialHorizonClassic(
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