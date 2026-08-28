package it.letscode.ringcontroller

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BleProtocolTest {
    @Test
    fun parsesCompleteControllerState() {
        val state = BleProtocol.parseState(
            "STATE|1.0|0.2.0|1|224|2|0|1|1|FF0000,00FF00,0000FF,FFFFFF|F2F6FF,FF6A00",
        )

        assertNotNull(state)
        assertEquals("0.2.0", state!!.firmwareVersion)
        assertTrue(state.enabled)
        assertEquals(224, state.brightness)
        assertEquals(2, state.scene)
        assertFalse(state.vehicleOverrideActive)
        assertTrue(state.vehicleSignalActive)
        assertTrue(state.vehicleAutomationEnabled)
        assertEquals(listOf("#FF0000", "#00FF00", "#0000FF", "#FFFFFF"), state.ringColors.map { it.toHex() })
        assertEquals(listOf("#F2F6FF", "#FF6A00"), state.favorites.map { it.toHex() })
        assertEquals(null, state.customSceneSlot)
        assertFalse(state.daylightSignalActive)
        assertFalse(state.daylightAutomationEnabled)
        assertEquals(50, state.daylightBrightnessPercent)
    }

    @Test
    fun encodesPerRingAndGlobalColorCommands() {
        val red = "#FF304E".parseHexColor()!!

        assertEquals("COLOR|2|FF304E", BleProtocol.color(2, red))
        assertEquals("COLOR|255|FF304E", BleProtocol.color(null, red))
    }

    @Test
    fun encodesTheExtendedSceneCatalogWithoutChangingStopSyntax() {
        assertEquals("SCENE|0", BleProtocol.scene(0))
        assertEquals("SCENE|19", BleProtocol.scene(19))
        assertEquals("SCENE|-1", BleProtocol.scene(null))
    }

    @Test
    fun parsesAndEncodesCustomSceneCommands() {
        val state = BleProtocol.parseState(
            "STATE|1.1|0.4.0|1|224|-1|0|0|1|FF0000,FF0000,FF0000,FF0000|FF0000|3",
        )
        val scene = CustomScene(
            slot = 3,
            name = "Hazard",
            description = "",
            moments = listOf(
                CustomSceneMoment(List(4) { "#FF6A00".parseHexColor()!! }, 450, CustomTransition.Jump),
                CustomSceneMoment(List(4) { Color.Black }, 550, CustomTransition.Smooth),
            ),
        )

        assertEquals(3, state?.customSceneSlot)
        assertEquals(
            listOf(
                "CUSTOM_BEGIN|3|2",
                "CUSTOM_STEP|3|0|450|0|FF6A00,FF6A00,FF6A00,FF6A00",
                "CUSTOM_STEP|3|1|550|1|000000,000000,000000,000000",
                "CUSTOM_COMMIT|3",
                "CUSTOM_PLAY|3",
            ),
            BleProtocol.customSceneUpload(scene, playAfterUpload = true),
        )
        assertEquals("CUSTOM_DELETE|3", BleProtocol.customSceneDelete(3))
    }

    @Test
    fun parsesAndEncodesDaylightAutomation() {
        val state = BleProtocol.parseState(
            "STATE|1.2|0.5.0|1|128|-1|0|0|1|FFFFFF,FFFFFF,FFFFFF,FFFFFF|FFFFFF|" +
                "-1|1|1|50",
        )

        assertNotNull(state)
        assertTrue(state!!.daylightSignalActive)
        assertTrue(state.daylightAutomationEnabled)
        assertEquals(50, state.daylightBrightnessPercent)
        assertEquals("DAYLIGHT|1|50", BleProtocol.daylightAutomation(true, 50))
        assertEquals("DAYLIGHT|0|100", BleProtocol.daylightAutomation(false, 140))
    }
}
