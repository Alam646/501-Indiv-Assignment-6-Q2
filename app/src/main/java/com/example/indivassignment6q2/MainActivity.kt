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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.indivassignment6q2.ui.theme.IndivAssignment6Q2Theme
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IndivAssignment6Q2Theme {
                SensorDashboard()
            }
        }
    }
}

@Composable
fun SensorDashboard() {
    val context = LocalContext.current

    // --- Sensor State ---
    var azimuth by remember { mutableFloatStateOf(0f) }
    var pitch by remember { mutableFloatStateOf(0f) }
    var roll by remember { mutableFloatStateOf(0f) }
    
    var gyroX by remember { mutableFloatStateOf(0f) }
    var gyroY by remember { mutableFloatStateOf(0f) }
    var gyroZ by remember { mutableFloatStateOf(0f) }

    var gravity by remember { mutableStateOf<FloatArray?>(null) }
    var geomagnetic by remember { mutableStateOf<FloatArray?>(null) }

    // --- Sensor Logic ---
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
                        
                        val az = (orientation[0] * 180 / PI).toFloat()
                        val pt = (orientation[1] * 180 / PI).toFloat()
                        val rl = (orientation[2] * 180 / PI).toFloat()

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

        onDispose { sensorManager.unregisterListener(sensorListener) }
    }

    // --- UI Layout ---
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFF5F5F5) // Light grey background for dashboard look
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()) // Allow scrolling on small screens
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Sensor Tools",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF333333)
            )

            // 1. Compass Card
            DashboardCard(title = "Compass") {
                EnhancedCompassView(azimuth)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "${azimuth.toInt()}° ${getDirectionLabel(azimuth)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 2. Level Card (Renamed from Spirit Level)
            DashboardCard(title = "Digital Level") {
                EnhancedLevelView(pitch, roll)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DataBadge(label = "Pitch", value = "${pitch.toInt()}°")
                    DataBadge(label = "Roll", value = "${roll.toInt()}°")
                }
            }

            // 3. Gyroscope Card
            DashboardCard(title = "Gyroscope (rad/s)") {
                GyroVisualizer(gyroX, gyroY, gyroZ)
            }
        }
    }
}

@Composable
fun DashboardCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun DataBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EnhancedCompassView(azimuth: Float) {
    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(200.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2

            // Bezel Gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color(0xFFE0E0E0)),
                    center = center,
                    radius = radius
                )
            )
            // Outer rim
            drawCircle(color = Color.DarkGray, style = Stroke(width = 6.dp.toPx()))

            // Ticks
            for (i in 0 until 360 step 30) {
                // Skip drawing ticks at cardinal directions to avoid blocking N, E, S, W
                if (i % 90 == 0) continue

                val angleRad = (i - 90) * (PI / 180f).toFloat()
                val isMajor = i % 90 == 0 // This is now redundant but kept for logic consistency if we change skip logic
                val tickLength = if (isMajor) 20.dp.toPx() else 10.dp.toPx()
                val strokeWidth = if (isMajor) 3.dp.toPx() else 1.dp.toPx()
                val color = if (isMajor) Color.Black else Color.Gray

                val startX = center.x + (radius - 10.dp.toPx()) * cos(angleRad)
                val startY = center.y + (radius - 10.dp.toPx()) * sin(angleRad)
                val endX = center.x + (radius - 10.dp.toPx() - tickLength) * cos(angleRad)
                val endY = center.y + (radius - 10.dp.toPx() - tickLength) * sin(angleRad)

                drawLine(color = color, start = Offset(startX, startY), end = Offset(endX, endY), strokeWidth = strokeWidth)
            }

            // Rotate the needle wrapper
            rotate(degrees = -azimuth, pivot = center) {
                // North Needle (Red)
                val northPath = Path().apply {
                    moveTo(center.x, center.y - radius + 35.dp.toPx())
                    lineTo(center.x - 15.dp.toPx(), center.y)
                    lineTo(center.x + 15.dp.toPx(), center.y)
                    close()
                }
                drawPath(northPath, Color(0xFFD32F2F)) // Material Red

                // South Needle (Silver/Grey)
                val southPath = Path().apply {
                    moveTo(center.x, center.y + radius - 35.dp.toPx())
                    lineTo(center.x - 15.dp.toPx(), center.y)
                    lineTo(center.x + 15.dp.toPx(), center.y)
                    close()
                }
                drawPath(southPath, Color(0xFF616161)) // Dark Grey
            }

            // Center Pin
            drawCircle(color = Color.Black, radius = 6.dp.toPx())
            drawCircle(color = Color.White, radius = 2.dp.toPx())
        }

        // Overlay Directions (Added more padding to keep clear of ticks if any)
        val textPadding = 12.dp
        Text("N", modifier = Modifier.align(Alignment.TopCenter).padding(top = textPadding), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.Black)
        Text("S", modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = textPadding), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.Black)
        Text("W", modifier = Modifier.align(Alignment.CenterStart).padding(start = textPadding), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.Black)
        Text("E", modifier = Modifier.align(Alignment.CenterEnd).padding(end = textPadding), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.Black)
    }
}

