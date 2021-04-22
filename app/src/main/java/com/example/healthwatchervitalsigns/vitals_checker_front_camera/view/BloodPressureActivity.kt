package com.example.healthwatchervitalsigns.vitals_checker_front_camera

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.Camera
import android.hardware.Camera.*
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Environment
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.text.format.Time
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.frontcamera.util.FastIcaRgb
import com.example.frontcamera.util.Fft
import com.example.frontcamera.util.StorageUtils
import com.example.healthwatchervitalsigns.R
import java.io.*

class BloodPressureActivity : AppCompatActivity() {
    private val PREFS_NAME = "MyPrefsFile"
    private var settings: SharedPreferences? = null

    private var mRelativeLayoutRoot: RelativeLayout? = null
    private var mFrameLayoutCameraPreview: FrameLayout? = null
    private var mSurfaceViewCameraPreview: SurfaceView? = null
    private var mImageViewRectangle0: ImageView? = null
    private var mCamera: Camera? = null
    private var defaultCameraId = 0

    private var mTextViewHeartRate: TextView? =
        null
    private var mTextViewBloodPressure: TextView? = null
    private var mTextViewTemperature: TextView? = null
    private var mTextViewFace0Coordinates: TextView? = null
    private var mTextViewDebug: TextView? = null
    private var mTextViewAge: TextView? = null
    private var mTextViewSex: TextView? = null
    private var mTextViewHeight: TextView? = null
    private var mTextViewWeight: TextView? = null
    private var mTextViewPosition: TextView? = null
    val CAMERA_MIC_PERMISSION_REQUEST_CODE = 302


    private var previewWidth =
        0
    private var previewHeight: Int = 0 // Defined in surfaceChanged()


    /* Heart Rate Related Variables */
    private val heartRateFrameLength = 24 // WAS 256;

    private val arrayRed =
        DoubleArray(heartRateFrameLength) //ArrayList<Double> arrayRed = new ArrayList<Double>();

    private val arrayGreen =
        DoubleArray(heartRateFrameLength) //ArrayList<Double> arrayGreen = new ArrayList<Double>();

    private val arrayBlue =
        DoubleArray(heartRateFrameLength) //ArrayList<Double> arrayBlue = new ArrayList<Double>();

    private var systolicPressure = 0
    private var diastolicPressure: Int = 0
    private var temperature = 0f
    private var heartRate = 0.0
    private var frameNumber = 0

    /* Frame Frequency */
    private var samplingFrequency: Long = 0

    /* Face Detection Variables */
    private var numberOfFacesCurrentlyDetected = 0
    private var faceLeft0 = 0
    private var faceTop0: Int = 0
    private var faceRight0: Int = 0
    private var faceBottom0: Int = 0
    private val faceLeft1 = 0
    private var faceTop1: Int = 0
    private var faceRight1: Int = 0
    private var faceBottom1: Int = 0
    private val faceLeft2 = 0
    private var faceTop2: Int = 0
    private var faceRight2: Int = 0
    private var faceBottom2: Int = 0


    /* Writing to SD card */
    private var fileDataRed = ""
    private var fileDataGreen = ""
    private var fileDataBlue = ""

    /* Settings */
    private var displayEnglishUnits = true

    /* GPS */
    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null

    /* For Intents' onActivityResults() */
    private val CONTACT_PICKER_EMAIL_RESULT = 100
    private val CONTACT_PICKER_PHONE_NUMBER_RESULT = 200


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //requestWindowFeature(Window.FEATURE_NO_TITLE); // Hide the window title
        setContentView(R.layout.activity_blood_pressure)
        settings = getSharedPreferences(
            PREFS_NAME,
            0
        ) // Load saved stats // Only done once while app is running
        mTextViewHeartRate = findViewById<View>(R.id.textView0) as TextView
        mTextViewBloodPressure = findViewById<View>(R.id.textView1) as TextView
        mTextViewTemperature = findViewById<View>(R.id.textView2) as TextView
        mTextViewFace0Coordinates = findViewById<View>(R.id.textView3) as TextView
        mTextViewDebug = findViewById<View>(R.id.textView4) as TextView
        mTextViewAge = findViewById<View>(R.id.textViewRightSide0) as TextView
        mTextViewSex = findViewById<View>(R.id.textViewRightSide1) as TextView
        mTextViewHeight = findViewById<View>(R.id.textViewRightSide2) as TextView
        mTextViewWeight = findViewById<View>(R.id.textViewRightSide3) as TextView
        mTextViewPosition = findViewById<View>(R.id.textViewRightSide4) as TextView
        mRelativeLayoutRoot =
            findViewById<View>(R.id.relativeLayoutRoot) as RelativeLayout
        mFrameLayoutCameraPreview =
            findViewById<View>(R.id.frameLayoutCameraPreview) as FrameLayout
        mSurfaceViewCameraPreview =
            findViewById<View>(R.id.surfaceViewCameraPreview) as SurfaceView
        mImageViewRectangle0 =
            findViewById<View>(R.id.imageViewRectangle0) as ImageView

