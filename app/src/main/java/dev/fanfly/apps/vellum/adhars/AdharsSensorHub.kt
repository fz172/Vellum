package dev.fanfly.apps.vellum.adhars

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.fanfly.apps.vellum.proto.adharsData
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.sqrt

@Singleton
class AdharsSensorHub @Inject internal constructor(
  @ApplicationContext private val context: Context,
) :
  SensorEventListener {

  private val sensorManager: SensorManager = context.getSystemService(SensorManager::class.java)

  // Get the WindowManager to detect screen rotation.
  private val windowManager = context.getSystemService(WindowManager::class.java)
  private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

  private var sensorUpdateListener: SensorUpdateListener? = null

  // Variables to store the initial orientation as an offset.
  private var pitchOffset: Float = 0f
  private var rollOffset: Float = 0f
  private var rotation: Int = windowManager.defaultDisplay.rotation
  private var isZeroPositionSet: Boolean = false
  private val gravity = DoubleArray(3)

  init {
    calibrate()
  }

  fun startListening(listener: SensorUpdateListener) {
    // Reset the zero position each time listening starts.
    isZeroPositionSet = false
    sensorUpdateListener = listener
    sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
  }

  fun stopListening() {
    sensorManager.unregisterListener(this)
    sensorUpdateListener = null
  }

  /**
   * Allows external callers (like a ViewModel) to reset thes zero position
   * on-demand, for example, when a user presses a "re-calibrate" button.
   */
  fun calibrate() {
    rotation = windowManager.defaultDisplay.rotation
    isZeroPositionSet = false
  }

  override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
  }

  override fun onSensorChanged(event: SensorEvent?) {
    if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
      val ax = event.values[0]
      val ay = event.values[1]
      val az = event.values[2]

      // Get the current screen rotation to remap the coordinate system.

      val remappedAx: Float
      val remappedAy: Float

      // Remap the accelerometer axes so that they are relative to the screen's
      // current orientation, not the device's physical orientation.
      when (rotation) {
        Surface.ROTATION_90 -> { // Landscape, tilted to the left
          remappedAx = -ay
          remappedAy = ax
        }

        Surface.ROTATION_180 -> { // Portrait, upside down
          remappedAx = -ax
          remappedAy = -ay
        }

        Surface.ROTATION_270 -> { // Landscape, tilted to the right
          remappedAx = ay
          remappedAy = -ax
        }

        else -> { // Portrait, default
          remappedAx = ax
          remappedAy = ay
        }
      }

      // Apply a low-pass filter to isolate the gravity component of the remapped data.
      val alpha = 0.8f
      gravity[0] = alpha * gravity[0] + (1 - alpha) * remappedAx
      gravity[1] = alpha * gravity[1] + (1 - alpha) * remappedAy
      gravity[2] = alpha * gravity[2] + (1 - alpha) * az // Z-axis is unaffected by this rotation

      // Calculate pitch and roll from the remapped gravity vector.
      val absolutePitch = Math.toDegrees(
        atan2(
          gravity[1], sqrt((gravity[0] * gravity[0] + gravity[2] * gravity[2]))
        )
      ).toFloat()
      val absoluteRoll = Math.toDegrees(atan2(-gravity[0], gravity[2])).toFloat()

      // On the first valid sensor event, capture the current orientation as the offset.
      if (!isZeroPositionSet) {
        pitchOffset = absolutePitch
        rollOffset = absoluteRoll
        isZeroPositionSet = true
      }

      // Calculate the final relative pitch and roll by subtracting the offset.
      val relativePitch = absolutePitch - pitchOffset
      val relativeRoll = absoluteRoll - rollOffset

      sensorUpdateListener?.onDataUpdate(
        adharsData {
          this.pitch = relativePitch
          this.roll = relativeRoll
        })
    }
  }

}