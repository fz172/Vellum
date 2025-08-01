package dev.fanfly.apps.vellum.adhars

import android.content.Context
import android.hardware.Sensor
import android.hardware.Sensor.TYPE_ACCELEROMETER
import android.hardware.Sensor.TYPE_GYROSCOPE
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.view.Surface
import android.view.WindowManager
import com.google.common.flogger.FluentLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.fanfly.apps.vellum.location.LocationUpdateListener
import dev.fanfly.apps.vellum.proto.AdharsData
import dev.fanfly.apps.vellum.proto.adharsData
import java.lang.System.arraycopy
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Singleton
class AdharsSensorHub
@Inject internal constructor(
  @param:ApplicationContext private val context: Context,
) : SensorEventListener, LocationUpdateListener {

  private val sensorManager: SensorManager =
    context.getSystemService(SensorManager::class.java)
  private val windowManager =
    context.getSystemService(WindowManager::class.java)

  private val accelerometer: Sensor? =
    sensorManager.getDefaultSensor(TYPE_ACCELEROMETER)
  private val gyroscope: Sensor? =
    sensorManager.getDefaultSensor(TYPE_GYROSCOPE)

  // --- State Variables ---
  private val accelData = FloatArray(3)
  private val gyroData = FloatArray(3)
  private var fusedPitch: Float = 0f
  private var fusedRoll: Float = 0f
  private var sensorUpdateListener: SensorUpdateListener? = null
  private var pitchOffset: Float = 0f
  private var rollOffset: Float = 0f
  private var isZeroPositionSet: Boolean = false
  private var lastKnownPitchRoll: AdharsData? = null
  private var lastTimestamp: Long = 0

  // --- GPS and Acceleration State ---
  private val estimatedLinearAccel = FloatArray(3)
  private val currentVelocity = FloatArray(3)
  private var lastLocationTimestamp: Long = 0

  // --- Vertical Acceleration State (High-Pass Filter) ---
  private var previousAccelZ: Float = 0f


  fun startListening(listener: SensorUpdateListener) {
    sensorUpdateListener = listener
    calibrate()
    sensorManager.registerListener(
      this, accelerometer, SensorManager.SENSOR_DELAY_UI
    )
    sensorManager.registerListener(
      this, gyroscope, SensorManager.SENSOR_DELAY_UI
    )
  }

  fun stopListening() {
    sensorManager.unregisterListener(this)
    sensorUpdateListener = null
  }

  /** Allows external callers (like a ViewModel) to reset the zero position on-demand. */
  fun calibrate() {
// Reset all state variables
    accelData.fill(0f)
    gyroData.fill(0f)
    estimatedLinearAccel.fill(0f)
    currentVelocity.fill(0f)
    lastLocationTimestamp = 0L
    previousAccelZ = 0f
    lastKnownPitchRoll = null
    isZeroPositionSet = false
    fusedPitch = 0f
    fusedRoll = 0f
    pitchOffset = 0f
    rollOffset = 0f
    lastTimestamp = 0L
  }

  /**
   * Receives location updates from the system to calculate horizontal linear acceleration.
   */
  override fun onLocationUpdate(location: Location?) {
    if (location == null) {
      logger.atWarning().log("No location received.")
      return
    }
    if (lastLocationTimestamp == 0L) {
      lastLocationTimestamp = location.time
      logger.atWarning()
        .log("Updating last location timestamp but not updating location.")
      return
    }

    val dt = (location.time - lastLocationTimestamp) / 1000.0f
    if (dt <= 0) {
      logger.atWarning().log("Skipping as dt is too small.")
      return
    }

    val speed = location.speed
    val bearingRad = Math.toRadians(location.bearing.toDouble()).toFloat()
    val vx = speed * sin(bearingRad)
    val vy = speed * cos(bearingRad)


    val ax = (vx - currentVelocity[0]) / dt
    val ay = (vy - currentVelocity[1]) / dt

    // Update only the horizontal components from GPS
    estimatedLinearAccel[0] = ax
    estimatedLinearAccel[1] = ay

    currentVelocity[0] = vx
    currentVelocity[1] = vy
    lastLocationTimestamp = location.time
    logger.atWarning()
      .log("Updating last location timestamp and velocity on x,y axis.")
  }

  override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

  override fun onSensorChanged(event: SensorEvent) {
    // Store raw sensor data based on the event type
    when (event.sensor.type) {
      TYPE_ACCELEROMETER -> arraycopy(event.values, 0, accelData, 0, 3)
      TYPE_GYROSCOPE -> arraycopy(event.values, 0, gyroData, 0, 3)
      else -> return
    }

    val dt = if (lastTimestamp == 0L) {
      lastTimestamp = event.timestamp
      // Initialize previousAccelZ for the filter on the first run
      previousAccelZ = accelData[2]
      return
    } else {
      val temp = (event.timestamp - lastTimestamp) * NS2S
      lastTimestamp = event.timestamp
      temp
    }

    // --- Correction for Total Linear Acceleration ---
    val gravityX = accelData[0] - estimatedLinearAccel[0]
    val gravityY = accelData[1] - estimatedLinearAccel[1]
    val gravityZ =
      accelData[2] - estimatedLinearAccel[2] // Now uses the filtered value

    // --- Remapping Corrected Gravity Vector and Gyro ---
    val remappedAx: Float
    val remappedAy: Float
    val remappedGx: Float
    val remappedGy: Float

    // Get live screen rotation on every event to handle orientation changes.
    val rotation = windowManager.defaultDisplay.rotation
    when (rotation) {
      Surface.ROTATION_90 -> {
        remappedAx = gravityY
        remappedAy = -gravityX
        remappedGx = gyroData[1]
        remappedGy = gyroData[0]
      }

      Surface.ROTATION_270 -> {
        remappedAx = -gravityY
        remappedAy = gravityX
        remappedGx = -gyroData[1]
        remappedGy = -gyroData[0]
      }

      Surface.ROTATION_180 -> {
        remappedAx = -gravityX
        remappedAy = -gravityY
        remappedGx = -gyroData[0]
        remappedGy = -gyroData[1]
      }

      else -> { // ROTATION_0
        remappedAx = -gravityX
        remappedAy = -gravityY
        remappedGx = -gyroData[0]
        remappedGy = -gyroData[1]
      }
    }

    // --- SENSOR FUSION ---
    val accelPitch = Math.toDegrees(
      atan2(
        -remappedAy.toDouble(),
        sqrt(remappedAx * remappedAx + gravityZ * gravityZ).toDouble()
      )
    ).toFloat()
    val accelRoll =
      Math.toDegrees(atan2(remappedAx.toDouble(), -remappedAy.toDouble()))
        .toFloat()

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
      val gyroPitchChange = Math.toDegrees(remappedGx * dt.toDouble()).toFloat()
      val gyroRollChange = Math.toDegrees(remappedGy * dt.toDouble()).toFloat()

      fusedPitch =
        COMPLEMENTARY_FILTER_ALPHA * (fusedPitch - gyroPitchChange) + (1 - COMPLEMENTARY_FILTER_ALPHA) * accelPitch

      if (abs(fusedPitch) < GIMBAL_LOCK_PITCH_THRESHOLD_DEGREES) {

        fusedRoll =
          COMPLEMENTARY_FILTER_ALPHA * (fusedRoll - gyroRollChange) + (1 - COMPLEMENTARY_FILTER_ALPHA) * accelRoll
      } else {
        fusedRoll -= gyroRollChange
      }
    }

    // FINAL CALCULATION:
    // Report the fused orientation relative to the calibration offset.
    val relativePitch = fusedPitch - pitchOffset
    // FIX: Remove the inversion. The fusion logic is now consistent.
    val relativeRoll = fusedRoll - rollOffset

    val newData = adharsData {
      this.pitch = relativePitch
      this.roll = relativeRoll
    }

    val oldData = lastKnownPitchRoll
    if (oldData == null || abs(oldData.roll - newData.roll) > MIN_ROLL_UPDATE_DEGREE || abs(
        oldData.pitch - newData.pitch
      ) > MIN_PITCH_UPDATE_DEGREE
    ) {
      sensorUpdateListener?.onDataUpdate(newData)
      lastKnownPitchRoll = newData
    }
  }

  companion object {
    private val logger = FluentLogger.forEnclosingClass()

    private const val COMPLEMENTARY_FILTER_ALPHA = 0.99f

    // The pitch angle at which to engage gimbal lock protection.
    private const val GIMBAL_LOCK_PITCH_THRESHOLD_DEGREES = 85f

    // Thresholds to prevent updating the UI for minuscule, imperceptible changes.
    private const val MIN_PITCH_UPDATE_DEGREE: Double = 0.25
    private const val MIN_ROLL_UPDATE_DEGREE: Double = 0.25

    // Converts nanoseconds to seconds.
    private const val NS2S = 1.0f / 1_000_000_000.0f
  }
}
