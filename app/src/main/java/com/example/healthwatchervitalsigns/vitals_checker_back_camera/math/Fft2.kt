package com.example.healthwatchervitalsigns.vitals_checker_back_camera.math

object Fft2 {
    fun FFT(`in`: Array<Double>, size: Int, samplingFrequency: Double): Double {
        var temp = 0.0
        var POMP = 0.0
        val frequency: Double
        val output = DoubleArray(2 * size)
        val butterworth = Butterworth()
        butterworth.bandPass(2, samplingFrequency, 0.2, 0.3)
        for (i in output.indices) output[i] = 0.0
        for (x in 0 until size) {
            output[x] = `in`[x]
        }
        val fft = DoubleFft1d(size)
        fft.realForward(output)
        for (x in 0 until 2 * size) {
            output[x] = butterworth.filter(output[x])
        }
        for (x in 0 until 2 * size) {
            output[x] = Math.abs(output[x])
        }
        for (p in 12 until size) {
            if (temp < output[p]) {
                temp = output[p]
                POMP = p.toDouble()
            }
        }
        frequency = POMP * samplingFrequency / (2 * size)
        return frequency
    }
}