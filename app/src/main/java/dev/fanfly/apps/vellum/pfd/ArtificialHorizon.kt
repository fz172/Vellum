package dev.fanfly.apps.vellum.pfd

import android.graphics.Color.WHITE
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import dev.fanfly.apps.vellum.pfd.DisplayConfigs.PITCH_TICK_DEGREES
import dev.fanfly.apps.vellum.theme.COLOR_PFD_GROUND
import dev.fanfly.apps.vellum.theme.COLOR_PFD_SKY
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private object DisplayConfigs {
  const val NUM_OF_TICKS_PITCH = 3
  const val PITCH_TICK_DEGREES = 5
  const val MIN_PITCH = -30
  const val MAX_PITCH = 30
}

private object RollIndicatorConfig {
  val TICK_ANGLES_DEGREES = listOf(-60, -45, -30, -20, -10, 10, 20, 30, 45, 60)
  val LABEL_ANGLES_DEGREES = listOf(10, 20, 30, 45, 60)
  const val MAJOR_TICK_LENGTH = 25f
  const val MEDIUM_TICK_LENGTH = 15f
  const val MINOR_TICK_LENGTH = 10f
}

@Composable
fun ArtificialHorizon(
    pitch: Float, // degrees
    roll: Float, // degrees
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
          size = Size(diagonal, diagonal * 2f))

      // Ground: also overfill downward
      drawRect(
          color = COLOR_PFD_GROUND,
          topLeft = Offset(center.x - diagonal / 2f, center.y + pitchOffset),
          size = Size(diagonal, diagonal * 2))

      // Horizon line
      drawLine(
          color = Color.White,
          start = Offset(center.x - diagonal / 2f, center.y + pitchOffset),
          end = Offset(center.x + diagonal / 2f, center.y + pitchOffset),
          strokeWidth = 4f)
      val minDisplayPitch = pitch - DisplayConfigs.NUM_OF_TICKS_PITCH * PITCH_TICK_DEGREES
      val maxDisplayPitch = pitch + DisplayConfigs.NUM_OF_TICKS_PITCH * PITCH_TICK_DEGREES

      // Pitch ladder (every 5° from -30 to +30)
      for (i in DisplayConfigs.MIN_PITCH..DisplayConfigs.MAX_PITCH step PITCH_TICK_DEGREES) {
        if (i > maxDisplayPitch || i < minDisplayPitch) {
          continue
        }
        val y = center.y + pitchOffset - i * pitchPxPerDeg
        val lineWidth = if (i % 10 == 0) 60f else 30f
        val label = if (i != 0) i.toString() else null

        drawLine(
            color = Color.White,
            start = Offset(center.x - lineWidth, y),
            end = Offset(center.x + lineWidth, y),
            strokeWidth = 2f)

        label?.let {
          drawContext.canvas.nativeCanvas.apply {
            drawText(it, center.x - lineWidth - 25f, y + 10f, labelPaint)
            drawText(it, center.x + lineWidth + 5f, y + 10f, labelPaint)
          }
        }
      }
    }

    // --- Layer 2: Rotating Roll Indicator Scale ---
    // 2. Arc is made smaller and moved down slightly.
    val rollArcRadius = size.width * 0.35f
    val rollArcTopY = 100f // Vertical position from the top of the canvas
    val rollArcCenter = Offset(center.x, rollArcTopY + rollArcRadius)

    rotate(-roll, pivot = rollArcCenter) {
      // Draw a triangle for the 0° mark on the rotating scale
      val zeroAngleRad = Math.toRadians(-90.0).toFloat()
      val triangleSize = 15f
      val topPt =
          Offset(
              rollArcCenter.x + rollArcRadius * cos(zeroAngleRad),
              rollArcCenter.y + rollArcRadius * sin(zeroAngleRad))
      val path =
          Path().apply {
            moveTo(topPt.x, topPt.y)
            // 3. Triangle points outwards.
            lineTo(topPt.x - triangleSize / 2, topPt.y - triangleSize)
            lineTo(topPt.x + triangleSize / 2, topPt.y - triangleSize)
            close()
          }
      drawPath(path, Color.White)

      // Draw the connecting arc for the roll ticks
      drawArc(
          color = Color.White,
          startAngle = -150f, // -90 (top) - 60 degrees
          sweepAngle = 120f, // from -60 to +60 degrees
          useCenter = false,
          topLeft = Offset(rollArcCenter.x - rollArcRadius, rollArcCenter.y - rollArcRadius),
          size = Size(rollArcRadius * 2, rollArcRadius * 2),
          style = Stroke(width = 2f))

      // Draw Ticks
      RollIndicatorConfig.TICK_ANGLES_DEGREES.forEach { angleDeg ->
        val angleRad = Math.toRadians(angleDeg.toDouble() - 90).toFloat()

        val tickLength =
            when {
              abs(angleDeg) % 30 == 0 -> RollIndicatorConfig.MAJOR_TICK_LENGTH // 30, 60
              abs(angleDeg) % 10 == 0 -> RollIndicatorConfig.MEDIUM_TICK_LENGTH // 10, 20
              else -> RollIndicatorConfig.MINOR_TICK_LENGTH // 45
            }

        val start =
            Offset(
                rollArcCenter.x + rollArcRadius * cos(angleRad),
                rollArcCenter.y + rollArcRadius * sin(angleRad))
        // 4. Ticks are drawn outwards from the arc.
        val end =
            Offset(
                rollArcCenter.x + (rollArcRadius + tickLength) * cos(angleRad),
                rollArcCenter.y + (rollArcRadius + tickLength) * sin(angleRad))
        // 5. Ticks are made thicker.
        drawLine(Color.White, start, end, strokeWidth = 3f)
      }
    }

    // --- Layer 3: Fixed Aircraft Symbol and Roll Pointer ---
    val fixedPointerSize = 20f
    val fixedPointerPath =
        Path().apply {
          moveTo(center.x, rollArcTopY)
          lineTo(center.x - fixedPointerSize / 2, rollArcTopY - fixedPointerSize)
          lineTo(center.x + fixedPointerSize / 2, rollArcTopY - fixedPointerSize)
          close()
        }
    drawPath(fixedPointerPath, color = Color.White)

    drawLine(
        color = Color.Yellow,
        start = Offset(center.x - 50f, center.y),
        end = Offset(center.x + 50f, center.y),
        strokeWidth = 6f)
    drawLine(
        color = Color.Yellow,
        start = Offset(center.x, center.y),
        end = Offset(center.x, center.y + 20f),
        strokeWidth = 6f)
  }
}
