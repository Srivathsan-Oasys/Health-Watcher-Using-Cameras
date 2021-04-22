package com.example.healthwatchervitalsigns.main

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.healthwatchervitalsigns.R
import com.example.healthwatchervitalsigns.utils.Utils
import com.example.healthwatchervitalsigns.vitals_checker_back_camera.constants.Constants
import com.example.healthwatchervitalsigns.vitals_checker_back_camera.view.VitalsProcessActivity
import com.example.healthwatchervitalsigns.vitals_checker_front_camera.view.BloodPressureActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var context: Context
    private val REQ_CODE = 100
    private var permissionGrantedCamera = false

    private lateinit var pref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initView()
        listeners()
    }

    private fun initView() {
        context = this

        pref = getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)

        requestCameraPermission()
    }

    private fun listeners() {
        btnVitalsBackCamera.setOnClickListener {
            if (permissionGrantedCamera) {
                val intent = Intent(this, VitalsProcessActivity::class.java)
                startActivity(intent)
            } else {
                Utils.displayDialog(
                    context = context,
                    message = "Camera is needed for analysing the health",
                    positiveButtonText = "OK",
                    positiveButtonClickListener = { dialog, which ->
                        requestCameraPermission()
                    }
                )
            }
        }

        btnVitalsFrontCamera.setOnClickListener {
            val intent = Intent(this, BloodPressureActivity::class.java)
            startActivity(intent)
        }
    }

    private fun requestCameraPermission() {
        val permission = Manifest.permission.CAMERA
        ActivityCompat.requestPermissions(
            context as Activity,
            Array(1) { permission },
            REQ_CODE
        )
        if (ActivityCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionGrantedCamera = true
        } else if (ActivityCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(
                context as Activity,
                Array(1) { permission },
                REQ_CODE
            )
            permissionGrantedCamera = false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQ_CODE -> {
                if (grantResults.isNotEmpty())
                    permissionGrantedCamera = grantResults[0] == PackageManager.PERMISSION_GRANTED
            }
        }
    }
}