        requestPermissionForCameraAndMicrophone()
        // Create and add camera preview to screen
    } // END onCreate()


    override fun onResume() {
        super.onResume()
        loadPatientEditableStats()
        activateGps()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        deactivateGps()
        super.onDestroy()
    }

    fun setBloodPressure(
        heartRate: Double,
        age: Int,
        sex: String,
        weight: Int,
        height: Int,
        position: String
    ) {
        val R =
            18.5 // Average R = 18.31; // Vascular resistance // Very hard to calculate from person to person
        val Q: Double = if (sex.equals("Male", ignoreCase = true) || sex.equals(
                "M",
                ignoreCase = true
            )
        ) 5.0 else 4.5 // Liters per minute of blood through heart
        val ejectionTime = if (!position.equals(
                "Laying Down",
                ignoreCase = true
            )
        ) 386 - 1.64 * heartRate else 364.5 - 1.23 * heartRate // WAS ()?376-1.64*heartRate:354.5-1.23*heartRate; // ()?sitting:supine
        val bodySurfaceArea = 0.007184 * Math.pow(
            weight.toDouble(),
            0.425
        ) * Math.pow(height.toDouble(), 0.725)
        val strokeVolume =
            -6.6 + 0.25 * (ejectionTime - 35) - 0.62 * heartRate + 40.4 * bodySurfaceArea - 0.51 * age // Volume of blood pumped from heart in one beat
        val pulsePressure =
            Math.abs(strokeVolume / (0.013 * weight - 0.007 * age - 0.004 * heartRate + 1.307))
        val meanPulsePressure = Q * R
        systolicPressure = (meanPulsePressure + 4.5 / 3 * pulsePressure).toInt()
        diastolicPressure = (meanPulsePressure - pulsePressure / 3).toInt()
        mTextViewBloodPressure?.setText("Blood Pressure: $systolicPressure/$diastolicPressure")
        saveSharedPreference("systolicPressure", systolicPressure)
        saveSharedPreference("diastolicPressure", diastolicPressure)
    }

    /** A basic Camera preview class  */
    inner class CameraPreview(
        context: Context?,
        camera: Camera
    ) :
        SurfaceView(context), SurfaceHolder.Callback {
        var mPreviewSize: Size? = null
        var mSupportedPreviewSizes: List<Size>?

        init {
            val mHolder: SurfaceHolder
            mCamera = camera
            mHolder =
                holder // Install a SurfaceHolder.Callback so we get notified when the underlying surface is created and destroyed
            mHolder.addCallback(this)
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS) // deprecated, but required on Android versions prior to 3.0
            mSupportedPreviewSizes = mCamera!!.getParameters().getSupportedPreviewSizes()
        }

        override fun surfaceCreated(holder: SurfaceHolder) { // The Surface has been created, now tell the camera where to draw the preview
            try {
                mCamera?.setPreviewDisplay(holder)
                mCamera?.startPreview()
            } catch (e: Exception) {
            } // Camera is not available (in use or does not exist)
        } // END surfaceCreated()

        override fun surfaceDestroyed(holder: SurfaceHolder) { // Called right before surface is destroyed
            // Because the CameraDevice object is not a shared resource, it's very important to release it when the activity is paused.
            if (mCamera != null) {
                mCamera!!.setPreviewCallback(null) // This is for manually added buffers/threads // Use setPreviewCallback() for automatic buffers
                mCamera!!.stopPreview()
                mCamera!!.release() // release the camera for other applications
                mCamera = null
            }
            Toast.makeText(context, "surfaceDestroyed()", Toast.LENGTH_SHORT)
                .show()
        }

        override fun surfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            w: Int,
            h: Int
        ) {
            if (holder.surface == null) {
                return
            } // preview surface does not exist // WAS mHolder
            mCamera?.stopPreview() // stop preview before making changes
            previewWidth = w
            previewHeight = h
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, w, h)
            val parameters: Parameters? = mCamera?.getParameters()
            parameters?.setPreviewSize(mPreviewSize!!.width, mPreviewSize!!.height)
            mCamera?.setParameters(parameters)
            requestLayout()
            previewWidth = mPreviewSize!!.width
            previewHeight = mPreviewSize!!.height
            try {
                mCamera?.let {
                    setCameraDisplayOrientation(
                        this@BloodPressureActivity, defaultCameraId,
                        it
                    )
                }
                mCamera?.setFaceDetectionListener(MyFaceDetectionListener())
                mCamera?.setPreviewDisplay(holder) // WAS mHolder
                mCamera?.startPreview()
                startFaceDetection() // start face detection feature
            } catch (e: Exception) {
            } // Error starting camera preview


            /* Uncomment below to manually add buffers, aka get ~30 fps */
