package com.radiusnetworks.example.flybuy

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.iid.FirebaseInstanceId
import com.radiusnetworks.flybuy.sdk.FlyBuy
import com.radiusnetworks.flybuy.sdk.data.customer.CustomerInfo
import kotlinx.android.synthetic.main.terms_and_conditions.*

class TermsAndConditions : AppCompatActivity() {
    private var app: ExampleApplication? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.terms_and_conditions)
        app = application as ExampleApplication
        val options: RequestOptions = RequestOptions()
            .centerInside()
        Glide.with(this).load(app?.activeOrder?.site?.iconUrl).apply(options)
            .into(logo_image_location)
    }

    private fun createFlyBuyCustomer() {
        val customerInfo = CustomerInfo(
            name = "Marty McFly",
            carType = "DeLorean",
            carColor = "Silver",
            licensePlate = "OUTATIME",
            phone = "555-555-5555"
        )
        FlyBuy.customer.create(
            customerInfo,
            termsOfService = true,
            ageVerification = true
        ) { customer, sdkError ->
            sdkError?.let {
                app?.handleFlyBuyError(it)
            } ?: run {
                customer?.let {
                    FirebaseInstanceId.getInstance().deleteInstanceId()
                }
            }
        }
    }

    fun okClick(v: View) {
        createFlyBuyCustomer()
        startActivity(Intent(this, OnMyWayActivity::class.java))
    }

    fun onCheck(v: View) {
        button_ok.isEnabled = age_check.isChecked and toc_check.isChecked
    }

}
