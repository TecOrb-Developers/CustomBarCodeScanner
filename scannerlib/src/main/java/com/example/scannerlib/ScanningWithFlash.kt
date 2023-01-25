package com.example.scannerlib

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Camera
import android.hardware.camera2.CameraManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.SparseArray
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.example.scannerlib.databinding.ActivityScannerBinding
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import java.io.IOException
import java.lang.reflect.Field
import java.util.*


class ScanningWithFlash : AppCompatActivity() {
    lateinit var binding: ActivityScannerBinding
    private var surfaceView: SurfaceView? = null
    private var cameraSource: CameraSource? = null
    private var myCamera: Camera? = null
    private var setFlash=true
    private var barcodeDetector: BarcodeDetector? = null
    private val REQUEST_CAMERA_PERMISSION = 201
    private var barcodeData: String? = null
    private val toneGen1: ToneGenerator? = null
    private var barcodeText: TextView? = null

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_scanner)
        Toast.makeText(this, "onCreate", Toast.LENGTH_SHORT).show()

        blink()

        initialiseDetectorsAndSources()
    }

    private fun blink() {
        val handler = Handler()
        Thread(Runnable {
            val timeToBlink = 500
            try {
                Thread.sleep(timeToBlink.toLong())
            } catch (e: Exception) {
            }

            handler.post(Runnable {

                if (binding.line.visibility == View.VISIBLE) {
                    binding.line.visibility = View.INVISIBLE
                } else {
                    binding.line.visibility = View.VISIBLE
                }
              blink()
            })
        }).start()
    }


    @RequiresApi(Build.VERSION_CODES.M)
    fun initialiseDetectorsAndSources() {


        surfaceView = findViewById(R.id.surface_view)
        barcodeText = findViewById(R.id.barcode_text)
        barcodeDetector = BarcodeDetector.Builder(this)
            .setBarcodeFormats(Barcode.ALL_FORMATS)
            .build()

        cameraSource = CameraSource.Builder(this, barcodeDetector)
            .setRequestedPreviewSize(268, 200)
            .setAutoFocusEnabled(true) //you should add this feature
            .build()

        surfaceView?.holder?.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                try {
                    if (ActivityCompat.checkSelfPermission(
                            this@ScanningWithFlash,
                            Manifest.permission.CAMERA
                        ) === PackageManager.PERMISSION_GRANTED
                    ) {
                       binding.flash.setOnClickListener{
                           setFlash()
                       }
                        cameraSource?.start(surfaceView!!.holder)

                    } else {
                        ActivityCompat.requestPermissions(
                            this@ScanningWithFlash,
                            arrayOf<String>(Manifest.permission.CAMERA),
                            REQUEST_CAMERA_PERMISSION
                        )
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                cameraSource?.stop()
            }
        })
        barcodeDetector?.setProcessor(object : Detector.Processor<Barcode?> {
            override fun release() {
                // Toast.makeText(getApplicationContext(), "To prevent memory leaks barcode scanner has been stopped", Toast.LENGTH_SHORT).show();
            }

            override fun receiveDetections(detections: Detector.Detections<Barcode?>) {
                val barcodes: SparseArray<Barcode?>? = detections.detectedItems
                if (barcodes?.size() != 0) {
                    barcodeText?.post {
                        if (barcodes?.valueAt(0)?.email != null) {
                            barcodeText?.removeCallbacks(null)
                            barcodeData = barcodes.valueAt(0)!!.email.address
                            barcodeText?.text = barcodeData
                            toneGen1?.startTone(ToneGenerator.TONE_CDMA_PIP, 150)
                        } else {
                            barcodeData = barcodes?.valueAt(0)?.displayValue
                            barcodeText?.text = barcodeData
                            val returnIntent = Intent()
                            returnIntent.putExtra("result", barcodeData)
                            setResult(RESULT_OK, returnIntent)
                            finish()
                            toneGen1?.startTone(ToneGenerator.TONE_CDMA_PIP, 150)
                        }
                    }
                }
            }
        })
    }

    @Throws(IOException::class)
    fun setFlash() {
        packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            cameraSource!!.start(surfaceView!!.holder)
            myCamera = getCamera(cameraSource)!!
        if(setFlash) {
            setFlash = false
            if (myCamera != null) {
                val _pareMeters = myCamera?.parameters
                _pareMeters?.flashMode = Camera.Parameters.FLASH_MODE_TORCH
                binding.flash.setBackgroundResource(R.drawable.dark_torch)
                myCamera?.parameters = _pareMeters
                myCamera?.startPreview()
            }
        }else if(!setFlash) {
            val _pareMeters = myCamera?.parameters
            _pareMeters?.flashMode = Camera.Parameters.FLASH_MODE_OFF
            binding.flash.setBackgroundResource(R.drawable.light_torch)
            myCamera?.parameters = _pareMeters
            myCamera?.startPreview()
            setFlash=true
        }
    }

    override fun onPause() {
        super.onPause()
        supportActionBar!!.hide()
        cameraSource!!.release()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onResume() {
        super.onResume()
        supportActionBar!!.hide()
        initialiseDetectorsAndSources()
    }

    fun getCamera(cameraSource: CameraSource?): Camera? {
        val declaredFields: Array<Field> = CameraSource::class.java.declaredFields
        for (field in declaredFields) {
            if (field.type === Camera::class.java) {
                field.isAccessible = true
                try {
                    val camera = field.get(cameraSource) as Camera
                    return if (camera != null) {
                        camera
                    } else null
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                }
                break
            }
        }
        return null
    }


}
