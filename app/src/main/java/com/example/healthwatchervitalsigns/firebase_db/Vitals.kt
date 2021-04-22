package com.example.healthwatchervitalsigns.firebase_db

data class Vitals(
    val name: String,
    val time: String,
    val heartRate: String,
    val bp: String,
    val respirationRate: String,
    val oxygenSaturation: String
)