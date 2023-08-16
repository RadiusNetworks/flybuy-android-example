package com.radiusnetworks.example.flybuy

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.radiusnetworks.flybuy.sdk.data.customer.CustomerState
import com.radiusnetworks.flybuy.sdk.data.order.OrderState
import com.radiusnetworks.flybuy.sdk.data.room.domain.Order
import com.radiusnetworks.flybuy.sdk.FlyBuyCore
import com.radiusnetworks.flybuy.sdk.data.room.domain.open
import com.radiusnetworks.flybuy.sdk.manager.builder.OrderOptions
import com.radiusnetworks.flybuy.sdk.util.hasFineLocationPermission
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
            FlyBuyCore.orders.getOrder(it.id)
        } ?: run {
            MutableLiveData<Order>()
        }

        order.observe(this, orderObserver)
        app?.activeOrder?.let {
            orderProgress(it)
        }
    }

    private fun orderProgress(order: Order) {
        when {
            order.state == OrderState.CANCELLED -> {
                startActivity(Intent(this, MainActivity::class.java))
            }
            !(order.open()) -> {
                showOrderCompleted()
            }
            order.customerId == null -> {
                progressBar.progress = 25
                eta.text = formatETA(order.etaAt)
                claimOrder(order)
            }
            order.customerState == CustomerState.ARRIVED -> {
                progressBar.progress = 67
                textView.text = getString(R.string.youre_here)
                eta.text = order.site.instructions
            }
            order.customerState == CustomerState.WAITING -> {
                progressBar.progress = 75
                // Once a customer is waiting, hide the I'm here button and show I'm done
                textView.text = getString(R.string.youre_here)
                eta.text = order.site.instructions
                imDoneButton.visibility = View.VISIBLE
                imHereButton.visibility = View.INVISIBLE
            }
            order.customerState == CustomerState.NEARBY -> {
                progressBar.progress = 50
                eta.text = formatETA(order.etaAt)
            }
            else -> {
                // customer en route
                progressBar.progress = 33
                eta.text = formatETA(order.etaAt)
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
            if (hasFineLocationPermission()) {
                getString(R.string.calculating)
            } else {
                getString(R.string.no_location)
            }
        }
    }

    private fun claimOrder(order: Order) {
        FlyBuyCore.customer.current?.let { customer ->
            val orderOptions = intent?.let {
                OrderOptions.Builder(customerName = it.getStringExtra("CustomerName") ?: "")
                    .setCustomerPhone(it.getStringExtra("CustomerPhone") ?: "")
                    .setCustomerCarColor(it.getStringExtra("CustomerCarColor") ?: "")
                    .setCustomerCarType(it.getStringExtra("CustomerCarType") ?: "")
                    .setCustomerCarPlate(it.getStringExtra("CustomerLicensePlate") ?: "")
                    .build()
            } ?: run {
                OrderOptions.Builder(customerName = customer.name)
                    .setCustomerPhone(customer.phone)
                    .setCustomerCarColor(customer.carColor.toString())
                    .setCustomerCarType(customer.carType.toString())
                    .setCustomerCarPlate(customer.licensePlate.toString())
                    .build()
            }

            FlyBuyCore.orders.claim(order.redemptionCode.toString(), orderOptions) { _, sdkError ->
                sdkError?.let {
                    app?.handleFlyBuyError(it)
                } ?: run {
                    // if location permissions denied, send an EN_ROUTE update
                    if (!hasFineLocationPermission()) {
                        FlyBuyCore.orders.updateCustomerState(order.id, CustomerState.EN_ROUTE) { _, sdkError ->
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
        startActivity(Intent(this, OrderCompleted::class.java, ))
    }

    fun imHereClick(v: View) {
        app?.activeOrder?.let { it ->
            FlyBuyCore.orders.updateCustomerState(it.id, CustomerState.WAITING) { order, sdkError ->
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
            FlyBuyCore.orders.updateCustomerState(it.id, CustomerState.COMPLETED) { order, sdkError ->
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
