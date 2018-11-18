package com.zabozhanov.whoisthere.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import cafe.adriel.kbus.KBus
import com.tbruyelle.rxpermissions2.Permission
import com.tbruyelle.rxpermissions2.RxPermissions
import com.zabozhanov.whoisthere.KnockService
import com.zabozhanov.whoisthere.MyApp
import com.zabozhanov.whoisthere.R
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private val PREFS_FILENAME = "com.zabozhanov.whoisthere.prefs"
    private var persmissions: Observable<Permission>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        persmissions = RxPermissions(this)
                .requestEach(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        /*Manifest.permission.GET_ACCOUNTS,*/
                        Manifest.permission.CAMERA)
                .observeOn(AndroidSchedulers.mainThread())

        val prefs = this.getSharedPreferences(PREFS_FILENAME, 0)
        val editor = prefs.edit()

        val firstStart = prefs.getBoolean("first", true)
        if (!firstStart) {
            requestPermissions()
        }
        editor.putBoolean("first", false).apply()

        btnSettings.setOnClickListener {
            startActivity(GraphActivity.getIntent(this))
        }
    }


    private fun checkPermissions() {
        val allGranted = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        /*&& checkSelfPermission(Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED*/
        if (allGranted) {
            txtHello.visibility = View.GONE
            btnPause.visibility = View.VISIBLE
            btnEnable.text = getString(R.string.camera_setup)
            btnEnable.setOnClickListener {
                val intent = RecordActivity.getIntent(this)
                intent.putExtra("setup", true)
                startActivity(intent)
            }
        } else {
            btnPause.visibility = View.GONE
            btnEnable.text = getString(R.string.go)
            txtHello.visibility = View.VISIBLE
            btnEnable.setOnClickListener {
                requestPermissions()
            }
        }

        btnPause.setOnClickListener {
            KBus.post(KnockService.ServiceEvent(KnockService.ServiceMsg.PAUSE_30))
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }


    private fun requestPermissions() {
        persmissions?.subscribe { permission ->
            if (!permission.granted) {
                requestPermissions()
            } else {
                init()
            }
        }
    }

    private fun init() {
        //startService(Intent(MyApp.instance, KnockService::class.java))
    }

}
