package com.example.healthwatchervitalsigns.vitals_checker_back_camera.view

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Camera
import android.hardware.Camera.PreviewCallback
import android.os.Bundle
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.healthwatchervitalsigns.R
import com.example.healthwatchervitalsigns.vitals_checker_back_camera.constants.Constants
import com.example.healthwatchervitalsigns.vitals_checker_back_camera.math.Fft
import com.example.healthwatchervitalsigns.vitals_checker_back_camera.math.Fft2
import com.example.healthwatchervitalsigns.vitals_checker_back_camera.process.ImageProcessing
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class VitalsProcessActivity : AppCompatActivity() {


    //Variables Initialization
    private val processing = AtomicBoolean(false)
    private var preview: SurfaceView? = null
    private var previewHolder: SurfaceHolder? = null
    private var camera: Camera? = null
    private var wakeLock: WakeLock? = null

    //Toast
    private var mainToast: Toast? = null

    //ProgressBar
    private var ProgHeart: ProgressBar? = null
    var ProgP = 0
    var inc = 0

    //Beats variable
    var Beats = 0
    var bufferAvgB = 0.0

    //Freq + timer variable
    private var startTime: Long = 0
    private var SamplingFreq = 0.0

    //SPO2 variable
    private val RedBlueRatio: Array<Double>? = null
    var o2 = 0
    var Stdr = 0.0
    var Stdb = 0.0
    var sumred = 0.0
    var sumblue = 0.0

    //RR variable
    var Breath = 0
    var bufferAvgBr = 0.0

    //BloodPressure variables
    var Gen = 0.0
    var Agg = 0.0
    var Hei = 0.0
    var Wei = 0.0
    var Q = 4.5
    private var SP: Int = 0
    private var DP: Int = 0

    //Arraylist
    var GreenAvgList = ArrayList<Double>()
    var RedAvgList = ArrayList<Double>()
    var BlueAvgList = ArrayList<Double>()
    var counter = 0

    private lateinit var pref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vitals_process)

        pref = getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)

        Hei = pref.getFloat(Constants.PREF_HEIGHT, 0F).toDouble()
        Wei = pref.getFloat(Constants.PREF_WEIGHT, 0F).toDouble()
        Agg = pref.getFloat(Constants.PREF_AGE, 0F).toDouble()
        Gen = pref.getFloat(Constants.PREF_GENDER, 0F).toDouble()

        if (Gen == 1.0) {
            Q = 5.0
        }

        // XML - Java Connecting
        preview = findViewById(R.id.preview)
        previewHolder = preview?.holder
        previewHolder?.addCallback(surfaceCallback)
        previewHolder?.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        ProgHeart = findViewById(R.id.VSPB)
        ProgHeart?.progress = 0

        // WakeLock Initialization : Forces the phone to stay On
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "DoNotDimScreen")
    }

    //Prevent the system from restarting your activity during certain configuration changes,
    // but receive a callback when the configurations do change, so that you can manually update your activity as necessary.
    //such as screen orientation, keyboard availability, and language

    //Prevent the system from restarting your activity during certain configuration changes,
    // but receive a callback when the configurations do change, so that you can manually update your activity as necessary.
    //such as screen orientation, keyboard availability, and language
