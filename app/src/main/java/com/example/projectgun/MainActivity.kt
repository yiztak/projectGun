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
    var yAnterior=0f
    var tiempoUltimoDisparo=System.currentTimeMillis()

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
            val tiempoActual=System.currentTimeMillis()
            val x = it.values[0]
            val y = it.values[1]
            val z = it.values[2]
            _sensorValues.value = Triple(x, y, z)
            if((x>-10||x<10)&&(y>-10||y<10)&&(z>-10||z<10)){
                if ((x < 2.0 && x > -2.0) && (y < -9.5) && (z < 4.0 && z > -4.0)&&fase==1) {
                    vibrarCelular(500)

                    fase=0
                }else if((x < -2.0) && (y > -9.5) && (z < 4.0 && z > -4.0)&&fase!=1&&fase!=2){
                    vibrarCelular(500)
                    fase=1

                }else if(fase==1 &&(y-yAnterior>1&&(tiempoActual-tiempoUltimoDisparo>500))&&(y>2)&&fase==1){
                    vibrarCelular(100)
                    tiempoUltimoDisparo= tiempoActual
                    reproducirSonido(this, R.raw.revolver_shoot)

                    fase=2
                }else if (fase == 2 && y > -9.5) {
                    fase = 1
                }
                yAnterior=y

            }
        }

    }

    private var mediaPlayer: MediaPlayer? = null

    fun reproducirSonido(context: Context, sonidoResId: Int) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(context, sonidoResId)
        mediaPlayer?.start()
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

