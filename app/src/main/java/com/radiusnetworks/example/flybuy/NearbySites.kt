package com.radiusnetworks.example.flybuy

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.radiusnetworks.example.flybuy.location.FlybuyGoogleMap
import com.radiusnetworks.example.flybuy.location.LocationUIAdapter
import com.radiusnetworks.flybuy.sdk.data.places.Place
import com.radiusnetworks.flybuy.sdk.data.room.domain.Site

class NearbySites : ComponentActivity() {
    private lateinit var locationUIAdapter: LocationUIAdapter
    private var ignoreNextQueryTextUpdate: Boolean = false

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationUIAdapter = LocationUIAdapter(this)
        setContent { CenterAlignedTopAppBarExample() }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CenterAlignedTopAppBarExample() {
        val context = LocalContext.current
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

        Scaffold(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .imePadding(),
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        Text(
                            "SELECT A LOCATION",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                context.startActivity(Intent(this, MainActivity::class.java))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { innerPadding ->
            ScrollContent(innerPadding)
        }
    }

    @Composable
    fun ScrollContent(innerPadding: PaddingValues) {
        Column(
            modifier = Modifier
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight(0.4f)
                    .defaultMinSize(minHeight = 200.dp)
            ) {
                Box {
                    FlybuyGoogleMap(
                        modifier = Modifier
                            .fillMaxSize(),
                        locationUIAdapter = locationUIAdapter,
                        defaultZoom = 11F
                    )
                    Column {
                        AddressQuery(
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
            LazyColumn(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(locationUIAdapter.nearbySitesList()) { _, site: Site ->
                    NearbySiteCard(site)
                }
            }
        }
    }

    @Composable
    fun NearbySiteCard(site: Site) {
        val context = LocalContext.current

        Column(
            modifier = Modifier
                .padding(PaddingValues(12.dp))
        ) {
            Row {
                Column {
                    Row {
                        Text(
                            text = site.name!!,
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            modifier = Modifier.padding(PaddingValues(12.dp, 3.dp, 0.dp, 0.dp)),
                            text = "%.1f miles".format(locationUIAdapter.distanceFrom(site, "miles")),
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                background = Color.Red.copy(0.2f),
                                //shape = RoundedCornerShape(25.dp)
                            )
                        )
                    }
                    Text(text = site.fullAddress!!)
                    Text(
                        text = "Open until (who knows!)",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row {
                        site.pickupConfig.availablePickupTypes.forEach { pickupType ->
                            Column(
                                modifier = Modifier
                                    .size(80.dp)
                                    .padding(top = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val drawableId = when (pickupType.pickupType) {
                                    "curbside" -> R.drawable.pickup_type_curbside
                                    "delivery" -> R.drawable.pickup_type_delivery
                                    else -> R.drawable.pickup_type_pickup
                                }
                                Image(
                                    modifier = Modifier.size(30.dp),
                                    painter = painterResource(id = drawableId),
                                    contentDescription = pickupType.pickupTypeLocalizedString,
                                    alignment = Alignment.Center
                                )
                                Text(text = pickupType.pickupTypeLocalizedString)
                            }
                        }
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .fillMaxWidth(1F)
                        .align(Alignment.Bottom)
                ) {
                    Button(
                        shape = RoundedCornerShape(30),
                        content = { Text(text = "Order Now") },
                        onClick = {
                            Toast.makeText(
                                context,
                                "You placed an order at store ${site.partnerIdentifier}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                }
            }
            HorizontalDivider(color = Color.Black)
        }
    }

    @Composable
    fun AddressQuery(modifier: Modifier) {
        var resultsVisible by remember { locationUIAdapter.searchResultsVisible }
        var text by remember { locationUIAdapter.queryText }
        val focusManager = LocalFocusManager.current


        OutlinedTextField(
            modifier = modifier,
            value = text,
            shape = RoundedCornerShape(8.dp),
            label = { Text("Where are you?") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.9f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.7f)
            ),
            onValueChange = {
                text = it
                if (it.text == "") {
                    focusManager.clearFocus()
                } else {
                    if (ignoreNextQueryTextUpdate) {
                        ignoreNextQueryTextUpdate = false
                    } else {
                        locationUIAdapter.suggestPlaces(it.text)
                    }
                }
            }
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            if (resultsVisible) {
                itemsIndexed(locationUIAdapter.placeSuggestions()) { _, place: Place ->
                    SuggestedPlace(place)
                }
            }
        }
    }

    @Composable
    fun SuggestedPlace(place: Place) {
        val keyboardController = LocalSoftwareKeyboardController.current
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f))
                .padding(PaddingValues(10.dp))
                .fillMaxWidth()
                .clickable {
                    locationUIAdapter.selectPlace(place)
                    keyboardController?.hide()
                }
        ) {
            Row {
                Image(
                    modifier = Modifier
                        .size(30.dp)
                        .padding(PaddingValues(4.dp)),
                painter = painterResource(id = R.drawable.flybuy_pin),
                    contentDescription = "",
                    alignment = Alignment.Center
                )
                Column {
                    Text(
                        text = place.name,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    place.placeFormatted?.let {
                        Text(
                            text = it,
                            style = TextStyle(
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = Color.Black)
    }

    private fun Context.showToast(@StringRes resId: Int): Unit = Toast.makeText(this, resId, Toast.LENGTH_LONG).show()
}