package com.radiusnetworks.example.flybuy

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.radiusnetworks.flybuy.sdk.FlyBuyCore
import kotlinx.android.synthetic.main.activity_order_completed.*


class OrderCompleted : AppCompatActivity() {
    private var app: ExampleApplication? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as ExampleApplication
        setContentView(R.layout.activity_order_completed)
    }

    fun onFeedbackClick(v: View) {
        app?.activeOrder?.let { it ->
            FlyBuyCore.orders.rateOrder(
                orderId = it.id,
                rating = ratingBar.numStars,
                comments = commentsText.text.toString()
            )
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}
