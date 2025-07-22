package dev.fanfly.apps.vellum.adhars

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.fanfly.apps.vellum.location.LocationProvider
import dev.fanfly.apps.vellum.proto.AdharsData
import dev.fanfly.apps.vellum.proto.adharsData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class AdharsViewModel @Inject constructor(
  @ApplicationContext private val context: Context,
  private val sensorHub: AdharsSensorHub,
  private val locationProvider: LocationProvider
) : ViewModel(), SensorUpdateListener {

  private val _data = MutableStateFlow(adharsData { })
  val adharsData: StateFlow<AdharsData> = _data.asStateFlow()

  init {
    locationProvider.addLocationListener(sensorHub)
    if (ActivityCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
      ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
      ) == PackageManager.PERMISSION_GRANTED
    ) {
      locationProvider.startLocationUpdates()
    }

    sensorHub.startListening(this)
  }

  override fun onCleared() {
    locationProvider.stopLocationUpdates()
    sensorHub.stopListening()
  }

  override fun onDataUpdate(data: AdharsData) {
    viewModelScope.launch {
      _data.update { it ->
        data
      }
    }
  }

  fun calibrate() {
    sensorHub.calibrate()
  }

}