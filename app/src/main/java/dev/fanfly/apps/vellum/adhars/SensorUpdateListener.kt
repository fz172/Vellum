package dev.fanfly.apps.vellum.adhars

import dev.fanfly.apps.vellum.proto.AdharsData

interface SensorUpdateListener {
  fun onDataUpdate(data: AdharsData)
}
