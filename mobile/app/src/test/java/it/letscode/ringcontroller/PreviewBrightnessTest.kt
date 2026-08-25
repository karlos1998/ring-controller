package it.letscode.ringcontroller

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewBrightnessTest {
    @Test
    fun lowPhysicalBrightnessRemainsLegibleInThePreview() {
        val previewLevel = perceptualPreviewBrightness(0.30f)

        assertTrue(previewLevel > 0.55f)
        assertTrue(previewLevel < 0.70f)
    }

    @Test
    fun previewCurveKeepsTrueOffAndFullBrightnessEndpoints() {
        assertEquals(0f, perceptualPreviewBrightness(0f), 0.0001f)
        assertEquals(1f, perceptualPreviewBrightness(1f), 0.0001f)
    }

    @Test
    fun emittedPreviewColorPreservesItsHue() {
        val emission = Color(0xFF00E5E5).asPreviewEmission(0.30f)

        assertEquals(0f, emission.red, 0.0001f)
        assertEquals(emission.green, emission.blue, 0.0001f)
        assertTrue(emission.green > 0.50f)
    }
}
