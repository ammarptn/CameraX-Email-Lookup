package com.ammarptn.camerax

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState==null) {
            var cameraFragment = supportFragmentManager.findFragmentByTag("cameraFragment") as CameraFragment?
            if (cameraFragment == null) {
                cameraFragment = CameraFragment.newInstance()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, cameraFragment, "cameraFragment")
                .commitNow()
        }
    }
}
