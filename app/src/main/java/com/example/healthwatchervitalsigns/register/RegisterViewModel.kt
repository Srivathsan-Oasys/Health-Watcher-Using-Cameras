package com.example.healthwatchervitalsigns.register

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RegisterViewModel:ViewModel() {

    val isValid = MutableLiveData<Boolean>()
}