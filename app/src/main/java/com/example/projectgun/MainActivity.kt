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
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.projectgun.ui.theme.WesternColor
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
    companion object {
        var onMenu by mutableStateOf(true)
        var shots by mutableStateOf(0)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        setContent {
            UIPrincipal(fase = fase,shots = shots)
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
            if((x>-10||x<10)&&(y>-10||y<10) && !onMenu){
                if ((x < 2.0 && x > -2.0) && (y < -9.5) && (z < 4.0 && z > -4.0)&&fase==1) {
                    //Enfundar
                    reproducirSonido(this, R.raw.revolver_prepare)
                    fase=0
                    //vibrarCelular(500)
                }else if((x < -2.0 || x > 2.0) && (y > -9.5) && (z < 4.0 && z > -4.0)&&fase!=1&&fase!=2){
                    //Apuntar
                    reproducirSonido(this,R.raw.gun_drawing)
                    fase=1
                    //vibrarCelular(500)
                }else if(fase==1 &&(y-yAnterior>2&&(tiempoActual-tiempoUltimoDisparo>500))
                    &&(y>2)&&(z < 4.0 && z > -4.0)){
                    //Disparar

                    if(shots >= 6){
                        //Sin balas
                        CoroutineScope(Dispatchers.Main).launch{
                            delay(300)
                        }
                        reproducirSonido(this, R.raw.no_bullets)
                        shots++
                    }else{
                        //Con balas
                        controlFlash(true)
                        CoroutineScope(Dispatchers.Main).launch{
                            delay(300)
                            controlFlash(false)
                        }
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
fun UIPrincipal(fase:Int ,shots:Int) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "pantalla1"){
        composable("pantalla1") { Pantalla1(navController,fase, shots) }
        composable("pantalla2") { Pantalla2(navController,fase, shots) }
        composable("pantalla3") { Pantalla3(navController, fase, shots ) }
    }
}

@Composable
fun Pantalla1(navController: NavController, fase: Int, shots: Int) {
    MainActivity.onMenu = true
    MainActivity.shots = 0
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.el_bueno),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(rotationZ = 270f, scaleX = 2.2f, scaleY = 2.2f)
        )
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {

            Row {
                Button(onClick = { navController.navigate("pantalla2")}, colors = ButtonDefaults.buttonColors(
                    containerColor = WesternColor,  // Color de fondo
                    contentColor = Color.White      // Color del texto
                ) ) {
                    Text("Zurda", fontSize = 24.sp)
                }
                Spacer(Modifier.padding(15.dp))
                Button(onClick = { navController.navigate("pantalla3") },colors = ButtonDefaults.buttonColors(
                    containerColor = WesternColor,  // Color de fondo
                    contentColor = Color.White      // Color del texto
                ) ) {
                    Text("Diestra", fontSize = 24.sp)
                }
            }
        }
    }
}

@Composable
fun Pantalla2(navController: NavController, fase: Int, shots: Int) { //Zurda
    MainActivity.onMenu = false
    val currentImage = when  {
        fase == 0 -> {
            R.drawable.fundada
        }
        fase == 1 -> {
            R.drawable.normal
        }
        fase == 2 ->{
            if(shots <= 6 ) {           //This allow us to detect whether there are bullets or not
                R.drawable.disparando
            }
            else {
                R.drawable.normal
            }
        }

        fase == 3 ->{
            R.drawable.recarga
        }
        else -> {
            R.drawable.normal
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.el_bueno),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(rotationZ = 270f, scaleX = 2.2f, scaleY = 2.2f,rotationY = 540f)
        )
        Image(
            painter = painterResource(id = currentImage),
            contentDescription = null,
            contentScale = ContentScale.Fit, // Escalar la imagen para que llene la pantalla
            modifier = Modifier
                .width(250.dp)
                .height(250.dp)
                .graphicsLayer(rotationZ = 270f, scaleX = 2f, scaleY = 2f,rotationY = 540f)
            //.border(2.dp, Color.Red)
        )
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom)  {

            Spacer(Modifier.padding(10.dp))

            Button( onClick = { navController.popBackStack() },colors = ButtonDefaults.buttonColors(
                containerColor = WesternColor,  // Color de fondo
                contentColor = Color.White      // Color del texto
            ) ) {
                Text("Volver", fontSize = 24.sp)
            }
            Spacer(Modifier.padding(10.dp))
        }
    }

}

@Composable
fun Pantalla3(navController: NavController,  fase: Int, shots: Int) { //Diestro
    MainActivity.onMenu = false
    val currentImage = when  {
        fase == 0 -> {
            R.drawable.fundada
        }
        fase == 1 -> {
            R.drawable.normal
        }
        fase == 2 ->{
            if(shots <= 6 ) {           //This allow us to detect whether there are bullets or not
                R.drawable.disparando
            }
            else {
                R.drawable.normal
            }
        }

        fase == 3 ->{
            R.drawable.recarga
        }
        else -> {
            R.drawable.normal
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.el_bueno),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(rotationZ = 270f, scaleX = 2.2f, scaleY = 2.2f)
        )
        Image(
            painter = painterResource(id = currentImage),
            contentDescription = null,
            contentScale = ContentScale.Fit, // Escalar la imagen para que llene la pantalla
            modifier = Modifier
                .width(250.dp)
                .height(250.dp)
                .graphicsLayer(rotationZ = 270f, scaleX = 2f, scaleY = 2f)
            //.border(2.dp, Color.Red)
        )
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom)  {

            Spacer(Modifier.padding(10.dp))

            Button( onClick = { navController.popBackStack() },colors = ButtonDefaults.buttonColors(
                containerColor = WesternColor,  // Color de fondo
                contentColor = Color.White      // Color del texto
            ) ) {
                Text("Volver", fontSize = 24.sp)
            }
            Spacer(Modifier.padding(10.dp))
        }
    }
}


@Preview(showBackground = true)
@Composable
fun Previsualizacion() {
    UIPrincipal(0,6)
}

