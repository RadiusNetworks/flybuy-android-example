package com.radiusnetworks.example.flybuy

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.radiusnetworks.example.flybuy.databinding.TermsAndConditionsBinding
import com.radiusnetworks.flybuy.sdk.FlyBuyCore
import com.radiusnetworks.flybuy.sdk.data.customer.CustomerInfo

class TermsAndConditions : AppCompatActivity() {
    private var app: ExampleApplication? = null
    private lateinit var binding: TermsAndConditionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = TermsAndConditionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        app = application as ExampleApplication
        val options: RequestOptions = RequestOptions()
            .centerInside()
        Glide.with(this).load(app?.activeOrder?.site?.iconUrl).apply(options)
            .into(binding.logoImageLocation)
    }

    private fun createFlyBuyCustomer() {
        val customerInfo = CustomerInfo(
            name = "Marty McFly",
            carType = "DeLorean",
            carColor = "Silver",
            licensePlate = "OUTATIME",
            phone = "555-555-5555"
        )
        FlyBuyCore.customer.create(
            customerInfo,
            termsOfService = true,
            ageVerification = true
        ) { customer, sdkError ->
            sdkError?.let {
                app?.handleFlyBuyError(it)
            } ?: run {
                customer?.let {
                    FirebaseMessaging.getInstance().deleteToken()
                }
            }
        }
    }

    fun okClick(v: View) {
        createFlyBuyCustomer()
        startActivity(Intent(this, OnMyWayActivity::class.java))
    }

    fun onCheck(v: View) {
        binding.buttonOk.isEnabled = binding.ageCheck.isChecked and binding.tocCheck.isChecked
    }

}
