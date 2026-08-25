package it.letscode.ringcontroller

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
}
