package com.example.projectgun

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context
import android.media.MediaPlayer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity(), SensorEventListener {

    lateinit var sensorManager: SensorManager
    var accelerometer: Sensor? = null
    var _sensorValues = mutableStateOf(Triple(0f, 0f, 0f))
    var fase=0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        setContent {
            UIPrincipal(sensorValues = _sensorValues.value,fase)
        }

        // Registrar el sensor
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {

            val x = it.values[0]
            val y = it.values[1]
            val z = it.values[2]
            _sensorValues.value = Triple(x, y, z)

            if ((x < 4.0 || x > -4.0) && (y < -7.0) && (z < 4.0 || z > -4.0)&&fase!=0) {
                vibrarCelular(500)

                fase=0
            }else if((x < -4.0) && (y > -9.0) && (z < 4.0 || z > -4.0)&&fase==0){
                vibrarCelular(500)
                
                fase=1
            }

        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No es obligatorio manejar esto
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun vibrarCelular(milisegundos: Long) {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(milisegundos, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(milisegundos)
        }
    }
}

@Composable
fun UIPrincipal(sensorValues: Triple<Float, Float, Float>,fase:Int) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Fase = $fase")
        Text(text = "X: ${sensorValues.first}")
        Text(text = "Y: ${sensorValues.second}")
        Text(text = "Z: ${sensorValues.third}")
    }
}

@Preview(showBackground = true)
@Composable
fun Previsualizacion() {
    UIPrincipal(sensorValues = Triple(0f, 0f, 0f),0)
}

