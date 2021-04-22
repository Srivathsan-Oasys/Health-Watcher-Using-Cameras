package com.example.healthwatchervitalsigns.register

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.healthwatchervitalsigns.R
import com.example.healthwatchervitalsigns.main.MainActivity
import com.example.healthwatchervitalsigns.vitals_checker_back_camera.constants.Constants
import kotlinx.android.synthetic.main.activity_register.*

class RegisterActivity : AppCompatActivity() {

    private lateinit var context: Context
    private lateinit var registerViewModel: RegisterViewModel

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initViews()
        listeners()
    }

    private fun initViews() {
        context = this

        registerViewModel = ViewModelProvider(this)[RegisterViewModel::class.java]
    }

    private fun listeners() {
        btnSave.setOnClickListener {
            val gender = if (rbMale.isChecked) 1 else 2
            if (registerViewModel.validate(
                    etName.text.toString(),
                    etAge.text.toString(),
                    etHeight.text.toString(),
                    etWeight.text.toString(),
                    gender
                )
            ) {
                sharedPreferences =
                    getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
                with(sharedPreferences.edit()) {
                    putString(Constants.PREF_USER_NAME, etName.text.toString())
                    putFloat(Constants.PREF_HEIGHT, etHeight.text.toString().toFloat())
                    putFloat(Constants.PREF_WEIGHT, etWeight.text.toString().toFloat())
                    putFloat(Constants.PREF_GENDER, gender.toFloat())
                    putFloat(Constants.PREF_AGE, etAge.text.toString().toFloat())
                    apply()
                }

                val intent = Intent(context, MainActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(context, "Please enter all the values", Toast.LENGTH_SHORT).show()
            }
        }
    }
}