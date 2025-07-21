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
import androidx.compose.ui.graphics.drawscope.DrawScope
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
    // Pre-calculate shared values
    val center = Offset(size.width / 2f, size.height / 2f)
    val pitchPxPerDeg = size.height / 60f // ±30° visible
    val pitchOffset = pitch * pitchPxPerDeg

    val labelPaint by lazy {
      Paint().apply {
        color = WHITE
        textSize = 30f
      }
    }

    // Geometry for the roll indicator
    val rollArcRadius = size.width * 0.35f
    val rollArcTopY = 100f // Vertical position from the top of the canvas
    val rollArcCenter = Offset(center.x, rollArcTopY + rollArcRadius)

    // Draw the layers in order
    drawMovingHorizonLayer(pitch, roll, pitchOffset, pitchPxPerDeg, labelPaint)
    drawRollIndicatorLayer(roll, rollArcRadius, rollArcTopY, rollArcCenter)
    drawFixedForegroundLayer(rollArcTopY)
  }
}

/**
 * Layer 1: Draws the moving background including the sky, ground, horizon line, and the pitch
 * ladder. This entire layer rotates with the roll angle.
 */
private fun DrawScope.drawMovingHorizonLayer(
    pitch: Float,
    roll: Float,
    pitchOffset: Float,
    pitchPxPerDeg: Float,
    labelPaint: Paint,
) {
  val diagonal = hypot(size.width, size.height) * 4
  rotate(-roll, pivot = center) {
    // Sky
    drawRect(
        color = COLOR_PFD_SKY,
        topLeft = Offset(center.x - diagonal / 2f, center.y - diagonal + pitchOffset),
        size = Size(diagonal, diagonal * 2f))

    // Ground
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

    // Pitch ladder
    val minDisplayPitch = pitch - DisplayConfigs.NUM_OF_TICKS_PITCH * PITCH_TICK_DEGREES
    val maxDisplayPitch = pitch + DisplayConfigs.NUM_OF_TICKS_PITCH * PITCH_TICK_DEGREES
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
}

/**
 * Layer 2: Draws the rotating roll indicator scale, including the arc, ticks, and the zero-degree
 * triangle pointer.
 */
private fun DrawScope.drawRollIndicatorLayer(
    roll: Float,
    rollArcRadius: Float,
    rollArcTopY: Float,
    rollArcCenter: Offset,
) {
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
            abs(angleDeg) % 30 == 0 -> RollIndicatorConfig.MAJOR_TICK_LENGTH
            abs(angleDeg) % 10 == 0 -> RollIndicatorConfig.MEDIUM_TICK_LENGTH
            else -> RollIndicatorConfig.MINOR_TICK_LENGTH
          }

      val start =
          Offset(
              rollArcCenter.x + rollArcRadius * cos(angleRad),
              rollArcCenter.y + rollArcRadius * sin(angleRad))
      val end =
          Offset(
              rollArcCenter.x + (rollArcRadius + tickLength) * cos(angleRad),
              rollArcCenter.y + (rollArcRadius + tickLength) * sin(angleRad))
      drawLine(Color.White, start, end, strokeWidth = 3f)
    }
  }
}

/**
 * Layer 3: Draws the fixed elements that do not move, such as the aircraft symbol and the static
 * roll pointer.
 */
private fun DrawScope.drawFixedForegroundLayer(rollArcTopY: Float) {
  // Fixed roll pointer
  val fixedPointerSize = 20f
  val fixedPointerPath =
      Path().apply {
        moveTo(center.x, rollArcTopY)
        lineTo(center.x - fixedPointerSize / 2, rollArcTopY - fixedPointerSize)
        lineTo(center.x + fixedPointerSize / 2, rollArcTopY - fixedPointerSize)
        close()
      }
  drawPath(fixedPointerPath, color = Color.White)

  // Fixed aircraft symbol (yellow wings)
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