//    fun onConfigurationChanged(newConfig: Configuration?) {
//        super.onConfigurationChanged(newConfig!!)
//    }

    //Wakelock + Open device camera + set orientation to 90 degree
    //store system time as a start time for the analyzing process
    //your activity to start interacting with the user.
    override fun onResume() {
        super.onResume()
        wakeLock?.acquire(10 * 60 * 1000L /*10 minutes*/)
        camera = Camera.open()
        camera?.setDisplayOrientation(90)
        startTime = System.currentTimeMillis()
    }

    //call back the frames then release the camera + wakelock and Initialize the camera to null
    //Called as part of the activity lifecycle when an activity is going into the background, but has not (yet) been killed. The counterpart to onResume().
    //When activity B is launched in front of activity A,
    //this callback will be invoked on A. B will not be created until A's onPause() returns, so be sure to not do anything lengthy here.
    override fun onPause() {
        super.onPause()
        wakeLock!!.release()
        camera!!.setPreviewCallback(null)
        camera!!.stopPreview()
        camera!!.release()
        camera = null
    }

    //getting frames data from the camera and start the measuring process
    private val previewCallback = PreviewCallback { data, cam ->
        /**
         * {@inheritDoc}
         */

        //if data or size == null ****
        if (data == null) throw NullPointerException()
        val size = cam.parameters.previewSize ?: throw NullPointerException()

        //Atomically sets the value to the given updated value if the current value == the expected value.
        if (!processing.compareAndSet(false, true)) return@PreviewCallback

        //put width + height of the camera inside the variables
        val width = size.width
        val height = size.height

        //RGB intensities initialization
        val GreenAvg: Double
        val RedAvg: Double
        val BlueAvg: Double
        GreenAvg = ImageProcessing.decodeYUV420SPtoRedBlueGreenAvg(
            data.clone(),
            height,
            width,
            3
        ) //Getting Green intensity after applying image processing on frame data, 3 stands for green
        RedAvg = ImageProcessing.decodeYUV420SPtoRedBlueGreenAvg(
            data.clone(),
            height,
            width,
            1
        ) //Getting Red intensity after applying image processing on frame data, 1 stands for red
        sumred += RedAvg //Summing Red intensity for the whole period of recording which is 30 second
        BlueAvg = ImageProcessing.decodeYUV420SPtoRedBlueGreenAvg(
            data.clone(),
            height,
            width,
            2
        ) //Getting Blue intensity after applying image processing on frame data, 2 stands for blue
        sumblue += BlueAvg //Summing Red intensity for the whole period of recording which is 30 second

        //Adding rgb intensity values to listofarrays
        GreenAvgList.add(GreenAvg)
        RedAvgList.add(RedAvg)
        BlueAvgList.add(BlueAvg)
        ++counter //counts the number of frames for the whole period of recording " 30 s "


        //To check if we got a good red intensity to process if not return to the condition and set it again until we get a good red intensity
        if (RedAvg < 200) {
            inc = 0
            ProgP = inc
            counter = 0
            ProgHeart!!.progress = ProgP
            processing.set(false)
        }
        val endTime = System.currentTimeMillis()
        val totalTimeInSecs =
            (endTime - startTime) / 1000.0 //convert time to seconds to be compared with 30 seconds
        if (totalTimeInSecs >= 30) {

            //convert listofarrays to arrays to be used in processing
            val Green = GreenAvgList.toTypedArray()
            val Red = RedAvgList.toTypedArray()
            val Blue = BlueAvgList.toTypedArray()
            SamplingFreq = counter / totalTimeInSecs //calculating sampling frequency

            //sending the rg arrays with the counter to make an fft process to get the heartbeats out of it
            val HRFreq: Double = Fft.FFT(Green, counter, SamplingFreq)
            val bpm: Double = Math.ceil(HRFreq * 60)
            val HR1Freq: Double = Fft.FFT(Red, counter, SamplingFreq)
            val bpm1: Double = Math.ceil(HR1Freq * 60)

            //sending the rg arrays with the counter to make an fft process then a bandpass filter to get the respiration rate out of it
            val RRFreq: Double = Fft2.FFT(Green, counter, SamplingFreq)
            val breath: Double = Math.ceil(RRFreq * 60)
            val RR1Freq: Double = Fft2.FFT(Red, counter, SamplingFreq)
            val breath1: Double = Math.ceil(RR1Freq * 60)

            //calculating the mean of red and blue intensities on the whole period of recording
            val meanr = sumred / counter
            val meanb = sumblue / counter


            //calculating the standard  deviation
            for (i in 0 until counter - 1) {
                val bufferb = Blue[i]
                Stdb = Stdb + (bufferb - meanb) * (bufferb - meanb)
                val bufferr = Red[i]
                Stdr = Stdr + (bufferr - meanr) * (bufferr - meanr)
            }

            //calculating the variance
            val varr = Math.sqrt(Stdr / (counter - 1))
            val varb = Math.sqrt(Stdb / (counter - 1))

            //calculating ratio between the two means and two variances
            val R = varr / meanr / (varb / meanb)

            //estimating SPo2
            val spo2 = 100 - 5 * R
            o2 = spo2.toInt()


            //comparing if heartbeat and Respiration rate are reasonable from the green and red intensities then take the average, otherwise value from green or red intensity if one of them is good and other is bad.
            if (bpm > 45 || bpm < 200 || breath > 10 || breath < 20) {
                if (bpm1 > 45 || bpm1 < 200 || breath1 > 10 || breath1 < 24) {
                    bufferAvgB = (bpm + bpm1) / 2
                    bufferAvgBr = (breath + breath1) / 2
                } else {
                    bufferAvgB = bpm
                    bufferAvgBr = breath
                }
            } else if (bpm1 > 45 || bpm1 < 200 || breath1 > 10 || breath1 < 20) {
                bufferAvgB = bpm1
                bufferAvgBr = breath1
            }

            //if the values of hr and o2 are not reasonable then show a toast that measurement failed and restart the progress bar and the whole recording process for another 30 seconds
            if (bufferAvgB < 45 || bufferAvgB > 200 || bufferAvgBr < 10 || bufferAvgBr > 24) {
                inc = 0
                ProgP = inc
                ProgHeart!!.progress = ProgP
                mainToast =
                    Toast.makeText(applicationContext, "Measurement Failed", Toast.LENGTH_SHORT)
                mainToast?.show()
                startTime = System.currentTimeMillis()
                counter = 0
                processing.set(false)
                return@PreviewCallback
            }
            Beats = bufferAvgB.toInt()
            Breath = bufferAvgBr.toInt()

            //estimations to estimate the blood pressure
            val ROB = 18.5
            val ET = 364.5 - 1.23 * Beats
            val BSA = 0.007184 * Math.pow(Wei, 0.425) * Math.pow(Hei, 0.725)
            val SV = -6.6 + 0.25 * (ET - 35) - 0.62 * Beats + 40.4 * BSA - 0.51 * Agg
            val PP = SV / (0.013 * Wei - 0.007 * Agg - 0.004 * Beats + 1.307)
            val MPP = Q * ROB
            SP = (MPP + 3 / 2 * PP).toInt()
            DP = (MPP - PP / 3).toInt()
        }

        val user = pref.getString(Constants.PREF_USER_NAME, "User") // user name

        //if all those variable contains a valid values then swap them to results activity and finish the processing activity
        if (Beats != 0 && SP != 0 && DP != 0 && o2 != 0 && Breath != 0) {
            val i = Intent(this, VitalSignsResults::class.java)
            i.putExtra("O2R", o2)
            i.putExtra("breath", Breath)
            i.putExtra("bpm", Beats)
            i.putExtra("SP", SP)
            i.putExtra("DP", DP)
            i.putExtra("Usr", user)
            startActivity(i)
            finish()
        }

        //keeps incrementing the progress bar and keeps the loop until we have a valid values for the previous if state
        if (RedAvg != 0.0) {
            ProgP = inc++ / 34
            ProgHeart!!.progress = ProgP
        }
        processing.set(false)
    }

    private val surfaceCallback: SurfaceHolder.Callback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            try {
                camera!!.setPreviewDisplay(previewHolder)
                camera!!.setPreviewCallback(previewCallback)
            } catch (t: Throwable) {
            }
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            val parameters = camera!!.parameters
            parameters.flashMode = Camera.Parameters.FLASH_MODE_TORCH
            val size = getSmallestPreviewSize(width, height, parameters)
            if (size != null) {
                parameters.setPreviewSize(size.width, size.height)
            }
            camera!!.parameters = parameters
            camera!!.startPreview()
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {}
    }

    private fun getSmallestPreviewSize(
        width: Int,
        height: Int,
        parameters: Camera.Parameters
    ): Camera.Size? {
        var result: Camera.Size? = null
        for (size in parameters.supportedPreviewSizes) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size
                } else {
                    val resultArea = result.width * result.height
                    val newArea = size.width * size.height
                    if (newArea < resultArea) result = size
                }
            }
        }
        return result
    }
}