package com.example.healthwatchervitalsigns.register

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.healthwatchervitalsigns.R
import com.example.healthwatchervitalsigns.main.MainActivity
import kotlinx.android.synthetic.main.activity_register.*

class RegisterActivity : AppCompatActivity() {

    private val TAG = RegisterActivity::class.java.simpleName

    private lateinit var context: Context
    private lateinit var registerViewModel: RegisterViewModel

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
            if(registerViewModel.isValid.value == true) {
                val intent = Intent(context, MainActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(context, "Please enter all the values", Toast.LENGTH_SHORT).show()
            }
        }
    }
}