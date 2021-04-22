package com.example.healthwatchervitalsigns.register

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import com.example.healthwatchervitalsigns.vitals_checker_back_camera.constants.Constants

class RegisterViewModel : ViewModel() {


    fun validate(name: String, age: String, ht: String, wt: String, gender: Int): Boolean {
        val validate =
            name.isNotEmpty() && age.isNotEmpty() && ht.isNotEmpty() && wt.isNotEmpty() && gender > 0
        if (validate) {

        }
        return validate
    }
}