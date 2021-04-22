package com.example.healthwatchervitalsigns.main

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.example.healthwatchervitalsigns.R
import com.example.healthwatchervitalsigns.vitals_checker_back_camera.constants.Constants
import com.example.healthwatchervitalsigns.vitals_checker_back_camera.view.VitalsProcessActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var context: Context
    private val REQ_CODE = 100
    private var permissionGranted = false
    
    private lateinit var pref:SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        context = this
        
        pref = getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)

        requestPermission(Manifest.permission.CAMERA)

        initView()
        listeners()
    }
    
    private fun initView() {

    }

    private fun listeners() {
        btnVitalsBackCamera.setOnClickListener {
            if (permissionGranted) {
                val intent = Intent(this, VitalsProcessActivity::class.java)
                startActivity(intent)
            } else {
                displayDialog(
                    message = "Camera is needed for analysing the health",
                    positiveButtonText = "OK",
                    positiveButtonClickListener = { dialog, which ->
                        requestPermission(Manifest.permission.CAMERA)
                    }
                )
            }
        }
    }

    private fun requestPermission(permission: String) {
        ActivityCompat.requestPermissions(
            context as Activity,
            Array(1) { permission },
            REQ_CODE
        )
        if (ActivityCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionGranted = true
        } else if (ActivityCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(
                context as Activity,
                Array(1) { permission },
                REQ_CODE
            )
            permissionGranted = false
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
                    permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    private fun displayDialog(
        message: String,
        positiveButtonText: String? = null,
        positiveButtonClickListener: ((dialog: DialogInterface, which: Int) -> Unit)? = null,
        negatiButtonText: String? = null,
        negativeButtonClickListener: ((dialog: DialogInterface, which: Int) -> Unit)? = null
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setMessage(message)
        positiveButtonText?.let {
            builder.setPositiveButton(positiveButtonText) { dialog, which ->
                if (positiveButtonClickListener != null) {
                    positiveButtonClickListener(dialog, which)
                }
            }
        }
        negatiButtonText?.let {
            builder.setNegativeButton(negatiButtonText) { dialog, which ->
                if (negativeButtonClickListener != null) {
                    negativeButtonClickListener(dialog, which)
                }
            }
        }

        builder.show()
    }
}