package dev.fanfly.apps.vellum.adhars

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.fanfly.apps.vellum.proto.adharsData
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.sqrt

@Singleton
class AdharsSensorHub @Inject internal constructor(@ApplicationContext context: Context) :
  SensorEventListener {

  private val sensorManager: SensorManager = context.getSystemService(SensorManager::class.java)
  private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

  private var sensorUpdateListener: SensorUpdateListener? = null

  var pitch: Float = 0f
  var roll: Float = 0f
  private val gravity = DoubleArray(3)

  fun startListening(listener: SensorUpdateListener) {
    sensorUpdateListener = listener
    sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
  }

  fun stopListening() {
    sensorManager.unregisterListener(this)
    sensorUpdateListener = null
  }

  override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
  }

  override fun onSensorChanged(event: SensorEvent?) {
    if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
      // Apply a low-pass filter to isolate the gravity component
      val alpha = 0.8f // Adjust alpha for more or less smoothing
      gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
      gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
      gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]

      // Calculate Pitch and Roll
      // Pitch: rotation around the X-axis (forward/backward tilt)
      pitch = Math.toDegrees(
        atan2(
          gravity[1],
          sqrt((gravity[0] * gravity[0] + gravity[2] * gravity[2]))
        )
      ).toFloat()

      // Roll: rotation around the Y-axis (left/right tilt)
      roll = Math.toDegrees(atan2(-gravity[0], gravity[2])).toFloat()
      sensorUpdateListener?.onDataUpdate(
        adharsData {
          this.pitch = this@AdharsSensorHub.pitch
          this.roll = this@AdharsSensorHub.roll
        }
      )
    }
  }

}