package it.letscode.ringcontroller

import androidx.compose.ui.graphics.Color
import java.util.UUID

internal data class ControllerSnapshot(
    val protocolVersion: String,
    val firmwareVersion: String,
    val enabled: Boolean,
    val brightness: Int,
    val scene: Int?,
    val vehicleOverrideActive: Boolean,
    val vehicleSignalActive: Boolean,
    val vehicleAutomationEnabled: Boolean,
    val ringColors: List<Color>,
    val favorites: List<Color>,
    val customSceneSlot: Int?,
)

internal object BleProtocol {
    const val DEVICE_NAME = "D4WID-Ring"
    val SERVICE_UUID: UUID = UUID.fromString("7d2f0001-9c5a-4f28-b4d7-4b3a6d9a0001")
    val COMMAND_UUID: UUID = UUID.fromString("7d2f0002-9c5a-4f28-b4d7-4b3a6d9a0001")
    val STATE_UUID: UUID = UUID.fromString("7d2f0003-9c5a-4f28-b4d7-4b3a6d9a0001")
    val INFO_UUID: UUID = UUID.fromString("7d2f0004-9c5a-4f28-b4d7-4b3a6d9a0001")

    fun parseState(raw: String): ControllerSnapshot? {
        val parts = raw.trim().split('|')
        if (parts.size < 11 || parts[0] != "STATE") return null
        val colors = parts[9].split(',').mapNotNull(String::parseHexColor)
        if (colors.size != 4) return null
        val favorites = parts[10]
            .takeIf(String::isNotBlank)
            ?.split(',')
            ?.mapNotNull(String::parseHexColor)
            .orEmpty()
        return ControllerSnapshot(
            protocolVersion = parts[1],
            firmwareVersion = parts[2],
            enabled = parts[3] == "1",
            brightness = parts[4].toIntOrNull()?.coerceIn(0, 255) ?: return null,
            scene = parts[5].toIntOrNull()?.takeIf { it >= 0 },
            vehicleOverrideActive = parts[6] == "1",
            vehicleSignalActive = parts[7] == "1",
            vehicleAutomationEnabled = parts[8] == "1",
            ringColors = colors,
            favorites = favorites.take(12),
            customSceneSlot = parts.getOrNull(11)?.toIntOrNull()?.takeIf { it in 0 until MAX_CUSTOM_SCENES },
        )
    }

    fun power(enabled: Boolean): String = "POWER|${if (enabled) 1 else 0}"

    fun brightness(value: Float): String = "BRIGHTNESS|${(value * 255f).toInt().coerceIn(0, 255)}"

    fun color(target: Int?, color: Color): String = "COLOR|${target ?: 255}|${color.toHex().drop(1)}"

    fun scene(index: Int?): String = "SCENE|${index ?: -1}"

    fun favorites(colors: List<Color>): String =
        "FAVORITES|${colors.take(12).joinToString(",") { it.toHex().drop(1) }}"

    fun vehicleAutomation(enabled: Boolean): String = "VEHICLE|${if (enabled) 1 else 0}"

    fun customSceneUpload(scene: CustomScene, playAfterUpload: Boolean): List<String> {
        val normalized = scene.normalized()
        return buildList {
            add("CUSTOM_BEGIN|${normalized.slot}|${normalized.moments.size}")
            normalized.moments.forEachIndexed { index, moment ->
                val colors = moment.colors.joinToString(",") { it.toHex().drop(1) }
                add(
                    "CUSTOM_STEP|${normalized.slot}|$index|${moment.durationMs}|" +
                        "${moment.transition.protocolValue}|$colors",
                )
            }
            add("CUSTOM_COMMIT|${normalized.slot}")
            if (playAfterUpload) add("CUSTOM_PLAY|${normalized.slot}")
        }
    }

    fun customSceneDelete(slot: Int): String = "CUSTOM_DELETE|${slot.coerceIn(0, MAX_CUSTOM_SCENES - 1)}"
}
