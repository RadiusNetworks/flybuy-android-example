package com.radiusnetworks.example.flybuy

import android.util.Log
import com.radiusnetworks.flybuy.sdk.FlyBuyApplication
import com.radiusnetworks.flybuy.sdk.FlyBuyCore
import com.radiusnetworks.flybuy.sdk.pickup.PickupManager
import com.radiusnetworks.flybuy.sdk.data.common.SdkError
import com.radiusnetworks.flybuy.sdk.data.room.domain.Order
import com.radiusnetworks.flybuy.sdk.jobs.ResponseEventType


class ExampleApplication : FlyBuyApplication() {
    var activeOrder: Order? = null


    override fun onCreate() {
        super.onCreate()
        FlyBuyCore.configure(this, "FLYBUY_APP_TOKEN")
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
        }
    }
}