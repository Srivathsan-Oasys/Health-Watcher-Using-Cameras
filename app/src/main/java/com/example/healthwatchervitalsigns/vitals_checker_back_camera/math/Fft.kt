package com.example.healthwatchervitalsigns.vitals_checker_back_camera.math

object Fft {
    fun FFT(`in`: Array<Double>, size: Int, samplingFrequency: Double): Double {
        var temp = 0.0
        var POMP = 0.0
        val frequency: Double
        val output = DoubleArray(2 * size)
        for (i in output.indices) output[i] = 0.0
        for (x in 0 until size) {
            output[x] = `in`[x]
        }
        val fft = DoubleFft1d(size)
        fft.realForward(output)
        for (x in 0 until 2 * size) {
            output[x] = Math.abs(output[x])
        }
        for (p in 35 until size) { // 12 was chosen because it is a minimum frequency that we think people can get to determine heart rate.
            if (temp < output[p]) {
                temp = output[p]
                POMP = p.toDouble()
            }
        }
        if (POMP < 35) {
            POMP = 0.0
        }
        frequency = POMP * samplingFrequency / (2 * size)
        return frequency
    }
}