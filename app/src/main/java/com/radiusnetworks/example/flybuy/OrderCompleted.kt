package com.radiusnetworks.example.flybuy

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.radiusnetworks.example.flybuy.databinding.ActivityOrderCompletedBinding
import com.radiusnetworks.flybuy.sdk.FlyBuyCore

class OrderCompleted : AppCompatActivity() {
    private var app: ExampleApplication? = null
    private lateinit var binding: ActivityOrderCompletedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderCompletedBinding.inflate(layoutInflater)
        app = application as ExampleApplication
        setContentView(binding.root)
    }

    fun onFeedbackClick(v: View) {
        app?.activeOrder?.let { it ->
            FlyBuyCore.orders.rateOrder(
                orderId = it.id,
                rating = binding.ratingBar.numStars,
                comments = binding.commentsText.text.toString()
            )
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}
