package com.radiusnetworks.example.flybuy

import android.app.Application
import android.util.Log
import com.radiusnetworks.flybuy.sdk.ConfigOptions
import com.radiusnetworks.flybuy.sdk.FlyBuyCore
import com.radiusnetworks.flybuy.sdk.pickup.PickupManager
import com.radiusnetworks.flybuy.sdk.data.common.SdkError
import com.radiusnetworks.flybuy.sdk.data.room.domain.Order
import com.radiusnetworks.flybuy.sdk.jobs.ResponseEventType


class ExampleApplication : Application() {
    var activeOrder: Order? = null

    override fun onCreate() {
        super.onCreate()
        val configOptions: ConfigOptions = ConfigOptions.Builder("TOKEN_HERE")
            .build()
        FlyBuyCore.configure(this, configOptions)
        PickupManager.getInstance().configure(this)
    }

    fun handleFlyBuyError(sdkError: SdkError?) {
        when (sdkError?.type) {
            ResponseEventType.NO_CONNECTION -> {
                Log.e("FlyBuy SDK Error", "No Connection")
            }
            ResponseEventType.FAILED -> {
                when (sdkError.code) {
                    425 -> {
                        Log.e("FlyBuy SDK Error", "Upgrade your app!")
                    }
                    else -> {
                        Log.e("FlyBuy SDK Error", sdkError.userError())
                    }
                }
            }
            else -> {
                Log.e("FlyBuy SDK Error", "Unknown")
            }
        }
    }
}