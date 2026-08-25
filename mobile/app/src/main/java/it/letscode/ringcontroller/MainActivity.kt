package it.letscode.ringcontroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { RingControllerApp() }
    }
}

enum class VehicleActivationAction(val displayName: String) {
    Ignore("Do nothing"),
    ForceWhite("Force bright white"),
    TurnOnLast("Turn on last color"),
    ApplyPreset("Apply preset"),
    TurnOff("Turn rings off"),
}

data class RingUiState(
    val number: Int,
    val color: Color,
    val brightnessPercent: Int,
)

private val AppColors = darkColorScheme(
    primary = Color(0xFFFF6B22),
    secondary = Color(0xFFFFB36B),
    background = Color(0xFF111318),
    surface = Color(0xFF1A1D23),
    onPrimary = Color.Black,
    onBackground = Color(0xFFF2F2F2),
    onSurface = Color(0xFFF2F2F2),
)

@Composable
fun RingControllerApp() {
    var ringsEnabled by remember { mutableStateOf(true) }
    var vehicleAutomationEnabled by remember { mutableStateOf(true) }
    val rings = remember {
        listOf(
            RingUiState(1, Color(0xFFFF5A36), 100),
            RingUiState(2, Color(0xFFFFB000), 100),
            RingUiState(3, Color(0xFF32D583), 100),
            RingUiState(4, Color(0xFF4D9CFF), 100),
        )
    }

    MaterialTheme(colorScheme = AppColors) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Text(
                        text = "D4WID Ring",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "Controller offline · BLE integration next",
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.height(8.dp))
                }

                item {
                    SettingCard(
                        title = "Rings enabled",
                        description = "The physical button and ESP32 remain authoritative.",
                        checked = ringsEnabled,
                        onCheckedChange = { ringsEnabled = it },
                    )
                }

                items(rings) { ring -> RingCard(ring) }

                item {
                    SettingCard(
                        title = "Vehicle signal automation",
                        description = "On +12 V: ${VehicleActivationAction.ForceWhite.displayName}",
                        checked = vehicleAutomationEnabled,
                        onCheckedChange = { vehicleAutomationEnabled = it },
                    )
                }

                item {
                    Text(
                        text = "This first build establishes the Android package, release pipeline, and target configuration. Controls are local UI placeholders until the BLE protocol is implemented.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFAEB4BE),
                    )
                }
            }
        }
    }
}

@Composable
private fun RingCard(ring: RingUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(ring.color, CircleShape),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Ring ${ring.number}", fontWeight = FontWeight.Bold)
                Text(
                    "Independent RGB · ${ring.brightnessPercent}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFAEB4BE),
                )
            }
        }
    }
}

@Composable
private fun SettingCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFAEB4BE),
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RingControllerPreview() {
    RingControllerApp()
}

