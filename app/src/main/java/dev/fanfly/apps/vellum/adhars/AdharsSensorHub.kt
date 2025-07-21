package dev.fanfly.apps.vellum.adhars

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import com.google.common.flogger.FluentLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.fanfly.apps.vellum.proto.AdharsData
import dev.fanfly.apps.vellum.proto.adharsData
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

@Singleton
class AdharsSensorHub @Inject internal constructor(
  @ApplicationContext private val context: Context,
) : SensorEventListener {

  private val sensorManager: SensorManager = context.getSystemService(SensorManager::class.java)

  // Get the WindowManager to detect screen rotation.
  private val windowManager = context.getSystemService(WindowManager::class.java)

  private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
  private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

  // --- State Variables ---
  // Raw sensor data arrays
  private val accelData = FloatArray(3)
  private val gyroData = FloatArray(3)

  // Fused orientation values
  private var fusedPitch: Float = 0f
  private var fusedRoll: Float = 0f


  private var sensorUpdateListener: SensorUpdateListener? = null

  // Calibration offsets
  private var pitchOffset: Float = 0f
  private var rollOffset: Float = 0f
  private var isZeroPositionSet: Boolean = false

  private var lastKnownPitchRoll: AdharsData? = null

  // Timestamp for calculating delta time (dt) for the gyroscope
  private var lastTimestamp: Long = 0

  fun startListening(listener: SensorUpdateListener) {
    // Reset the zero position each time listening starts.
    isZeroPositionSet = false
    sensorUpdateListener = listener
    sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
    sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI)
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
    accelData.fill(0f)
    gyroData.fill(0f)
    lastKnownPitchRoll = null
    isZeroPositionSet = false
    fusedPitch = 0f
    fusedRoll = 0f
    pitchOffset = 0f
    rollOffset = 0f
    lastTimestamp = 0L
  }

  override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
  }

  override fun onSensorChanged(event: SensorEvent) {
    // Store raw sensor data based on the event type
    when (event.sensor.type) {
      Sensor.TYPE_ACCELEROMETER -> System.arraycopy(event.values, 0, accelData, 0, 3)
      Sensor.TYPE_GYROSCOPE -> System.arraycopy(event.values, 0, gyroData, 0, 3)
      else -> return
    }

    // Wait for the next event to get a valid delta time
    val dt = if (lastTimestamp == 0L) {
      lastTimestamp = event.timestamp
      return
    } else {
      val temp = (event.timestamp - lastTimestamp) * NS2S
      lastTimestamp = event.timestamp
      temp
    }

    // Remap accelerometer and gyroscope data so that their axes are relative
    // to the screen's current orientation, not the device's physical orientation.
    val remappedAx: Float
    val remappedAy: Float
    val remappedGx: Float
    val remappedGy: Float


    // Get live screen rotation on every event to handle orientation changes.
    val rotation = windowManager.defaultDisplay.rotation
    logger.atInfo().log("rotation: %d", rotation)

    when (rotation) {
      Surface.ROTATION_270 -> { // Landscape,tilted to the right
        remappedAx = accelData[1]
        remappedAy = -accelData[0]
        remappedGx = gyroData[1]
        remappedGy = -gyroData[0]
      }

      Surface.ROTATION_90 -> { // Landscape, tilted to the left
        remappedAx = -accelData[1]
        remappedAy = accelData[0]
        remappedGx = -gyroData[1]
        remappedGy = gyroData[0]
      }

      Surface.ROTATION_180 -> { // Portrait, upside down
        remappedAx = -accelData[0]
        remappedAy = -accelData[1]
        remappedGx = -gyroData[0]
        remappedGy = -gyroData[1]
      }

      else -> { // ROTATION_0, Portrait default
        remappedAx = accelData[0]
        remappedAy = accelData[1]
        remappedGx = gyroData[0]
        remappedGy = gyroData[1]
      }
    }
    val az = accelData[2] // Z-axis is not affected by screen rotation

    // --- SENSOR FUSION with Gimbal Lock protection ---
    // 1. Calculate pitch and roll from the reliable accelerometer
    val accelPitch = Math.toDegrees(
      atan2(-remappedAy.toDouble(), sqrt(remappedAx * remappedAx + az * az).toDouble())
    ).toFloat()
    val accelRoll = Math.toDegrees(atan2(-remappedAx.toDouble(), az.toDouble())).toFloat()

    if (!isZeroPositionSet) {
      // INITIALIZATION STEP:
      // On the first run after calibration, bootstrap the filter state AND
      // the calibration offsets directly from the stable accelerometer data.
      pitchOffset = accelPitch
      rollOffset = accelRoll
      fusedPitch = accelPitch
      fusedRoll = accelRoll
      isZeroPositionSet = true
    } else {
      // FUSION STEP:
      // For all subsequent events, fuse gyroscope data with the accelerometer data.
      val gyroPitchChange = Math.toDegrees(remappedGx * dt.toDouble()).toFloat()
      val gyroRollChange = Math.toDegrees(remappedGy * dt.toDouble()).toFloat()

      // The pitch change from the gyro also needs to be inverted to match the new convention.
      fusedPitch =
        COMPLEMENTARY_FILTER_ALPHA * (fusedPitch - gyroPitchChange) + (1 - COMPLEMENTARY_FILTER_ALPHA) * accelPitch

      if (abs(fusedPitch) < GIMBAL_LOCK_PITCH_THRESHOLD_DEGREES) {
        fusedRoll =
          COMPLEMENTARY_FILTER_ALPHA * (fusedRoll + gyroRollChange) + (1 - COMPLEMENTARY_FILTER_ALPHA) * accelRoll
      } else {
        fusedRoll += gyroRollChange // Avoid drift in gimbal lock
      }
    }

    // FINAL CALCULATION:
    // Report the fused orientation relative to the calibration offset.
    val relativePitch = fusedPitch - pitchOffset
    val relativeRoll = fusedRoll - rollOffset

    val newData = adharsData {
      this.pitch = relativePitch
      this.roll = relativeRoll
    }

    val oldData = lastKnownPitchRoll
    if (oldData == null ||
      abs(oldData.roll - newData.roll) > MIN_ROLL_UPDATE_DEGREE ||
      abs(oldData.pitch - newData.pitch) > MIN_PITCH_UPDATE_DEGREE
    ) {
      sensorUpdateListener?.onDataUpdate(newData)
      lastKnownPitchRoll = newData
    }
  }

  companion object {
    private val logger = FluentLogger.forEnclosingClass()

    private const val COMPLEMENTARY_FILTER_ALPHA = 0.98f

    // The pitch angle at which to engage gimbal lock protection.
    private const val GIMBAL_LOCK_PITCH_THRESHOLD_DEGREES = 85f

    // Thresholds to prevent updating the UI for minuscule, imperceptible changes.
    private const val MIN_PITCH_UPDATE_DEGREE: Double = 0.25
    private const val MIN_ROLL_UPDATE_DEGREE: Double = 0.25

    // Converts nanoseconds to seconds.
    private const val NS2S = 1.0f / 1_000_000_000.0f
  }

}