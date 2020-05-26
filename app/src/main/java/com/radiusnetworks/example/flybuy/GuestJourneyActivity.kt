package com.radiusnetworks.example.flybuy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.radiusnetworks.flybuy.sdk.FlyBuy
import com.radiusnetworks.flybuy.sdk.data.customer.CustomerInfo
import com.radiusnetworks.flybuy.sdk.data.customer.CustomerState
import com.radiusnetworks.flybuy.sdk.data.order.OrderState
import com.radiusnetworks.flybuy.sdk.data.room.domain.Order
import kotlinx.android.synthetic.main.activity_guest_journey.*
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.temporal.ChronoUnit

class GuestJourneyActivity : AppCompatActivity() {
    private var app: ExampleApplication? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guest_journey)
        app = application as ExampleApplication
        // Get merchant logo from FlyBuy
        val options: RequestOptions = RequestOptions()
            .centerInside()
        Glide.with(this).load(app?.activeOrder?.site?.iconUrl).apply(options)
            .into(journey_logo_image)
        val orderObserver = Observer<Order> {
            orderProgress(it)
        }
        val order: LiveData<Order> = app?.activeOrder?.let {
            FlyBuy.orders.getOrder(it.id)
        } ?: run {
            MutableLiveData<Order>()
        }

        order.observe(this, orderObserver)
        app?.activeOrder?.let {
            orderProgress(it)
        }
    }

    private fun orderProgress(order: Order) {
        // OrderStates of COMPLETED and CANCELLED take priority over CustomerState
        when (order.orderState) {
            OrderState.COMPLETED -> showOrderCompleted()
            OrderState.CANCELLED -> startActivity(Intent(this, MainActivity::class.java))
            OrderState.GONE -> startActivity(Intent(this, MainActivity::class.java))
            else -> {
                when (order.customerState) {
                    CustomerState.CREATED -> {
                        progressBar.progress = 25
                        eta.text = formatETA(order.etaAt)
                        claimOrder(order)
                    }
                    CustomerState.EN_ROUTE -> {
                        progressBar.progress = 33
                        eta.text = formatETA(order.etaAt)
                    }
                    CustomerState.NEARBY -> {
                        progressBar.progress = 50
                        eta.text = formatETA(order.etaAt)
                    }
                    CustomerState.ARRIVED -> {
                        progressBar.progress = 67
                        textView.text = getString(R.string.youre_here)
                        eta.text = order.site.instructions
                    }
                    CustomerState.WAITING -> {
                        progressBar.progress = 75
                        // Once a customer is waiting, hide the I'm here button and show I'm done
                        textView.text = getString(R.string.youre_here)
                        eta.text = order.site.instructions
                        imDoneButton.visibility = View.VISIBLE
                        imHereButton.visibility = View.INVISIBLE
                    }
                    CustomerState.COMPLETED -> {
                        progressBar.progress = 100
                        showOrderCompleted()
                    }
                }
            }
        }
    }

    private fun formatETA(etaAt: Instant?): String {
        return etaAt?.let {
            val now = Instant.now()
            val etaSeconds = ChronoUnit.SECONDS.between(now, it)
            val etaMinutes = etaSeconds / 60
            val localDateTime = org.threeten.bp.LocalDateTime.ofInstant(it, ZoneId.systemDefault())
            val etaTimeFormatter = org.threeten.bp.format.DateTimeFormatter.ofPattern(getString(R.string.eta_format))
            val etaText = localDateTime.format(etaTimeFormatter)
            when {
                etaSeconds < 0 -> getString(R.string.overdue, etaText)
                etaSeconds < 60 -> getString(R.string.less_than_a_minute, etaText)
                etaMinutes < 2 -> getString(R.string.one_minute, etaText)
                else -> {
                    getString(R.string.eta_minutes, etaMinutes, etaText)
                }
            }
        } ?: run {
            if (checkLocationPermissions()) {
                getString(R.string.calculating)
            } else {
                getString(R.string.no_location)
            }
        }
    }

    private fun checkLocationPermissions(): Boolean {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
    }

    private fun claimOrder(order: Order) {
        FlyBuy.customer.current?.let { customer ->
            val customerInfo = intent?.let {
                CustomerInfo(
                    name = it.getStringExtra("CustomerName"),
                    phone = it.getStringExtra("CustomerPhone"),
                    carType = it.getStringExtra("CustomerCarType"),
                    carColor = it.getStringExtra("CustomerCarColor"),
                    licensePlate = it.getStringExtra("CustomerLicensePlate")
                )
            } ?: run {
                CustomerInfo(
                    name = customer.name,
                    phone = customer.phone,
                    carType = customer.carType.toString(),
                    carColor = customer.carColor.toString(),
                    licensePlate = customer.licensePlate.toString()
                )
            }

            FlyBuy.orders.claim(order.redemptionCode.toString(), customerInfo) { _, sdkError ->
                sdkError?.let {
                    app?.handleFlyBuyError(it)
                } ?: run {
                    // if location permissions denied, send an EN_ROUTE update
                    if (!checkLocationPermissions()) {
                        FlyBuy.orders.event(order.id, CustomerState.EN_ROUTE) { _, sdkError ->
                            sdkError?.let {
                                app?.handleFlyBuyError(it)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showOrderCompleted() {
        startActivity(Intent(this, OrderCompleted::class.java))
    }

    fun imHereClick(v: View) {
        app?.activeOrder?.let {
            FlyBuy.orders.event(it.id, CustomerState.WAITING) { order, sdkError ->
                sdkError?.let {
                    app?.handleFlyBuyError(it)
                } ?: run {
                    app?.activeOrder = order
                    runOnUiThread {
                        imDoneButton.visibility = View.VISIBLE
                        v.visibility = View.INVISIBLE
                    }
                }
            }
        }
    }

    fun imDoneClick(v: View) {
        app?.activeOrder?.let {
            FlyBuy.orders.event(it.id, CustomerState.COMPLETED) { order, sdkError ->
                sdkError?.let {
                    app?.handleFlyBuyError(it)
                } ?: run {
                    app?.activeOrder = order
                    showOrderCompleted()
                }
            }
        }
    }

}
