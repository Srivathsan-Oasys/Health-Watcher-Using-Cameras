package com.example.healthwatchervitalsigns.firebase_db

data class VitalsFrontCamera(
    val name: String,
    val time: String,
    val heartRate: String,
    val bp: String,
    val temp: String
)