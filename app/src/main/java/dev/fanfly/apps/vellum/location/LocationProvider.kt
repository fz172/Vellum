package dev.fanfly.apps.vellum.location

import android.Manifest
import android.content.Context
import android.location.Location
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import java.util.concurrent.CopyOnWriteArraySet
import javax.inject.Inject

@ViewModelScoped
class LocationProvider @Inject internal constructor(@ApplicationContext private val context: Context) {

  private val locationUpdateListeners: CopyOnWriteArraySet<LocationUpdateListener> =
    CopyOnWriteArraySet()

  private var lastSeenLocation: Location? = null

  private val fusedLocationClient =
    LocationServices.getFusedLocationProviderClient(context)

  private val locationCallback: LocationCallback = object :
    LocationCallback() {
    override fun onLocationResult(locationResult: LocationResult) {
      lastSeenLocation = locationResult.lastLocation
      locationUpdateListeners.forEach { it.onLocationUpdate(lastSeenLocation) }
    }
  }

  fun addLocationListener(listener: LocationUpdateListener) {
    this.locationUpdateListeners += listener
  }

  fun removeLocationListener(listener: LocationUpdateListener) {
    this.locationUpdateListeners -= listener
  }

  // Call this after checking for permissions
  @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
  fun startLocationUpdates() {
    val locationRequest =
      LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 100)
        .build()
    val unused = fusedLocationClient.requestLocationUpdates(
      locationRequest,
      locationCallback,
      context.mainLooper,
    )
  }

  fun stopLocationUpdates() {
    fusedLocationClient.removeLocationUpdates(locationCallback)
  }
}