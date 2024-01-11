package com.nexus.farmap.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nexus.farmap.R
import com.nexus.farmap.data.ml.classification.ARCoreSessionLifecycleHelper
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val arCoreSessionHelper = ARCoreSessionLifecycleHelper(this)

        arCoreSessionHelper.beforeSessionResume = { session ->
            session.configure(session.config.apply {
                // To get the best image of the object in question, enable autofocus.
                focusMode = Config.FocusMode.AUTO
                if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    depthMode = Config.DepthMode.AUTOMATIC
                }
            })

            val filter = CameraConfigFilter(session).setFacingDirection(CameraConfig.FacingDirection.BACK)
            val configs = session.getSupportedCameraConfigs(filter)
            val sort = compareByDescending<CameraConfig> { it.imageSize.width }.thenByDescending { it.imageSize.height }
            session.cameraConfig = configs.sortedWith(sort)[0]
        }

    }

}