package com.example.indivassignment6q2

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.indivassignment6q2.ui.theme.IndivAssignment6Q2Theme
import kotlin.math.PI

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IndivAssignment6Q2Theme {
                CompassScreen()
            }
        }
    }
}

@Composable
fun CompassScreen() {
    val context = LocalContext.current

    // State for Compass
    var azimuth by remember { mutableFloatStateOf(0f) }

    // Sensor Data Holders
    var gravity by remember { mutableStateOf<FloatArray?>(null) }
    var geomagnetic by remember { mutableStateOf<FloatArray?>(null) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event ?: return
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> gravity = event.values.clone()
                    Sensor.TYPE_MAGNETIC_FIELD -> geomagnetic = event.values.clone()
                }

                if (gravity != null && geomagnetic != null) {
                    val R = FloatArray(9)
                    val I = FloatArray(9)
                    if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(R, orientation)
                        
                        // orientation[0] is Azimuth in radians
                        var az = (orientation[0] * 180 / PI).toFloat()
                        // Normalize to 0-360
                        if (az < 0) az += 360
                        azimuth = az
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        accelerometer?.let { sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI) }
        magnetometer?.let { sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI) }

        onDispose {
            sensorManager.unregisterListener(sensorListener)
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Compass",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            // Display the visual compass
            CompassView(azimuth = azimuth)
            
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "${azimuth.toInt()}° ${getDirectionLabel(azimuth)}",
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
fun CompassView(azimuth: Float) {
    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(250.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2
            
            // 1. Draw Outer Ring
            drawCircle(
                color = Color.Gray,
                style = Stroke(width = 4.dp.toPx())
            )
            
            // 2. Rotate the entire needle assembly based on azimuth
            // We rotate by -azimuth so the needle stays pointing North relative to the phone
            rotate(degrees = -azimuth, pivot = center) {
                
                // North Needle (Red)
                val northPath = Path().apply {
                    moveTo(center.x, center.y - radius + 20) // Tip
                    lineTo(center.x - 15, center.y) // Base Left
                    lineTo(center.x + 15, center.y) // Base Right
                    close()
                }
                drawPath(northPath, Color.Red)
                
                // South Needle (Blue)
                val southPath = Path().apply {
                    moveTo(center.x, center.y + radius - 20) // Tip
                    lineTo(center.x - 15, center.y) // Base Left
                    lineTo(center.x + 15, center.y) // Base Right
                    close()
                }
                drawPath(southPath, Color.Blue)
            }
            
            // 3. Draw Center Pin
            drawCircle(
                color = Color.Black,
                radius = 8.dp.toPx(),
                center = center
            )
        }
        
        // Static Cardinal Direction Labels (N, S, E, W)
        // These stay fixed relative to the UI (phone), visualizing that "Up" on the screen is the direction we are facing
        // Alternatively, for a compass, usually "N" rotates. 
        // Let's Draw fixed N marker to indicate "Up is North" if the needle was fixed?
        // No, standard digital compass: Needle points Real North. Phone Top points Heading.
        // So we draw "N" on the top of the circle, but it should rotate with the circle if we were rotating the dial.
        // For simplicity: We rotated the NEEDLE. So the needle points to true North.
    }
}

fun getDirectionLabel(azimuth: Float): String {
    return when {
        azimuth >= 337.5 || azimuth < 22.5 -> "N"
        azimuth >= 22.5 && azimuth < 67.5 -> "NE"
        azimuth >= 67.5 && azimuth < 112.5 -> "E"
        azimuth >= 112.5 && azimuth < 157.5 -> "SE"
        azimuth >= 157.5 && azimuth < 202.5 -> "S"
        azimuth >= 202.5 && azimuth < 247.5 -> "SW"
        azimuth >= 247.5 && azimuth < 292.5 -> "W"
        azimuth >= 292.5 && azimuth < 337.5 -> "NW"
        else -> ""
    }
}
