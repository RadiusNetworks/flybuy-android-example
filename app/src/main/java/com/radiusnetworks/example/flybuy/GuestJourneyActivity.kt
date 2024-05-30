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
import com.radiusnetworks.example.flybuy.databinding.ActivityGuestJourneyBinding
import com.radiusnetworks.flybuy.sdk.data.customer.CustomerState
import com.radiusnetworks.flybuy.sdk.data.order.OrderState
import com.radiusnetworks.flybuy.sdk.data.room.domain.Order
import com.radiusnetworks.flybuy.sdk.FlyBuyCore
import com.radiusnetworks.flybuy.sdk.data.room.domain.open
import com.radiusnetworks.flybuy.sdk.manager.builder.OrderOptions
import com.radiusnetworks.flybuy.sdk.util.hasFineLocationPermission
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class GuestJourneyActivity : AppCompatActivity() {
    private var app: ExampleApplication? = null
    private lateinit var binding: ActivityGuestJourneyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuestJourneyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        app = application as ExampleApplication
        // Get merchant logo from FlyBuy
        val options: RequestOptions = RequestOptions()
            .centerInside()
        Glide.with(this).load(app?.activeOrder?.site?.iconUrl).apply(options)
            .into(binding.journeyLogoImage)
        val orderObserver = Observer<Order?> {
            orderProgress(it)
        }
        val order: LiveData<Order?> = app?.activeOrder?.let {
            FlyBuyCore.orders.getOrder(it.id)
        } ?: run {
            MutableLiveData<Order>()
        }

        order.observe(this, orderObserver)
        app?.activeOrder?.let {
            orderProgress(it)
        }
    }

    private fun orderProgress(order: Order?) {
        order?.let {
            when {
                order.state == OrderState.CANCELLED -> {
                    startActivity(Intent(this, MainActivity::class.java))
                }
                !(order.open()) -> {
                    showOrderCompleted()
                }
                order.customerId == null -> {
                    binding.progressBar.progress = 25
                    binding.eta.text = formatETA(order.etaAt)
                    claimOrder(order)
                }
                order.customerState == CustomerState.ARRIVED -> {
                    binding.progressBar.progress = 67
                    binding.textView.text = getString(R.string.youre_here)
                    binding.eta.text = order.site.instructions
                }
                order.customerState == CustomerState.WAITING -> {
                    binding.progressBar.progress = 75
                    // Once a customer is waiting, hide the I'm here button and show I'm done
                    binding.textView.text = getString(R.string.youre_here)
                    binding.eta.text = order.site.instructions
                    binding.imDoneButton.visibility = View.VISIBLE
                    binding.imHereButton.visibility = View.INVISIBLE
                }
                order.customerState == CustomerState.NEARBY -> {
                    binding.progressBar.progress = 50
                    binding.eta.text = formatETA(order.etaAt)
                }
                else -> {
                    // customer en route
                    binding.progressBar.progress = 33
                    binding.eta.text = formatETA(order.etaAt)
                }
            }
        }
    }

    private fun formatETA(etaAt: Instant?): String {
        return etaAt?.let {
            val now = Instant.now()
            val etaSeconds = ChronoUnit.SECONDS.between(now, it)
            val etaMinutes = etaSeconds / 60
            val localDateTime = LocalDateTime.ofInstant(it, ZoneId.systemDefault())
            val etaTimeFormatter = DateTimeFormatter.ofPattern(getString(R.string.eta_format))
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
                        binding.imDoneButton.visibility = View.VISIBLE
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
