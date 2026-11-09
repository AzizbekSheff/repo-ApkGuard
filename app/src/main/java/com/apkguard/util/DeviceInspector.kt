package com.apkguard.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import com.apkguard.model.DeviceInfo
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.*

class DeviceInspector(private val context: Context) {

    fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            ipAddress = getIpAddress(),
            location = getLastLocationName(),
            latitude = lastLocation?.latitude,
            longitude = lastLocation?.longitude
        )
    }

    private fun getIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "N/A"
    }

    private var lastLocation: Location? = null

    @SuppressLint("MissingPermission")
    private fun getLastLocationName(): String? {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = locationManager.getProviders(true)
            var bestLocation: Location? = null
            for (provider in providers) {
                val l = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                    bestLocation = l
                }
            }
            
            lastLocation = bestLocation
            
            if (bestLocation != null) {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(bestLocation.latitude, bestLocation.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    return "${address.locality ?: ""}, ${address.countryName ?: ""}".trim(',',' ')
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "Aniqlanmadi"
    }
}
