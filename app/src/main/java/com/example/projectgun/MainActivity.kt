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
import android.hardware.camera2.CameraManager
import android.media.Image
import android.media.MediaPlayer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch




class MainActivity : ComponentActivity(), SensorEventListener {
    var backgroundMediaPlayer: MediaPlayer? = null
    lateinit var sensorManager: SensorManager
    var accelerometer: Sensor? = null
    var _sensorValues = mutableStateOf(Triple(0f, 0f, 0f))
    var yAnterior=0f
    var zAnterior=0f
    var tiempoUltimoDisparo=System.currentTimeMillis()
    var tiempoUltimaRecarga=System.currentTimeMillis()
    private var mediaPlayer: MediaPlayer? = null
    var fase by mutableStateOf(0)
    var shots by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        setContent {
            UIPrincipal(sensorValues = _sensorValues.value,fase = fase,shots = shots)
        }

        // Registrar el sensor
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        iniciarMusicaDeFondo(this, R.raw.fondo)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val tiempoActual=System.currentTimeMillis()
            val x = it.values[0]
            val y = it.values[1]
            val z = it.values[2]
            _sensorValues.value = Triple(x, y, z)
            if((x>-10||x<10)&&(y>-10||y<10)){
                if ((x < 2.0 && x > -2.0) && (y < -9.5) && (z < 4.0 && z > -4.0)&&fase==1) {
                    //Enfundar
                    reproducirSonido(this, R.raw.revolver_prepare)
                    fase=0
                    vibrarCelular(500)
                }else if((x < -2.0) && (y > -9.5) && (z < 4.0 && z > -4.0)&&fase!=1&&fase!=2){
                    //Apuntar
                    //reproducirSonido(this,R.raw.spining)
                    fase=1
                    vibrarCelular(500)
                }else if(fase==1 &&(y-yAnterior>2&&(tiempoActual-tiempoUltimoDisparo>500))
                    &&(y>2)&&(z < 8.0 && z > -8.0)){
                    //Disparar
                    controlFlash(true)
                    CoroutineScope(Dispatchers.Main).launch{
                        delay(300)
                        controlFlash(false)
                    }
                    if(shots == 6){
                        //Sin balas
                        reproducirSonido(this, R.raw.no_bullets)
                    }else{
                        //Con balas
                        reproducirSonido(this, R.raw.revolver_shoot)
                        shots++
                    }

                    vibrarCelular(100)
                    tiempoUltimoDisparo= tiempoActual
                    fase=2
                }else if (fase == 2 && y > -9.5) {
                    //Apuntado despues del disparo
                    fase = 1
                    //}else if(fase==1&&(z<-15)){
                }else if(fase==1 &&(z-zAnterior>1&&(tiempoActual-tiempoUltimaRecarga>500))
                        &&(z>8)&&(y < 4.0 && y > -4.0)&&(tiempoActual-tiempoUltimoDisparo>500)&&
                (x>-8)){
                    //Spining
                    shots=0
                    reproducirSonido(this,R.raw.spining)
                    vibrarCelular(100)
                    tiempoUltimaRecarga= tiempoActual
                }
                yAnterior=y
                zAnterior=z

            }
        }

    }


    fun reproducirSonido(context: Context, sonidoResId: Int) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(context, sonidoResId)
        mediaPlayer?.start()
    }

    private fun iniciarMusicaDeFondo(context: Context, musicaResId: Int) {
        backgroundMediaPlayer = MediaPlayer.create(context, musicaResId).apply {
            isLooping = true
            start()
        }
    }

    private fun detenerMusicaDeFondo() {
        backgroundMediaPlayer?.release()
        backgroundMediaPlayer = null
    }




    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No es obligatorio manejar esto
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        detenerMusicaDeFondo()
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        iniciarMusicaDeFondo(this, R.raw.fondo)
    }

    override fun onDestroy() {
        super.onDestroy()
        detenerMusicaDeFondo()
    }


    fun vibrarCelular(milisegundos: Long) {
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

    fun controlFlash(state:Boolean){
        val cameraManager=getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList[0]
        cameraManager.setTorchMode(cameraId,state)
    }
}

@Composable
fun UIPrincipal(sensorValues: Triple<Float, Float, Float>,fase:Int,shots:Int) {
    val currentImage = when (fase) {
        0 -> R.drawable.fundada
        1 -> R.drawable.normal
        2 -> R.drawable.disparando
        3 -> R.drawable.recarga
        else -> R.drawable.normal
    }
    Image(
        painter = painterResource(id = currentImage),
        contentDescription = null,
        modifier = Modifier.fillMaxWidth().fillMaxHeight().graphicsLayer(
            rotationZ = 270f
        ),
    )

    /*
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(text = "Fase = $fase")
        Text(text = "Shots = $shots")
        Text(text = "X: ${sensorValues.first}")
        Text(text = "Y: ${sensorValues.second}")
        Text(text = "Z: ${sensorValues.third}")
    } */
}

@Preview(showBackground = true)
@Composable
fun Previsualizacion() {
    UIPrincipal(sensorValues = Triple(0f, 0f, 0f),0,6)
}

