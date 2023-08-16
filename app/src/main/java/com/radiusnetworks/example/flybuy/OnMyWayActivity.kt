package com.radiusnetworks.example.flybuy

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.radiusnetworks.flybuy.sdk.FlyBuyCore
import com.radiusnetworks.flybuy.sdk.data.customer.CustomerInfo
import com.radiusnetworks.flybuy.sdk.pickup.PickupManager
import com.radiusnetworks.flybuy.sdk.util.fineLocationPermissions
import com.radiusnetworks.flybuy.sdk.util.hasFineLocationPermission
import com.radiusnetworks.flybuy.sdk.util.hasPostNotificationsPermissions
import com.radiusnetworks.flybuy.sdk.util.postNotificationsPermission
import kotlinx.android.synthetic.main.activity_on_my_way.*
import org.threeten.bp.ZoneId

class OnMyWayActivity : AppCompatActivity() {
    private var app: ExampleApplication? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_on_my_way)
        app = application as ExampleApplication
        displayFlyBuyCustomer()
        displayFlyBuyOrder()
    }

    private fun updateFlyBuyCustomer() {
        FlyBuyCore.customer.update(readFlyBuyCustomer()) { _, sdkError ->
            sdkError?.let {
                app?.handleFlyBuyError(it)
            }
        }
    }

    private fun readFlyBuyCustomer(): CustomerInfo {
        return CustomerInfo(
            name = customer_name.text.toString(),
            carType = customer_vehicle_type.text.toString(),
            carColor = customer_vehicle_color.text.toString(),
            licensePlate = customer_license_plate.text.toString()
        )
    }

    private fun displayFlyBuyCustomer() {
        FlyBuyCore.customer.current?.let {
            runOnUiThread {
                customer_name.setText(it.name)
                customer_phone.setText(it.phone)
                customer_vehicle_type.setText(it.carType)
                customer_vehicle_color.setText(it.carColor)
                customer_license_plate.setText(it.licensePlate)
            }
        }
    }

    private fun displayFlyBuyOrder() {
        val pickupTime = app?.activeOrder?.pickupWindow?.start?.let {
            val localDateTime = org.threeten.bp.LocalDateTime.ofInstant(it, ZoneId.systemDefault())
            val timeFormatter = org.threeten.bp.format.DateTimeFormatter.ofPattern(getString(R.string.eta_format))
            localDateTime.format(timeFormatter)
        } ?: run {
            getString(R.string.asap)
        }

        runOnUiThread {
            site_name.text = app?.activeOrder?.site?.name
            site_address.text = app?.activeOrder?.site?.fullAddress
            pickup_time.text = pickupTime
            val options: RequestOptions = RequestOptions()
                .centerInside()
            Glide.with(this).load(app?.activeOrder?.site?.iconUrl).apply(options)
                .into(journey_logo_image)
        }
    }

    private fun startGuestJourney() {
        val intent = Intent(this, GuestJourneyActivity::class.java)
        intent.putExtra("CustomerName", customer_name.text.toString())
        intent.putExtra("CustomerPhone", customer_phone.text.toString())
        intent.putExtra("CustomerCarType", customer_vehicle_type.text.toString())
        intent.putExtra("CustomerCarColor", customer_vehicle_color.text.toString())
        intent.putExtra("CustomerLicensePlate", customer_license_plate.text.toString())
        startActivity(intent)
    }

    fun onMyWayClick(v: View) {
        updateFlyBuyCustomer()
        // if app doesn't have Notifications permissions, ask for them
        requestPostNotificationsPermissions(::notificationPermissionCallback)
    }

    private fun notificationPermissionCallback(enabled: Boolean) {
        // if app doesn't have Location permissions, ask for them
        requestLocationPermissions(::locationPermissionCallback)
    }

    private fun locationPermissionCallback(enabled: Boolean) {
        startGuestJourney()
    }

    private var postNotificationsPermissionChangeCallback: ((Boolean) -> Unit)? = null
    private var locationPermissionChangeCallback: ((Boolean) -> Unit)? = null

    private val requestPostNotificationsPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            PickupManager.getInstance().onPermissionChanged()
            postNotificationsPermissionChangeCallback?.invoke(results.all { it.value })
        }

    private val requestLocationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            PickupManager.getInstance().onPermissionChanged()
            locationPermissionChangeCallback?.invoke(results.all { it.value })
        }

    // Call this whenever the app needs to check and ask for Notifications permissions at runtime.
    private fun requestPostNotificationsPermissions(permissionChangeCallback: (Boolean) -> Unit) {
        postNotificationsPermissionChangeCallback = permissionChangeCallback
        if (hasPostNotificationsPermissions()) {
            permissionChangeCallback(true)
        } else {
            requestPostNotificationsPermissionLauncher.launch(postNotificationsPermission().toTypedArray())
        }
    }

    private fun requestLocationPermissions(permissionChangeCallback: (Boolean) -> Unit) {
        locationPermissionChangeCallback = permissionChangeCallback
        if (hasFineLocationPermission()) {
            permissionChangeCallback(true)
        } else {
            requestLocationPermissionLauncher.launch(fineLocationPermissions().toTypedArray())
        }
    }
}
