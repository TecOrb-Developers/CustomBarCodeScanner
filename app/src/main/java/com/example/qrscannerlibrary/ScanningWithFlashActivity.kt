package com.example.qrscannerlibrary

//import kotlinx.coroutines.flow.internal.NoOpContinuation.context

import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.CompoundButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.qrscannerlibrary.databinding.ActivityScanningWithFlashBinding
import com.example.scannerlib.ScanningWithFlash


class ScanningWithFlashActivity : AppCompatActivity() {
    lateinit var binding:ActivityScanningWithFlashBinding
    private lateinit var cameraId: String
    private lateinit var result: String
    private var camManager: CameraManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=DataBindingUtil.setContentView(this,R.layout.activity_scanning_with_flash)

        binding.flash.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
            @RequiresApi(Build.VERSION_CODES.M)
            override fun onCheckedChanged(p0: CompoundButton?, p1: Boolean) {
                switchFlashLight(p1)
            }

        })
        val i = Intent(this,ScanningWithFlash::class.java)
        startActivityForResult(i,150)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun switchFlashLight(isChecked: Boolean) {

        try {
            camManager = getSystemService(CAMERA_SERVICE) as CameraManager
            cameraId = camManager!!.cameraIdList[0]
            camManager?.setTorchMode(cameraId, isChecked)
//            result= isChecked.toString() ? "ON" : "OFF"
        } catch (e: CameraAccessException) {
            Log.d("TAG", "switchFlashLight: $e")
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 150) {
            if (resultCode == RESULT_OK) {
                val result: String =data?.getStringExtra("result")!!
                Toast.makeText(this, result, Toast.LENGTH_SHORT).show()
            }
            if (resultCode === RESULT_CANCELED) {
            }
        }
    }


}