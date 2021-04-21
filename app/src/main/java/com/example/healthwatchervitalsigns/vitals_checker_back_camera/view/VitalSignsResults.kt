package com.example.healthwatchervitalsigns.vitals_checker_back_camera.view

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.healthwatchervitalsigns.R
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class VitalSignsResults : AppCompatActivity() {

    var df: DateFormat = SimpleDateFormat("MM/dd/yyyy HH:mm:ss")
    var today = Calendar.getInstance().time
    var VBP1 = 0
    var VBP2 = 0
    var VRR = 0
    var VHR = 0
    var VO2 = 0
    private var user: String? = null
    private var Date: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vital_signs_results)
        Date = df.format(today)
        val VSRR = findViewById<TextView>(R.id.RRV)
        val VSBPS = findViewById<TextView>(R.id.BP2V)
        val VSHR = findViewById<TextView>(R.id.HRV)
        val VSO2 = findViewById<TextView>(R.id.O2V)
        val All = findViewById<Button>(R.id.SendAll)
        val bundle = intent.extras
        if (bundle != null) {
            VRR = bundle.getInt("breath")
            VHR = bundle.getInt("bpm")
            VBP1 = bundle.getInt("SP")
            VBP2 = bundle.getInt("DP")
            VO2 = bundle.getInt("O2R")
            user = bundle.getString("Usr")
            VSRR.text = VRR.toString()
            VSHR.text = VHR.toString()
            VSBPS.text = "$VBP1 / $VBP2"
            VSO2.text = VO2.toString()
        }

        All.setOnClickListener { v: View? ->
            val i = Intent(Intent.ACTION_SEND)
            i.type = "message/rfc822"
            i.putExtra(Intent.EXTRA_EMAIL, arrayOf("recipient@example.com"))
            i.putExtra(Intent.EXTRA_SUBJECT, "Health Watcher")
            i.putExtra(
                Intent.EXTRA_TEXT,
                "$user's new measurement \n at $Date are :\nHeart Rate = $VHR\nBlood Pressure = $VBP1 / $VBP2\nRespiration Rate = $VRR\nOxygen Saturation = $VO2"
            )
            try {
                startActivity(Intent.createChooser(i, "Send mail..."))
            } catch (ex: ActivityNotFoundException) {
                Toast.makeText(this, "There are no email clients installed.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}