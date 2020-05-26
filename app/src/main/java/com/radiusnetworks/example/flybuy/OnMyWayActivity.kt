package com.radiusnetworks.example.flybuy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.radiusnetworks.flybuy.sdk.FlyBuy
import com.radiusnetworks.flybuy.sdk.data.customer.CustomerInfo
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
        FlyBuy.customer.update(readFlyBuyCustomer()) { _, sdkError ->
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
        FlyBuy.customer.current?.let {
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
        // if app doesn't have location permissions, ask for them
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
        } else {
            startGuestJourney()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        startGuestJourney()
    }
}
