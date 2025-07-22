package dev.fanfly.apps.vellum.location

import android.location.Location

interface LocationUpdateListener {

  fun onLocationUpdate(location: Location?)

}