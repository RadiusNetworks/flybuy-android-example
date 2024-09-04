package com.radiusnetworks.example.flybuy.location

import androidx.activity.ComponentActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.radiusnetworks.flybuy.sdk.FlyBuyCore
import com.radiusnetworks.flybuy.sdk.data.location.CircularRegion
import com.radiusnetworks.flybuy.sdk.data.places.PlaceType
import com.radiusnetworks.flybuy.sdk.data.places.Place
import com.radiusnetworks.flybuy.sdk.data.room.domain.Site
import com.radiusnetworks.flybuy.sdk.manager.builder.PlaceSuggestionOptions

class LocationUIAdapter(context: ComponentActivity) {
    private val nearbySitesList: MutableList<Site> = mutableStateListOf()
    private val searchRegion: MutableLiveData<CircularRegion> = MutableLiveData<CircularRegion>()
    private var cameraPositionState: CameraPositionState? = null
    private var placeSuggestions: MutableList<Place> = mutableStateListOf()
    public var searchResultsVisible = mutableStateOf(false)
    public val queryText: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue(""))
    private val placeSuggestionOptions = PlaceSuggestionOptions.Builder().apply {
        //setType(PlaceType.CITY)
        setType(PlaceType.ADDRESS)
    }.build()

    init {
        val regionObserver = Observer<CircularRegion> { region ->
            cameraPositionState?.let {
                it.move(
                    CameraUpdateFactory.newLatLng(LatLng(region.latitude, region.longitude))
                )
            }

            nearbySites(region) { sites ->
                nearbySitesList.clear()
                nearbySitesList.addAll(sites)
            }
        }

        searchRegion.postValue(CircularRegion(0.0, 0.0, 1F))

        searchRegion.observe(context, regionObserver)

        currentLocation(context, 10F, "miles")?.let { region ->
            searchRegion.postValue(region)
        }
    }

    fun suggestPlaces(query: String) {
        FlyBuyCore.places.suggest(query, placeSuggestionOptions) { places, _ ->
            if (null != places) {
                placeSuggestions.clear()
                placeSuggestions.addAll(places)
                searchResultsVisible.value = places.isNotEmpty()
            }
        }
    }

    fun selectPlace(place: Place) {
        FlyBuyCore.places.retrieve(place) { coordinate, sdkError ->
            if (null != sdkError) {
                // Handle error
            } else {
                searchRegion.postValue(CircularRegion(coordinate!!.latitude, coordinate!!.longitude, 10000F))
                queryText.value = TextFieldValue(place.name)
                searchResultsVisible.value = false
            }
        }
    }

    fun nearbySitesList(): MutableList<Site> {
        return nearbySitesList
    }

    fun cameraPositionState(): CameraPositionState? {
        return cameraPositionState
    }

    fun cameraPositionState(state: CameraPositionState) {
        cameraPositionState = state
    }

    fun placeSuggestions(): MutableList<Place> {
        return placeSuggestions
    }

    fun distanceFrom(site: Site, units: String): Float {
        return distanceFrom(searchRegion.value!!, site, units)
    }

    fun searchRegion(region: CircularRegion) {
        searchRegion.postValue(region)
    }

    fun searchRegion(): CircularRegion? {
        return searchRegion.value
    }
}