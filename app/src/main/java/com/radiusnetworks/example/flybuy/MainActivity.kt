package com.radiusnetworks.example.flybuy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.iid.FirebaseInstanceId
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.FirebaseApp
import com.radiusnetworks.flybuy.sdk.FlyBuy
import com.radiusnetworks.flybuy.sdk.data.customer.CustomerState
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private var app: ExampleApplication? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as ExampleApplication
        setContentView(R.layout.activity_main)
        FirebaseApp.initializeApp(this)
        updatePushToken()
        // if opening a deep link
        intent?.data?.let {uri ->
            uri.getQueryParameter("r")?.let {
                fetchOrder(it)
            }
        } ?: run {
            FlyBuy.orders.fetch()
            var activity: Class<AppCompatActivity> = OnMyWayActivity::class.java as Class<AppCompatActivity>
            var openOrders = FlyBuy.orders.open
            // if this customer has open orders, choose the first one and return to the guest journey.
            if (openOrders.isNotEmpty()) {
                app?.activeOrder = openOrders[0]
                when (app?.activeOrder?.customerState) {
                    CustomerState.CREATED -> activity = OnMyWayActivity::class.java as Class<AppCompatActivity>
                    CustomerState.EN_ROUTE -> activity = GuestJourneyActivity::class.java as Class<AppCompatActivity>
                    CustomerState.NEARBY -> activity = GuestJourneyActivity::class.java as Class<AppCompatActivity>
                    CustomerState.ARRIVED -> activity = GuestJourneyActivity::class.java as Class<AppCompatActivity>
                    CustomerState.WAITING -> activity = GuestJourneyActivity::class.java as Class<AppCompatActivity>
                    CustomerState.COMPLETED -> activity = OrderCompleted::class.java as Class<AppCompatActivity>
                }
                startActivity(Intent(this, activity))
            } else {
                // if no link and no active orders, show the redeem order button
                redeem.visibility = View.VISIBLE
                redemption_code.visibility = View.VISIBLE
            }
        }
    }

    private fun updatePushToken() {
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.d("Listener", "getInstanceId failed")
                    return@OnCompleteListener
                }
                // Get new Instance ID token
                task.result?.token?.let {
                    FlyBuy.onNewPushToken(it)
                }
            })
    }


    private fun fetchOrder(code: String) {
        FlyBuy.orders.fetch(code) { order, sdkError ->
            sdkError?.let {
                app?.handleFlyBuyError(it)
            } ?: run {
                app?.activeOrder = order
                // does FlyBuy customer exist?
                FlyBuy.customer.current?.let {
                    startActivity(Intent(this, OnMyWayActivity::class.java))
                } ?: run {
                    // if FlyBuy customer does not exist, show Terms and Conditions before creating user
                    startActivity(Intent(this, TermsAndConditions::class.java))
                }
            }
        }
    }

    fun redeemOrderClick(v: View?) {
        fetchOrder(redemption_code.text.toString())
    }

}
