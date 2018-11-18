package com.zabozhanov.whoisthere

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import java.lang.Math.abs


open class SpikeDetector(sensorManager: SensorManager?) : SensorEventListener {

    private var sensorManager: SensorManager? = null

    init {
        this.sensorManager = sensorManager
    }

    private val updateFrequency = 70


    val thresholdZ = 1f
    val threshholdX = 5f
    val threshholdY = 5f

    private var prevZVal = 0f
    private var currentZVal = 0f
    private var diffZ = 0f

    private var prevXVal = 0f
    private var currentXVal = 0f
    private var diffX = 0f

    private var prevYVal = 0f
    private var currentYVal = 0f
    private var diffY = 0f

    private var spikeListener: (() -> Unit)? = null
    private var pauseHandler: Handler? = null


    private var accelSensor: Sensor? = null
    private var gyroSensor: Sensor? = null

    private var accelX = 0.0f
    private var accelY = 0.0f
    private var accelZ = 0.0f
    private var gyroX = 0.0f
    private var gyroY = 0.0f
    private var gyroZ = 0.0f


    open fun start(spikeListener: () -> Unit) {
        sensorManager?.registerListener(this, sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                1000000 / updateFrequency)

        /*accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        sensorManager?.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager?.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL)*/

        this.spikeListener = spikeListener
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) {
            return
        }
        if (pauseHandler != null) {
            return
        }
        prevXVal = currentXVal
        currentXVal = abs(event.values[0])
        diffX = currentXVal - prevXVal
        // X-axis

        prevYVal = currentYVal
        currentYVal = abs(event.values[1]) // Y-axis
        diffY = currentYVal - prevYVal

        prevZVal = currentZVal
        currentZVal = abs(event.values[2]) // Z-axis
        diffZ = currentZVal - prevZVal

        //Z force must be above some limit, the other forces below some limit to filter out shaking motions
        if (currentZVal > prevZVal && diffZ > thresholdZ && diffX < threshholdX && diffY < threshholdY) {
            spikeListener?.invoke()
            pauseHandler = Handler()
            pauseHandler?.postDelayed({
                pauseHandler = null
            }, 15)
        }

        /*val sensor = event.sensor
        when (sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                accelX = event.values[0]
                accelY = event.values[1]
                accelZ = event.values[2]
                if ((accelZ > 10.5 || accelZ < -10.5) && (abs(gyroX) < 0.1)
                        && (abs(gyroY) < 0.1) && (abs(gyroZ) < 0.1)) {
                    spikeListener?.run()
                    pauseHandler = Handler()
                    pauseHandler?.postDelayed({
                        pauseHandler = null
                    }, 25)
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                gyroX = event.values[0]
                gyroY = event.values[1]
                gyroZ = event.values[2]
            }
        }*/
    }
}