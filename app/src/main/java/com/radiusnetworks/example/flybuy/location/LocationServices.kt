package com.radiusnetworks.example.flybuy.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.location.Location
import android.location.LocationManager
import com.radiusnetworks.flybuy.sdk.FlyBuyCore
import com.radiusnetworks.flybuy.sdk.data.common.SdkError
import com.radiusnetworks.flybuy.sdk.data.location.CircularRegion
import com.radiusnetworks.flybuy.sdk.data.room.domain.Site
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@SuppressLint("MissingPermission")
fun currentLocation(context: Context, radius: Float, units: String = "meters"): CircularRegion? {
    val lm = context.getSystemService(LOCATION_SERVICE) as LocationManager
    val providers: List<String> = lm.getProviders(true)
    var bestLocation: Location? = null

    providers.forEach() { provider ->
        lm.getLastKnownLocation(provider)?.let {
            if (bestLocation == null || it.accuracy < bestLocation!!.accuracy) {
                bestLocation = it;
            }
        }
    }
    bestLocation?.let {
        return CircularRegion(it.latitude, it.longitude, convertMetersFrom(radius, units))
    } ?: run {
        return null
    }
}

fun nearbySites(region: CircularRegion, callback: (List<Site>) -> Unit) {
    FlyBuyCore.sites.fetch(region, 1, 50, "live") { sites: List<Site>?, _: SdkError? ->
        sites?.let {
            callback(it)
        }
    }
}

fun distanceFrom(region: CircularRegion, site: Site, units: String = "meters"): Float {
    val earthRadius = 6371000F
    val dLat = Math.toRadians(region.latitude - site.latitude!!.toFloat())
    val dLng = Math.toRadians(region.longitude - site.longitude!!.toFloat())
    val a = sin(dLat / 2) * sin(dLat / 2) + cos(
        Math.toRadians(site.latitude!!.toDouble())
    ) * cos(Math.toRadians(region.latitude)) * sin(dLng / 2) * sin(
        dLng / 2
    )
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return convertMetersTo((earthRadius * c).toFloat(), units)
}

fun convertMetersTo(distance: Float, units: String = "meters"): Float {
    return when(units) {
        "miles" -> distance / 1609.34F
        "km" -> distance / 1000.0F
        else -> distance
    }
}

fun convertMetersFrom(distance: Float, units: String = "meters"): Float {
    return when(units) {
        "miles" -> distance * 1609.34F
        "km" -> distance * 1000.0F
        else -> distance
    }
}