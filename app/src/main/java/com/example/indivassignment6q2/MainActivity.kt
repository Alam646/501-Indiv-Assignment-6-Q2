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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.indivassignment6q2.ui.theme.IndivAssignment6Q2Theme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IndivAssignment6Q2Theme {
                SensorRawDataScreen()
            }
        }
    }
}

@Composable
fun SensorRawDataScreen() {
    val context = LocalContext.current
    
    // State for Raw Sensor Data
    var accelValues by remember { mutableStateOf(floatArrayOf(0f, 0f, 0f)) }
    var magValues by remember { mutableStateOf(floatArrayOf(0f, 0f, 0f)) }
    var gyroValues by remember { mutableStateOf(floatArrayOf(0f, 0f, 0f)) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event ?: return
                // Update state based on sensor type
                // We use .clone() because the event.values array is reused by the system
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> accelValues = event.values.clone()
                    Sensor.TYPE_MAGNETIC_FIELD -> magValues = event.values.clone()
                    Sensor.TYPE_GYROSCOPE -> gyroValues = event.values.clone()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        // Register listeners
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
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Step 1: Raw Sensor Data",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))

            SensorValueDisplay(title = "Accelerometer", values = accelValues)
            Spacer(modifier = Modifier.height(16.dp))
            
            SensorValueDisplay(title = "Magnetometer", values = magValues)
            Spacer(modifier = Modifier.height(16.dp))
            
            SensorValueDisplay(title = "Gyroscope", values = gyroValues)
        }
    }
}

@Composable
fun SensorValueDisplay(title: String, values: FloatArray) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = "X: ${String.format(Locale.US, "%.2f", values[0])}")
        Text(text = "Y: ${String.format(Locale.US, "%.2f", values[1])}")
        Text(text = "Z: ${String.format(Locale.US, "%.2f", values[2])}")
    }
}
