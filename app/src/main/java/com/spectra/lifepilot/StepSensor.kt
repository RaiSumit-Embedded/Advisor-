package com.spectra.lifepilot

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/** Reads the phone's TYPE_STEP_COUNTER (cumulative steps since last boot). */
class StepSensor(context: Context) : SensorEventListener {
    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    val available: Boolean get() = sensor != null
    var onSteps: ((Long) -> Unit)? = null

    fun start() { sensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) } }
    fun stop() { sm.unregisterListener(this) }

    override fun onSensorChanged(e: SensorEvent) { onSteps?.invoke(e.values[0].toLong()) }
    override fun onAccuracyChanged(s: Sensor?, a: Int) {}
}
