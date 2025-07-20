package dev.fanfly.apps.vellum.pfd

import android.graphics.Color.WHITE
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import dev.fanfly.apps.vellum.theme.COLOR_PFD_GROUND
import dev.fanfly.apps.vellum.theme.COLOR_PFD_SKY
import kotlin.math.hypot

@Composable
fun ArtificialHorizon(
  pitch: Float, // degrees
  roll: Float,  // degrees
  modifier: Modifier = Modifier,
  ) {
  Canvas(modifier = modifier) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val pitchPxPerDeg = size.height / 60f // ±30° visible
    val pitchOffset = pitch * pitchPxPerDeg

    val labelPaint by lazy {
      Paint().apply {
        color = WHITE
        textSize = 30f
      }
    }

    // 🔥 Diagonal size: ensures full coverage during rotation
    val diagonal = hypot(size.width, size.height) * 4

    // Rotate horizon by roll angle
    rotate(-roll, pivot = center) {
      // Sky: cover entire diagonal area
      drawRect(
        color = COLOR_PFD_SKY,
        topLeft = Offset(center.x - diagonal / 2f, center.y - diagonal + pitchOffset),
        size = Size(diagonal, diagonal * 2f)
      )

      // Ground: also overfill downward
      drawRect(
        color = COLOR_PFD_GROUND,
        topLeft = Offset(center.x - diagonal / 2f, center.y + pitchOffset),
        size = Size(diagonal, diagonal * 2)
      )

      // Horizon line
      drawLine(
        color = Color.White,
        start = Offset(center.x - diagonal / 2f, center.y + pitchOffset),
        end = Offset(center.x + diagonal / 2f, center.y + pitchOffset),
        strokeWidth = 4f
      )

      // Pitch ladder (every 5° from -30 to +30)
      for (i in -30..30 step 5) {
        val y = center.y + pitchOffset - i * pitchPxPerDeg
        val lineWidth = if (i % 10 == 0) 60f else 30f
        val label = if (i != 0) i.toString() else null

        drawLine(
          color = Color.White,
          start = Offset(center.x - lineWidth, y),
          end = Offset(center.x + lineWidth, y),
          strokeWidth = 2f
        )

        label?.let {
          drawContext.canvas.nativeCanvas.apply {
            drawText(
              it,
              center.x - lineWidth - 25f,
              y + 10f,
              labelPaint
            )
            drawText(
              it,
              center.x + lineWidth + 5f,
              y + 10f,
              labelPaint
            )
          }
        }
      }
    }

    // Fixed aircraft symbol (yellow wings)
    drawLine(
      color = Color.Yellow,
      start = Offset(center.x - 50f, center.y),
      end = Offset(center.x + 50f, center.y),
      strokeWidth = 6f
    )
    drawLine(
      color = Color.Yellow,
      start = Offset(center.x, center.y),
      end = Offset(center.x, center.y + 20f),
      strokeWidth = 6f
    )
  }
}