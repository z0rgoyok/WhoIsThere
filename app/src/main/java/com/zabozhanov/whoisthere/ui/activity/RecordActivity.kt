package com.zabozhanov.whoisthere.ui.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PointF
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import com.arellomobile.mvp.presenter.InjectPresenter
import com.zabozhanov.whoisthere.R
import com.zabozhanov.whoisthere.presentation.presenter.RecordPresenter
import com.zabozhanov.whoisthere.presentation.view.RecordView
import kotlinx.android.synthetic.main.activity_record.*
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import android.graphics.Bitmap
import cafe.adriel.kbus.KBus
import com.otaliastudios.cameraview.*
import java.io.FileOutputStream
import com.otaliastudios.cameraview.CameraUtils
import com.zabozhanov.whoisthere.data.RecordEventCallBackEvent
import java.io.ByteArrayOutputStream

class RecordActivity : BaseActivity(), RecordView {
    companion object {
        const val TAG = "RecordActivity"
        fun getIntent(context: Context): Intent = Intent(context, RecordActivity::class.java)
        var isStarted = false
    }

    @InjectPresenter
    lateinit var mRecordPresenter: RecordPresenter
    val PREFS_FILENAME = "com.zabozhanov.whoisthere.prefs"


    var prefs: SharedPreferences? = null

    @SuppressLint("SimpleDateFormat")
    var df: DateFormat = SimpleDateFormat("yyyyMMddHHmmss") as DateFormat

    private val dirFile: File = File(Environment.getExternalStorageDirectory().absolutePath + "/WhoIsThere/Videos/")

    /*@get:Arg(optional = true) var name: String by argExtra(defaultName)
    @get:Arg(optional = true) val id: Int by argExtra(defaultId)
    @get:Arg var grade: Char  by argExtra()
    @get:Arg val passing: Boolean by argExtra()*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)
        //ActivityStarter.fill(this, savedInstanceState)

        prefs = this.getSharedPreferences(PREFS_FILENAME, 0)
        val prefsEditor = prefs?.edit()

        cameraView.addCameraListener(object : CameraListener() {
            override fun onVideoTaken(video: File?) {
                if (video != null) {
                    KBus.post(RecordEventCallBackEvent(video.absolutePath))
                }
            }

            override fun onFocusEnd(successful: Boolean, point: PointF?) {
                if (point != null) {
                    prefsEditor?.putFloat("focusX", point.x)
                    prefsEditor?.putFloat("focusY", point.y)
                    prefsEditor?.apply()
                }
            }

            override fun onZoomChanged(newValue: Float, bounds: FloatArray?, fingers: Array<out PointF>?) {
                prefsEditor?.putFloat("zoom", newValue)
                prefsEditor?.apply()
            }

            override fun onPictureTaken(jpeg: ByteArray?) {
                dirFile.mkdirs()
                if (jpeg != null) {
                    var fileName = df.format(Date()) + ".jpg"
                    fileName = dirFile.absolutePath + "/" + fileName
                    CameraUtils.decodeBitmap(jpeg) {
                        try {
                            val stream = ByteArrayOutputStream()
                            it.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                            val byteArray = stream.toByteArray()
                            val out = FileOutputStream(fileName)
                            out.write(byteArray)
                            out.flush()
                            out.close()
                            KBus.post(RecordEventCallBackEvent(fileName))
                        } catch (e: Exception) {
                            e.printStackTrace()
                            KBus.post(RecordEventCallBackEvent(null))
                        }
                    }
                } else {
                    KBus.post(RecordEventCallBackEvent(null))
                }
            }
        })

        cameraView.mapGesture(Gesture.PINCH, GestureAction.ZOOM)
        cameraView.mapGesture(Gesture.TAP, GestureAction.FOCUS_WITH_MARKER)
        cameraView.playSounds = false
    }

    override fun startRecord(lengthInSec: Int) {
        dirFile.mkdirs()
        var fileName = df.format(Date()) + ".mp4"
        fileName = dirFile.absolutePath + "/" + fileName
        cameraView.videoMaxDuration = TimeUnit.SECONDS.toMillis(lengthInSec.toLong()).toInt()
        cameraView.startCapturingVideo(File(fileName))
    }

    override fun takePic() {
        cameraView.capturePicture()
    }

    override fun onResume() {
        super.onResume()
        mRecordPresenter.onResume()
        cameraView.start()

        val delayHandler = Handler()

        delayHandler.postDelayed({
            val zoom = prefs?.getFloat("zoom", -1.0f)
            if (zoom != null && zoom > -1.0f) {
                cameraView.zoom = zoom
            }
        }, 200)

        delayHandler.postDelayed({
            val focusX = prefs?.getFloat("focusX", -1.0f)
            val focusY = prefs?.getFloat("focusY", -1.0f)
            if (focusX != null && focusY != null) {
                if (focusX > -1.0f && focusY > -1.0f) {
                    cameraView.startAutoFocus(focusX, focusY)
                }
            }
        }, 500)

        isStarted = true
    }

    override fun onPause() {
        super.onPause()
        mRecordPresenter.onPause()
        cameraView.stop()
        isStarted = false
    }

    override fun onDestroy() {
        super.onDestroy()
        mRecordPresenter.onPause()
        cameraView.destroy()
    }

    override fun stopRecord() {
        cameraView.stopCapturingVideo()
    }

    override fun closeSelf() {
        finish()
    }
}
