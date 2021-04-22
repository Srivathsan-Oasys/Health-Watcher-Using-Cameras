package com.example.healthwatchervitalsigns.vitals_checker_back_camera.view

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.healthwatchervitalsigns.R
import com.example.healthwatchervitalsigns.firebase_db.VitalsBackCamera
import com.google.firebase.database.FirebaseDatabase
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

        upload("$user", "$Date", "$VHR", "$VBP1 / $VBP2", "$VRR", "$VO2")
    }

    private fun upload(
        user: String,
        date: String,
        heartRate: String,
        bp: String,
        respirationRate: String,
        oxygenSaturation: String
    ) {
        val databaseReference = FirebaseDatabase.getInstance().getReference(user)
        val id = databaseReference.push().key ?: "null"
        val vitals = VitalsBackCamera(
            name = user,
            time = date,
            heartRate = heartRate,
            bp = bp,
            respirationRate = respirationRate,
            oxygenSaturation = oxygenSaturation
        )
        databaseReference.child("back cam - $id").setValue(vitals)
    }
}