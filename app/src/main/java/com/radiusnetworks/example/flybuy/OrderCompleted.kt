package com.radiusnetworks.example.flybuy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.RatingBar.OnRatingBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.radiusnetworks.flybuy.sdk.FlyBuy
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
            FlyBuy.orders.rateOrder(
                orderId = it.id,
                rating = ratingBar.numStars,
                comments = commentsText.toString()
            )
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}
