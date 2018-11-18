package com.zabozhanov.whoisthere.ui.activity

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import com.arellomobile.mvp.presenter.InjectPresenter
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.zabozhanov.whoisthere.R
import com.zabozhanov.whoisthere.presentation.presenter.GraphPresenter
import com.zabozhanov.whoisthere.presentation.view.GraphView
import kotlinx.android.synthetic.main.activity_graph.*


class GraphActivity : BaseActivity(), GraphView, SensorEventListener {
    companion object {
        const val TAG = "GraphActivity"
        fun getIntent(context: Context): Intent = Intent(context, GraphActivity::class.java)
    }

    @InjectPresenter
    lateinit var mGraphPresenter: GraphPresenter

    private var sensorManager: SensorManager? = null
    private val updateFrequency = 100
    private var isRecording = false

    private val listX = mutableListOf<DataPoint>()
    private val listY = mutableListOf<DataPoint>()
    private val listZ = mutableListOf<DataPoint>()

    private var startTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager?
        sensorManager?.registerListener(this, sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                1000000 / updateFrequency)
        btnStartStop.setOnClickListener {
            isRecording = !isRecording
            if (isRecording) {
                startTime = System.currentTimeMillis()

                listX.clear()
                listY.clear()
                listZ.clear()
            } else {
                graphViewX.removeAllSeries()
                graphViewY.removeAllSeries()
                graphViewZ.removeAllSeries()

                graphViewX.addSeries(LineGraphSeries(listX.toTypedArray()))
                graphViewY.addSeries(LineGraphSeries(listY.toTypedArray()))
                graphViewZ.addSeries(LineGraphSeries(listZ.toTypedArray()))
            }
        }

        /*graphViewX.viewport.isScalable = true
        graphViewX.viewport.isScrollable = true

        graphViewY.viewport.isScalable = true
        graphViewY.viewport.isScrollable = true

        graphViewZ.viewport.isScalable = true
        graphViewZ.viewport.isScrollable = true*/
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

    }

    override fun onSensorChanged(p0: SensorEvent?) {
        if (isRecording) {
            if (p0 != null) {
                listX.add(DataPoint(System.currentTimeMillis().toDouble() - startTime, p0.values[0].toDouble()))
                listY.add(DataPoint(System.currentTimeMillis().toDouble() - startTime, p0.values[1].toDouble()))
                listZ.add(DataPoint(System.currentTimeMillis().toDouble() - startTime, p0.values[2].toDouble()))
            }
        }
    }
}
