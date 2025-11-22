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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.indivassignment6q2.ui.theme.IndivAssignment6Q2Theme
import java.util.Locale
import kotlin.math.PI

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IndivAssignment6Q2Theme {
                CompassAndLevelScreen()
            }
        }
    }
}

@Composable
fun CompassAndLevelScreen() {
    val context = LocalContext.current

    // State for Orientation
    var azimuth by remember { mutableFloatStateOf(0f) }
    var pitch by remember { mutableFloatStateOf(0f) }
    var roll by remember { mutableFloatStateOf(0f) }
    
    // State for Gyroscope
    var gyroX by remember { mutableFloatStateOf(0f) }
    var gyroY by remember { mutableFloatStateOf(0f) }
    var gyroZ by remember { mutableFloatStateOf(0f) }

    // Sensor Data Holders
    var gravity by remember { mutableStateOf<FloatArray?>(null) }
    var geomagnetic by remember { mutableStateOf<FloatArray?>(null) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event ?: return
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> gravity = event.values.clone()
                    Sensor.TYPE_MAGNETIC_FIELD -> geomagnetic = event.values.clone()
                    Sensor.TYPE_GYROSCOPE -> {
                        gyroX = event.values[0]
                        gyroY = event.values[1]
                        gyroZ = event.values[2]
                    }
                }

                if (gravity != null && geomagnetic != null) {
                    val R = FloatArray(9)
                    val I = FloatArray(9)
                    if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(R, orientation)
                        
                        // orientation[0] = Azimuth, [1] = Pitch, [2] = Roll (radians)
                        val az = (orientation[0] * 180 / PI).toFloat()
                        val pt = (orientation[1] * 180 / PI).toFloat()
                        val rl = (orientation[2] * 180 / PI).toFloat()

                        // Normalize Azimuth
                        azimuth = if (az < 0) az + 360 else az
                        pitch = pt
                        roll = rl
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        accelerometer?.let { sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI) }
        magnetometer?.let { sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI) }
        gyroscope?.let { sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI) }

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
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Compass Section
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Compass", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                CompassView(azimuth = azimuth)
                Spacer(modifier = Modifier.height(8.dp))
                Text("${azimuth.toInt()}° ${getDirectionLabel(azimuth)}")
            }

            // Level Section
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Digital Level", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                LevelView(pitch = pitch, roll = roll)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Pitch: ${pitch.toInt()}°  Roll: ${roll.toInt()}°")
            }
            
            // Step 4: Gyroscope Section
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                 Text("Rotation Speed (Gyro)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                 Text("X: ${String.format(Locale.US, "%.2f", gyroX)} rad/s")
                 Text("Y: ${String.format(Locale.US, "%.2f", gyroY)} rad/s")
                 Text("Z: ${String.format(Locale.US, "%.2f", gyroZ)} rad/s")
            }
        }
    }
}

@Composable
fun CompassView(azimuth: Float) {
    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(150.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2
            
            // Outer Ring
            drawCircle(color = Color.Gray, style = Stroke(width = 4.dp.toPx()))
            
            // Rotating Needle
            rotate(degrees = -azimuth, pivot = center) {
                val northPath = Path().apply {
                    moveTo(center.x, center.y - radius + 10)
                    lineTo(center.x - 10, center.y)
                    lineTo(center.x + 10, center.y)
                    close()
                }
                drawPath(northPath, Color.Red)
                
                val southPath = Path().apply {
                    moveTo(center.x, center.y + radius - 10)
                    lineTo(center.x - 10, center.y)
                    lineTo(center.x + 10, center.y)
                    close()
                }
                drawPath(southPath, Color.Blue)
            }
        }
        Text("N", modifier = Modifier.align(Alignment.TopCenter).padding(top = 2.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun LevelView(pitch: Float, roll: Float) {
    // A bubble level: The "bubble" moves opposite to the tilt.
    // If you tilt left (roll negative), bubble moves right.
    // Pitch up (positive), bubble moves down.
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(150.dp)
            .clip(CircleShape)
            .background(Color(0xFFDAF5DA)) // Light Green background
    ) {
        // Crosshairs
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                color = Color.Black.copy(alpha = 0.3f),
                start = Offset(size.width / 2, 0f),
                end = Offset(size.width / 2, size.height),
                strokeWidth = 2f
            )
            drawLine(
                color = Color.Black.copy(alpha = 0.3f),
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 2f
            )
            // Center target circle
            drawCircle(
                color = Color.Black.copy(alpha = 0.3f),
                radius = 10.dp.toPx(),
                style = Stroke(width = 2f)
            )
        }

        // The Bubble
        // Max tilt to display (e.g., 45 degrees hits the edge)
        val maxTilt = 45f
        
        // Calculate displacement
        // Roll controls X axis, Pitch controls Y axis
        // We clamp values so the bubble stays inside
        val normRoll = (roll / maxTilt).coerceIn(-1f, 1f)
        val normPitch = (pitch / maxTilt).coerceIn(-1f, 1f)
        
        // Map -1..1 to pixel offset.
        // Width is 150dp. Max offset is approx 75dp - bubble radius.
        
        Canvas(modifier = Modifier.size(150.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxOffset = size.width / 2 - 20.dp.toPx()
            
            val bubbleX = center.x + (normRoll * maxOffset)
            val bubbleY = center.y + (normPitch * maxOffset)
            
            drawCircle(
                color = Color(0xFF32CD32), // Darker Green Bubble
                radius = 15.dp.toPx(),
                center = Offset(bubbleX, bubbleY)
            )
        }
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
