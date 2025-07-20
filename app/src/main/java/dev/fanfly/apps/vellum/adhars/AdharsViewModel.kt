package dev.fanfly.apps.vellum.adhars

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fanfly.apps.vellum.proto.AdharsData
import dev.fanfly.apps.vellum.proto.adharsData
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


@HiltViewModel
class AdharsViewModel @Inject constructor(private val sensorHub: AdharsSensorHub) :
  ViewModel(), SensorUpdateListener {

  private val _data = MutableStateFlow(adharsData { })
  val adharsData: StateFlow<AdharsData> = _data.asStateFlow()

  init {
    sensorHub.startListening(this)
  }

  override fun onCleared() {
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