//          int dataBufferSize= previewHeight * previewWidth * (ImageFormat.getBitsPerPixel(mCamera.getParameters().getPreviewFormat()) / 8);
//          mCamera.addCallbackBuffer(new byte[dataBufferSize]);
//          mCamera.addCallbackBuffer(new byte[dataBufferSize]);
//          mCamera.addCallbackBuffer(new byte[dataBufferSize]);
//          mCamera.addCallbackBuffer(new byte[dataBufferSize]);
            mCamera?.setPreviewCallback(PreviewCallback { data, c ->

                // Gets called for every frame  // For manually added buffers/threads // Use setPreviewCallback() for automatic buffers
                // NOTE: not ran if buffer isn't big enough for data // NOTE: not all devices have cameras that support preview sizes at the same aspect ratio as the device's display
                if (numberOfFacesCurrentlyDetected == 0) {
                    mTextViewFace0Coordinates?.setText("Face Rectangle: No Face Detected")
                    frameNumber = 0
                    //				    	mImageViewRectangle0.bringToFront();
                    //				    	mImageViewRectangle0.setPadding(300, 400, 0, 0);
                    //mImageViewRectangle0.setPadding(c.getParameters().getPictureSize().width/2-75, c.getParameters().getPictureSize().height/2-100, 0, 0);
                } else {
                    //int top = faceLeft0, right = faceTop0, bottom = faceRight0, left = faceBottom0; // because coordinate systems are different
                    //int top = faceLeft0+1000, right = faceTop0+1000, bottom = faceRight0+1000, left = faceBottom0+1000; // because coordinate systems are different
                    //int top = faceLeft0+1000, left = faceTop0+1000, bottom = faceRight0+1000, right = faceBottom0+1000; // because coordinate systems are different and backwards
                    var left: Int = faceLeft0 + 1000
                    var top: Int = faceTop0 + 1000
                    val right: Int = faceRight0 + 1000
                    val bottom: Int = faceBottom0 + 1000
                    //int left = 50, top = 50, right = 100, bottom = 100; // NOTE: Negative values not accepted
                    //int smallPreviewWidth = right - left;
                    //int smallPreviewWidth = left - right; // because coordinate system is different
                    var smallPreviewWidth =
                        right - left + 1 // because coordinate system is different and backwards // 731
                    var smallPreviewHeight = bottom - top + 1 // 1300
                    val numberOfPixelsToAnalyze =
                        smallPreviewWidth * smallPreviewHeight // The number of pixels in the Face Rect
                    smallPreviewHeight =
                        smallPreviewHeight * previewHeight / 2000 // because backwards // 468
                    smallPreviewWidth =
                        smallPreviewWidth * previewWidth / 2000 // because backwards // 467
                    top = top * previewHeight / 2000 //
                    left = left * previewWidth / 2000 //
                    val topEnd = top + smallPreviewHeight //
                    val leftEnd = left + smallPreviewWidth //

                    //				    	mImageViewRectangle0.bringToFront();
                    //mImageViewRectangle0.setPadding(left, top, 0, 0);
                    //mImageViewRectangle0.forceLayout();
                    //mImageViewRectangle0.invalidate();
                    /** Trying to analyze part of the screen */
                    /** Trying to analyze part of the screen */
                    /** Trying to analyze part of the screen */
                    /** Trying to analyze part of the screen */
                    val outstr = ByteArrayOutputStream()
                    //			        	Log.d("DEBUG", "DEBUG: PreviewWidth,Height = " + previewWidth + "," + previewHeight); // 540,922
                    //			        	Log.d("DEBUG", "DEBUG: smallPreviewWidth,Height = " + smallPreviewWidth + "," + smallPreviewHeight); // 540,922
                    //			        	Log.d("DEBUG", "DEBUG: Rect left, top, right, bottom = " + top + "," + left + "," + topEnd + "," + leftEnd); // 0,0, 772,1372
                    val rect =
                        Rect(left, top, leftEnd, topEnd)
                    val yuvimage = YuvImage(
                        data,
                        ImageFormat.NV21,
                        previewWidth,
                        previewHeight,
                        null
                    ) // Create YUV image from byte[]
                    yuvimage.compressToJpeg(
                        rect,
                        100,
                        outstr
                    ) // Convert YUV image to Jpeg // NOTE: changes Rect's size
                    val bmp = BitmapFactory.decodeByteArray(
                        outstr.toByteArray(),
                        0,
                        outstr.size()
                    ) // Convert Jpeg to Bitmap
                    smallPreviewWidth = bmp.width
                    smallPreviewHeight = bmp.height
                    //			        	Log.d("DEBUG", "DEBUG: Bitmap Width,Height = " + smallPreviewWidth + "," + smallPreviewHeight);
                    var r = 0
                    var g = 0
                    var b = 0
                    val pix = IntArray(numberOfPixelsToAnalyze)
                    bmp.getPixels(
                        pix,
                        0,
                        smallPreviewWidth,
                        0,
                        0,
                        smallPreviewWidth,
                        smallPreviewHeight
                    )
                    for (i in 0 until smallPreviewHeight) {
                        for (j in 0 until smallPreviewWidth) {
                            val index = i * smallPreviewWidth + j
                            r += pix[index] shr 16 and 0xff //bitwise shifting
                            g += pix[index] shr 8 and 0xff
                            b += pix[index] and 0xff
                            //pix[index] = 0xff000000 | (r << 16) | (g << 8) | b; // to restore the values after RGB modification, use this statement, with adjustment above
                        }
                    }
                    r /= numberOfPixelsToAnalyze
                    g /= numberOfPixelsToAnalyze
                    b /= numberOfPixelsToAnalyze
                    if (frameNumber < heartRateFrameLength) {
                        mTextViewDebug?.setText("RGB: $r,$g,$b")
                        if (frameNumber == 0) {
                            samplingFrequency = System.nanoTime() // Start time
                            //Log.d("DEBUG RGB", "DEBUG: samplingFrequency: " + samplingFrequency);
                        }
                        fileDataRed += "$r " // a string to be saved on SD card
                        fileDataGreen += "$g " // a string to be saved on SD card
                        fileDataBlue += "$b " // a string to be saved on SD card
                        arrayRed[frameNumber] = r.toDouble()
                        arrayGreen[frameNumber] = g.toDouble()
                        arrayBlue[frameNumber] = b.toDouble()
                        mTextViewHeartRate?.setText("Heart Rate: in " + (heartRateFrameLength - frameNumber) + "..")
                        mTextViewBloodPressure?.setText("Blood Pressure: in " + (heartRateFrameLength - frameNumber + 1) + "..") // Shows how long until measurement will display
                        frameNumber++
                    } else if (frameNumber == heartRateFrameLength) { // So that these functions don't run every frame preview, just on the 32nd one // TODO add sound when finish
                        mTextViewHeartRate?.setText("Heart Rate: calculating..")
                        mTextViewBloodPressure?.setText("Blood Pressure: calculating..")
                        samplingFrequency =
                            System.nanoTime() - samplingFrequency // Minus end time = length of heartRateFrameLength frames
                        var finalSamplingFrequency: Double =
                            samplingFrequency / 1000000000.toDouble() // Length of time to get frames in seconds
                        finalSamplingFrequency =
                            heartRateFrameLength / finalSamplingFrequency // Frames per second in seconds
                        FastIcaRgb.preICA(
                            arrayRed,
                            arrayGreen,
                            arrayBlue,
                            heartRateFrameLength,
                            arrayRed,
                            arrayGreen,
                            arrayBlue
                        ) // heartRateFrameLength = 300 frames for now
                        val heartRateFrequency: Double =
                            Fft.FFT(arrayGreen, heartRateFrameLength, finalSamplingFrequency)
                        if (heartRateFrequency == 0.0) {
                            mTextViewHeartRate?.setText("Heart Rate: Error, try again")
                            mTextViewBloodPressure?.setText("Blood Pressure:")
                        } else {
                            heartRate =
                                Math.round(heartRateFrequency * 60 * 100) / 100.toDouble()
                            mTextViewHeartRate?.setText("Heart Rate: $heartRate")
                            mTextViewBloodPressure?.setText("Blood Pressure: in 0..") // Just informing the user that BP almost calculated
                            mTextViewDebug?.setText("Fps: $finalSamplingFrequency")
                            settings?.getInt("age", 25)?.let {
                                settings!!.getString("sex", "Male")?.let { it1 ->
                                    settings!!.getString("position", "Sitting")?.let { it2 ->
                                        setBloodPressure(
                                            heartRate,
                                            it,
                                            it1,
                                            settings!!.getInt("weight", 160),
                                            settings!!.getInt("height", 70),
                                            it2
                                        )
                                    }
                                }
                            }
                            val sample: Int = heartRate.toInt()
                            saveSharedPreference("heartRate", sample as Int)
                            frameNumber++ // Ensures this if-statement is only ran once by making frameNumber one bigger than heartRateLength
                            promptUserToSaveData() // Ask user if they would like to save this data
                        }
                    } else {
                        // do nothing
                    }
                }
                //mCamera.addCallbackBuffer(data); // "mCamera.addCallbackBuffer(data);" For manually added buffers/threads, aka ~18 fps
            } // END onPreviewFrame()
            ) // END mCamera.setPreviewCallback()
        } // END surfaceChanged

        override fun onMeasure(
            widthMeasureSpec: Int,
            heightMeasureSpec: Int
        ) {
            // We purposely disregard child measurements because act as a
            // wrapper to a SurfaceView that centers the camera preview instead
            // of stretching it.
            val width =
                View.resolveSize(suggestedMinimumWidth, widthMeasureSpec)
            val height =
                View.resolveSize(suggestedMinimumHeight, heightMeasureSpec)
            setMeasuredDimension(width, height)
            if (mSupportedPreviewSizes != null) {
                mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height)
            }
        } //        @Override

        //        protected void onLayout(boolean changed, int l, int t, int r, int b) {
        //            if (changed && getChildCount() > 0) {
        //                final View child = getChildAt(0);
        //
        //                final int width = r - l;
        //                final int height = b - t;
        //
        //                int previewWidth = width;
        //                int previewHeight = height;
        //                if (mPreviewSize != null) {
        //                    previewWidth = mPreviewSize.width;
        //                    previewHeight = mPreviewSize.height;
        //                }
        //
        //                // Center the child SurfaceView within the parent.
        //                if (width * previewHeight > height * previewWidth) {
        //                    final int scaledChildWidth = previewWidth * height / previewHeight;
        //                    child.layout((width - scaledChildWidth) / 2, 0,
        //                            (width + scaledChildWidth) / 2, height);
        //                } else {
        //                    final int scaledChildHeight = previewHeight * width / previewWidth;
        //                    child.layout(0, (height - scaledChildHeight) / 2,
        //                            width, (height + scaledChildHeight) / 2);
        //                }
        //            }
        //        }

    } // END class CameraPreview


    /** A safe way to get an instance of the Camera object. Call in onCreate()  */
    fun getCameraInstance(): Camera? {
        val numberOfCameras = getNumberOfCameras()

        // Find the ID of the default camera
        val cameraInfo = CameraInfo()
        for (i in 0 until numberOfCameras) {
            getCameraInfo(i, cameraInfo)
            if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
                defaultCameraId = i
            }
        }
        var c: Camera? = null
        try {
            c = open(defaultCameraId) // attempt to get a Camera instance
        } catch (e: Exception) {
        } // Camera is not available (in use or does not exist)
        return c // returns null if camera is unavailable
    }

    fun setCameraDisplayOrientation(
        activity: Activity,
        cameraId: Int,
        camera: Camera
    ) {
        val info = CameraInfo()
        getCameraInfo(cameraId, info)
        val rotation = activity.windowManager.defaultDisplay.rotation
        var degrees = 0
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }
        var result: Int
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360
            result = (360 - result) % 360 // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360
        }
        camera.setDisplayOrientation(result)
    }

    inner class MyFaceDetectionListener : FaceDetectionListener {
        override fun onFaceDetection(
            faces: Array<Face>,
            camera: Camera
        ) {
            numberOfFacesCurrentlyDetected = faces.size
            if (numberOfFacesCurrentlyDetected > 0) {
                faceLeft0 = faces[0].rect.left
                faceTop0 = faces[0].rect.top
                faceRight0 = faces[0].rect.right
                faceBottom0 = faces[0].rect.bottom
                mTextViewFace0Coordinates?.setText("Face Rectangle: ($faceLeft0,$faceTop0), ($faceRight0,$faceBottom0)")
                mImageViewRectangle0?.bringToFront()
                //mImageViewRectangle0.setPadding(100, 100, 0, 0);
                mImageViewRectangle0?.setPadding(
                    faceLeft0,
                    faceTop0,
                    0,
                    0
                ) // TODO: change coordinate system for left and top
                mImageViewRectangle0?.postInvalidate()

                // Try 4
//		    	Bitmap bitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.RGB_565);
//		        Paint paint = new Paint();
//		        paint.setColor(Color.BLUE);
//		        Canvas canvas = new Canvas(bitmap);
//		        canvas.drawColor(Color.TRANSPARENT);
//		        canvas.drawRect(25, 50, 75, 150, paint);
//		        mImageViewRectangle0.setImageBitmap(bitmap);

                // Try 3
                //mRectImage0.setPadding(left0, top0, previewWidth-right0, previewHeight-bottom0);
                //mRectImage0.bringToFront();

                // Try 2
//		    	ShapeDrawable rect = new ShapeDrawable(new RectShape());
//		        rect.getPaint().setColor(Color.GREEN);
//		        rect.setBounds(left0, top0, right0, bottom0);
//		        ImageView view1 = new ImageView(_activity);
//		        view1.setImageDrawable(rect);
//		        LinearLayout frame = (LinearLayout)findViewById(R.id.linearLayout1);
//		        frame.addView(view1);

                // Try 1
                //DrawRect drawRect = new DrawRect(_activity, faces);
                //setContentView(drawRect);
            }
            //	        if (numberOfFacesCurrentlyDetected > 1) {
//	            int left1   = faces[1].rect.left;
//	            int top1    = faces[1].rect.top;
//	            int right1  = faces[1].rect.right;
//	            int bottom1 = faces[1].rect.bottom;
//		    	mTextViewFace1Coordinates.setText("Face Rectangle: (" + left1 + "," + top1 + "), (" + right1 + "," + bottom1 + ")");
//	        }
//	        if (numberOfFacesCurrentlyDetected > 2) {
//	            int left2   = faces[2].rect.left;
//	            int top2    = faces[2].rect.top;
//	            int right2  = faces[2].rect.right;
//	            int bottom2 = faces[2].rect.bottom;
//		    	mTextViewFace2Coordinates.setText("Face Rectangle: (" + left2 + "," + top2 + "), (" + right2 + "," + bottom2 + ")");
//	        }
        }
    }

    fun startFaceDetection() {
        // Try starting Face Detection
        val params = mCamera!!.parameters

        // start face detection only *after* preview has started
        if (params.maxNumDetectedFaces > 0) {
            //Toast.makeText(_activity, "Max Num Faces Allows: " + params.getMaxNumDetectedFaces(), Toast.LENGTH_LONG).show();
            // camera supports face detection, so can start it:
            mCamera!!.startFaceDetection()
        }
    }

    private fun getOptimalPreviewSize(
        sizes: List<Size>?,
        w: Int,
        h: Int
    ): Size? {
        val ASPECT_TOLERANCE = 0.2
        val targetRatio = w.toDouble() / h
        if (sizes == null) return null
        var optimalSize: Size? = null
        var minDiff = Double.MAX_VALUE

        // Try to find an size match aspect ratio and size
        for (size in sizes) {
            val ratio = size.width.toDouble() / size.height
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue
            if (Math.abs(size.height - h) < minDiff) {
                optimalSize = size
                minDiff = Math.abs(size.height - h).toDouble()
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE
            for (size in sizes) {
                if (Math.abs(size.height - h) < minDiff) {
                    optimalSize = size
                    minDiff = Math.abs(size.height - h).toDouble()
                }
            }
        }
        return optimalSize
    }

    private fun promptUserToSaveData() {
        val input =
            EditText(this) // Added for debugging purposes, not needed in final product
        input.hint = "HR,BP,BP,temp"
        AlertDialog.Builder(this)
            .setTitle("Save Data?")
            .setMessage("Would you like to save the data?")
            .setView(input) // Added for debugging purposes, not needed in final product
            .setPositiveButton(
                "Okay"
            ) { dialog, whichButton ->
                val value =
                    input.text // Added for debugging purposes, not needed in final product
                if (StorageUtils.isExternalStorageAvailableAndWritable()) {
                    val timeNow =
                        Time(Time.getCurrentTimezone())
                    timeNow.setToNow()
                    writeToTextFile(
                        """
                            ${getFormattedVitalSigns()}
                            Time: ${timeNow.format2445()},$value
                            """.trimIndent(),
                        "data-LastMeasurement"
                    )
                    writeToTextFile2(
                        """
                            $heartRate,$systolicPressure,$diastolicPressure,$temperature,${if (displayEnglishUnits) "F" else "C"},${timeNow.format2445()},$value
                            
                            """.trimIndent()
                    )
                } else {
                    Toast.makeText(
                        this@BloodPressureActivity,
                        "SD card storage not available",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.setNegativeButton(
                "Cancel"
            ) { dialog, whichButton ->
                // Do nothing.
            }.show()
    }


    /* fun onClickGoToEditStats(v: View?) {
         startActivity(Intent(this, ::class.java))
     }

     fun onClickGoToTemperature(v: View?) {
         startActivity(Intent(this, AddTemperatureActivity::class.java))
     }*/

    private fun loadPatientEditableStats() {
        displayEnglishUnits = settings!!.getBoolean("displayEnglishUnits", true)
        temperature = settings!!.getFloat("internalTemperature", 0f)
        if (displayEnglishUnits) {
            mTextViewAge!!.text = "Age: " + settings!!.getInt("age", 23)
            mTextViewSex?.setText("Sex: " + settings!!.getString("sex", "Male"))
            mTextViewWeight?.setText("Weight: " + settings!!.getInt("weight", 160) + " pounds")
            mTextViewHeight?.setText("Height: " + settings!!.getInt("height", 75) + " inches")
            mTextViewPosition?.setText("Position: " + settings!!.getString("position", "Sitting"))
            mTextViewTemperature?.setText("Temperature: $temperature") // TODO: Add Click to add..
        } else {
            mTextViewAge!!.text = "Age: " + settings!!.getInt("age", 23)
            mTextViewSex?.setText("Sex: " + settings!!.getString("sex", "Male"))
            mTextViewWeight?.setText("Weight: " + settings!!.getInt("weight", 73) + " kg")
            mTextViewHeight?.setText("Height: " + settings!!.getInt("height", 75) + " cm")
            mTextViewPosition?.setText("Position: " + settings!!.getString("position", "Sitting"))
            mTextViewTemperature?.setText("Temperature: $temperature") // TODO: Add Click to add..
        }
    }

    private fun getFormattedVitalSigns(): String {
        return """Heart Rate: $heartRate bpm
Blood Pressure: $systolicPressure/$diastolicPressure
Temperature: $temperature${if (displayEnglishUnits == true) " F" else " C"}"""
    }

    private fun saveSharedPreference(key: String, value: Int) {
        val editor = settings!!.edit() // Needed to make changes
        editor.putInt(key, value)
        editor.commit() // This line saves the edits
    }

    private fun saveSharedPreference(key: String, value: String) {
        val editor = settings!!.edit() // Needed to make changes
        editor.putString(key, value)
        editor.commit() // This line saves the edits
    }


    /** Menu Related Items  */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_common, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.menu_settings -> {
                // Do nothing
                saveSharedPreference(
                    "userInputForEmail",
                    ""
                ) // This is done so that the one-time popup will show again
                saveSharedPreference("userInputForPhoneNumber", "")
                Toast.makeText(this, "Settings returned to default", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_convertUnits -> {
                onMenuClickConvertUnits()
                true
            }
            R.id.menu_sendEmail -> {
                if (settings!!.getString("userInputForEmail", "") == "") {
                    promptUserInputForEmail() // Basically runs just one time
                } else {
                    sendEmail(
                        settings!!.getString("userInputForEmail", ""),
                        getFormattedVitalSigns()
                    )
                }
                true
            }
            R.id.menu_sendSMS -> {
                if (settings!!.getString("userInputForPhoneNumber", "") == "") {
                    promptUserInputForPhoneNumber() // Basically runs just one time
                } else {
                    sendSMS(
                        settings!!.getString("userInputForPhoneNumber", ""),
                        getFormattedVitalSigns()
                    )
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    } // END onOptionsItemSelected()


    private fun onMenuClickConvertUnits() {
        displayEnglishUnits = !displayEnglishUnits // Toggle units.
        val editor = settings!!.edit() // Needed to make changes
        if (displayEnglishUnits) {
            editor.putInt("weight", (settings!!.getInt("weight", 73) * 2.20462).toInt())
            editor.putInt("height", (settings!!.getInt("height", 190) / 2.54).toInt())
            editor.putBoolean("displayEnglishUnits", displayEnglishUnits)
            editor.commit() // This line saves the edits
            mTextViewWeight?.setText("Weight: " + settings!!.getInt("weight", 73) + " pounds")
            mTextViewHeight?.setText("Height: " + settings!!.getInt("height", 190) + " inches")
        } else { // Metric
            editor.putInt("weight", (settings!!.getInt("weight", 160) / 2.20462).toInt())
            editor.putInt("height", (settings!!.getInt("height", 75) * 2.54).toInt())
            editor.putBoolean("displayEnglishUnits", displayEnglishUnits)
            editor.commit() // This line saves the edits
            mTextViewWeight?.setText("Weight: " + settings!!.getInt("weight", 160) + " kg")
            mTextViewHeight?.setText("Height: " + settings!!.getInt("height", 75) + " cm")
        }
    }

    private fun promptUserInputForEmail() {
        val input = EditText(this)
        input.setText(settings!!.getString("userInputForEmail", ""))
        AlertDialog.Builder(this)
            .setTitle("Enter Email")
            .setMessage("Where would you like to email the data?")
            .setView(input)
            .setPositiveButton(
                "Okay"
            ) { dialog, whichButton -> //Editable value = input.getText();
                saveSharedPreference("userInputForEmail", input.text.toString())
                sendEmail(settings!!.getString("userInputForEmail", ""), getFormattedVitalSigns())
            }.setNegativeButton(
                "Contacts"
            ) { dialog, whichButton ->
                val contactPickerIntent = Intent(
                    Intent.ACTION_PICK,
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI
                )
                contactPickerIntent.type =
                    "vnd.android.cursor.dir/email" // ContactsContract.CommonDataKinds.Email
                startActivityForResult(contactPickerIntent, CONTACT_PICKER_EMAIL_RESULT)
            }.show()
    }

    private fun sendEmail(to: String?, message: String) {
        try {
            val emailIntent = Intent(Intent.ACTION_SEND)
            emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Vital Signs")
            emailIntent.putExtra(Intent.EXTRA_TEXT, message)
            emailIntent.type =
                "vnd.android.cursor.dir/email" // Or "text/plain" "text/html" "plain/text"
            startActivity(emailIntent) // Use if you want the choice to automatically use the same option every time
            //startActivity(Intent.createChooser(emailIntent, "Send email:")); // Use if you want available options to appear every time
        } catch (e: ActivityNotFoundException) {
            Log.e("Emailing contact", "Email failed", e)
        }
    }

    private fun promptUserInputForPhoneNumber() {
        val input = EditText(this)
        input.setText(settings!!.getString("userInputForPhoneNumber", ""))
        AlertDialog.Builder(this)
            .setTitle("Enter Phone Number")
            .setMessage("Where would you like to text the data?")
            .setView(input)
            .setPositiveButton(
                "Okay"
            ) { dialog, whichButton -> //Editable value = input.getText();
                saveSharedPreference("userInputForPhoneNumber", input.text.toString())
                sendSMS(
                    settings!!.getString("userInputForPhoneNumber", ""),
                    getFormattedVitalSigns()
                )
                Toast.makeText(this@BloodPressureActivity, "Text sent", Toast.LENGTH_LONG)
                    .show()
            }.setNegativeButton(
                "Contacts"
            ) { dialog, whichButton ->
                val contactPickerIntent = Intent(
                    Intent.ACTION_PICK,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                )
                contactPickerIntent.type = "vnd.android.cursor.dir/phone"
                startActivityForResult(contactPickerIntent, CONTACT_PICKER_PHONE_NUMBER_RESULT)
            }.show()
    }

    private fun sendSMS(phoneNumber: String?, message: String) {
//		Uri smsUri = Uri.parse("sms:" + phoneNumber);
//		Intent intent = new Intent(Intent.ACTION_VIEW, smsUri);
//		intent.putExtra("sms_body", message);
//		intent.setType("vnd.android-dir/mms-sms");
//		startActivity(intent);
        val sms = SmsManager.getDefault()
        sms.sendTextMessage(phoneNumber, null, message, null, null)
    }

    private fun writeToTextFile(data: String, fileName: String) {
        val sdCard = Environment.getExternalStorageDirectory()
        val directory = File(sdCard.absolutePath + "/VitalSigns")
        directory.mkdirs()
        val file = File(directory, "$fileName.txt")
        val fOut: FileOutputStream
        try {
            fOut = FileOutputStream(file)
            val osw = OutputStreamWriter(fOut)
            osw.write(data)
            osw.flush()
            osw.close()
            //Toast.makeText(_activity, "Data successfully save in " + file.toString(), Toast.LENGTH_LONG).show();
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun writeToTextFile2(data: String) {
        val sdCard = Environment.getExternalStorageDirectory()
        val directory = File(sdCard.absolutePath + "/VitalSigns")
        directory.mkdirs()
        val file = File(directory, "data.csv")
        val fOut: FileOutputStream
        try {
            fOut = FileOutputStream(
                file,
                true
            ) // NOTE: This (", true") is the key to not overwriting everything
            val osw = OutputStreamWriter(fOut)
            osw.write(data)
            osw.flush()
            osw.close()
            Toast.makeText(this, "Data successfully save in $file", Toast.LENGTH_LONG)
                .show()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

/*
    */
    /** For Intents that return a value  *//*
    fun onActivityResult(reqCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(reqCode, resultCode, data)
        when (reqCode) {
            CONTACT_PICKER_EMAIL_RESULT -> if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Email: $data", Toast.LENGTH_LONG).show()
                sendEmail(data.toString(), getFormattedVitalSigns())
            }
            CONTACT_PICKER_PHONE_NUMBER_RESULT -> if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Phone Number: $data", Toast.LENGTH_LONG).show()
                sendSMS(data.toString(), getFormattedVitalSigns())
            }
            else ->                 // Code should never get here
                Toast.makeText(
                    this,
                    "Something wrong with code. Data: $data",
                    Toast.LENGTH_LONG
                ).show()
        }
    }*/


    /** GPS Related Code  */
    fun activateGps() {
        // Acquire a reference to the system Location Manager
        locationManager =
            this.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Define a listener that responds to location updates
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) { // Called when a new location is found by the network location provider.
                saveSharedPreference("currentLatitude", (location.latitude * 1000000).toInt())
                saveSharedPreference(
                    "currentLongitude",
                    (location.longitude * 1000000).toInt()
                )
                deactivateGps()
            }

            override fun onStatusChanged(
                provider: String,
                status: Int,
                extras: Bundle
            ) {
            }

            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        // Register the listener with the Location Manager to receive location updates
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        locationManager!!.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            0,
            0f,
            locationListener as LocationListener
        )
    }

    fun deactivateGps() { // Remove the listener you previously added
        try {
            locationManager!!.removeUpdates(locationListener!!)
        } catch (e: IllegalArgumentException) { // intent is null
            e.printStackTrace()
        }
    }

    private fun checkPermissionForCameraAndMicrophone(): Boolean {
        val resultCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val resultMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)

        return resultCamera == PackageManager.PERMISSION_GRANTED &&
                resultMic == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissionForCameraAndMicrophone() {
        if (!checkPermissionForCameraAndMicrophone()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                CAMERA_MIC_PERMISSION_REQUEST_CODE
            )
        } else {
            mCamera = getCameraInstance()
            mFrameLayoutCameraPreview!!.addView(
                CameraPreview(
                    this,
                    mCamera!!
                )
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == CAMERA_MIC_PERMISSION_REQUEST_CODE) {
            var cameraAndMicPermissionGranted = true

            for (grantResult in grantResults) {
                cameraAndMicPermissionGranted = cameraAndMicPermissionGranted and
                        (grantResult == PackageManager.PERMISSION_GRANTED)
            }
            if (cameraAndMicPermissionGranted) {
                requestPermissionForCameraAndMicrophone()
            } else {
                //  toast(R.string.permissions_needed)
            }
        }
    }
}