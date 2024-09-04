package com.radiusnetworks.example.flybuy.location

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.radiusnetworks.example.flybuy.R
import com.radiusnetworks.flybuy.sdk.data.room.domain.Site

@Composable
fun NearbySitesList(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical,
    nearbySites: MutableList<Site>,
    content: @Composable RowScope.() -> Unit
) {
    //val localSite = compositionLocalOf { Site }
    LazyColumn(
        modifier = modifier,
        verticalArrangement = verticalArrangement
    ) {
        itemsIndexed(nearbySites) { _, site: Site ->
            content
        }
    }
}

@Composable
fun FlybuyGoogleMap(
    modifier: Modifier,
    locationUIAdapter: LocationUIAdapter,
    defaultZoom: Float = 10F
) {
    val cameraPosition = CameraPosition.fromLatLngZoom(
        locationUIAdapter.searchRegion()?.let {
            LatLng(it.latitude, it.longitude)
        } ?: run {
            LatLng(0.0, 0.0)
        }, defaultZoom
    )

    locationUIAdapter.cameraPositionState(rememberCameraPositionState {
        position = cameraPosition
    })
    GoogleMap(
        modifier = modifier,
        cameraPositionState = locationUIAdapter.cameraPositionState()!!
    )
    {
        locationUIAdapter.nearbySitesList().forEach() {
            Marker(
                state = MarkerState(position = LatLng(it.latitude!!.toDouble(), it.longitude!!.toDouble())),
                title = it.name,
            )
        }
    }
}
