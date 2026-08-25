package it.letscode.ringcontroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

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
