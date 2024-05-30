package com.radiusnetworks.example.flybuy

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.radiusnetworks.example.flybuy.databinding.ActivityOnMyWayBinding
import com.radiusnetworks.flybuy.sdk.FlyBuyCore
import com.radiusnetworks.flybuy.sdk.data.customer.CustomerInfo
import com.radiusnetworks.flybuy.sdk.pickup.PickupManager
import com.radiusnetworks.flybuy.sdk.util.fineLocationPermissions
import com.radiusnetworks.flybuy.sdk.util.hasFineLocationPermission
import com.radiusnetworks.flybuy.sdk.util.hasPostNotificationsPermissions
import com.radiusnetworks.flybuy.sdk.util.postNotificationsPermission
import java.time.ZoneId
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class OnMyWayActivity : AppCompatActivity() {
    private var app: ExampleApplication? = null
    private lateinit var binding: ActivityOnMyWayBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnMyWayBinding.inflate(layoutInflater)
        setContentView(binding.root)
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
            name = binding.customerName.text.toString(),
            carType = binding.customerVehicleType.text.toString(),
            carColor = binding.customerVehicleColor.text.toString(),
            licensePlate = binding.customerLicensePlate.text.toString()
        )
    }

    private fun displayFlyBuyCustomer() {
        FlyBuyCore.customer.current?.let {
            runOnUiThread {
                binding.customerName.setText(it.name)
                binding.customerPhone.setText(it.phone)
                binding.customerVehicleType.setText(it.carType)
                binding.customerVehicleColor.setText(it.carColor)
                binding.customerLicensePlate.setText(it.licensePlate)
            }
        }
    }

    private fun displayFlyBuyOrder() {
        val pickupTime = app?.activeOrder?.pickupWindow?.start?.let {
            val localDateTime = LocalDateTime.ofInstant(it, ZoneId.systemDefault())
            val timeFormatter = DateTimeFormatter.ofPattern(getString(R.string.eta_format))
            localDateTime.format(timeFormatter)
        } ?: run {
            getString(R.string.asap)
        }

        runOnUiThread {
            binding.siteName.text = app?.activeOrder?.site?.name
            binding.siteAddress.text = app?.activeOrder?.site?.fullAddress
            binding.pickupTime.text = pickupTime
            val options: RequestOptions = RequestOptions()
                .centerInside()
            Glide.with(this).load(app?.activeOrder?.site?.iconUrl).apply(options)
                .into(binding.journeyLogoImage)
        }
    }

    private fun startGuestJourney() {
        val intent = Intent(this, GuestJourneyActivity::class.java)
        intent.putExtra("CustomerName", binding.customerName.text.toString())
        intent.putExtra("CustomerPhone", binding.customerPhone.text.toString())
        intent.putExtra("CustomerCarType", binding.customerVehicleType.text.toString())
        intent.putExtra("CustomerCarColor", binding.customerVehicleColor.text.toString())
        intent.putExtra("CustomerLicensePlate", binding.customerLicensePlate.text.toString())
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
