package it.letscode.ringcontroller

import org.junit.Assert.assertEquals
import org.junit.Test

class VehicleActivationActionTest {
    @Test
    fun forceWhiteHasStableDisplayName() {
        assertEquals("Force bright white", VehicleActivationAction.ForceWhite.displayName)
    }
}