@Composable
fun EnhancedLevelView(pitch: Float, roll: Float) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(180.dp)
            .clip(CircleShape)
            .background(
                // Dark "Professional Tool" Background
                Brush.radialGradient(listOf(Color(0xFF424242), Color(0xFF212121)))
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            
            // Bullseye rings (Lighter Grey for visibility)
            drawCircle(color = Color.White.copy(alpha = 0.1f), radius = size.width / 4, style = Stroke(width = 2.dp.toPx()))
            drawCircle(color = Color.White.copy(alpha = 0.3f), radius = 12.dp.toPx(), style = Stroke(width = 2.dp.toPx()))

            // Crosshairs
            drawLine(
                color = Color.White.copy(alpha = 0.2f),
                start = Offset(center.x, 0f), end = Offset(center.x, size.height),
                strokeWidth = 2f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.2f),
                start = Offset(0f, center.y), end = Offset(size.width, center.y),
                strokeWidth = 2f
            )
        }

        // The Bubble
        val maxTilt = 45f
        val normRoll = (roll / maxTilt).coerceIn(-1f, 1f)
        val normPitch = (pitch / maxTilt).coerceIn(-1f, 1f)

        Canvas(modifier = Modifier.size(180.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxOffset = size.width / 2 - 25.dp.toPx() // Keep inside

            val bubbleX = center.x + (normRoll * maxOffset)
            val bubbleY = center.y + (normPitch * maxOffset)

            // Bubble shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.5f),
                radius = 20.dp.toPx(),
                center = Offset(bubbleX + 4, bubbleY + 4)
            )
            // Bubble main - Neon Green for high contrast
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFB2FF59), Color(0xFF76FF03)), // Neon Green
                    center = Offset(bubbleX - 5, bubbleY - 5),
                    radius = 20.dp.toPx()
                ),
                radius = 20.dp.toPx(),
                center = Offset(bubbleX, bubbleY)
            )
        }
    }
}

@Composable
fun GyroVisualizer(x: Float, y: Float, z: Float) {
    Column(modifier = Modifier.fillMaxWidth()) {
        GyroBar(label = "X Axis", value = x, color = Color(0xFFEF5350))
        Spacer(modifier = Modifier.height(8.dp))
        GyroBar(label = "Y Axis", value = y, color = Color(0xFF66BB6A))
        Spacer(modifier = Modifier.height(8.dp))
        GyroBar(label = "Z Axis", value = z, color = Color(0xFF42A5F5))
    }
}

@Composable
fun GyroBar(label: String, value: Float, color: Color) {
    // Visualizer: A bar that grows from center left or right
    val maxRange = 5f // Assume max 5 rad/s for visual scale
    val clampedValue = value.coerceIn(-maxRange, maxRange)
    val normalized = clampedValue / maxRange // -1 to 1

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Text(String.format(Locale.US, "%.2f", value), style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(Color(0xFFEEEEEE), RoundedCornerShape(6.dp))
                .clip(RoundedCornerShape(6.dp))
        ) {
            // Center marker
            Box(modifier = Modifier.align(Alignment.Center).width(2.dp).height(12.dp).background(Color.Gray))
            
            Canvas(modifier = Modifier.fillMaxSize()) {
                val midX = size.width / 2
                val barWidth = (size.width / 2) * abs(normalized)
                
                val startX = if (normalized >= 0) midX else midX - barWidth
                
                drawRect(
                    color = color,
                    topLeft = Offset(startX, 0f),
                    size = androidx.compose.ui.geometry.Size(barWidth, size.height)
                )
            }
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
