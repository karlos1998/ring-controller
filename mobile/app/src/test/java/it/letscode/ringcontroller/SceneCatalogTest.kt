package it.letscode.ringcontroller

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneCatalogTest {
    private val favorites = listOf(Color.Red, Color.Blue, Color.Green)

    @Test
    fun exposesTwentyStableSceneIdsWithoutGaps() {
        assertEquals((0..19).toList(), ScenePreset.entries.map { it.id }.sorted())
        assertEquals(ScenePreset.AmberChase, ScenePreset.fromId(0))
        assertEquals(ScenePreset.DemonPulse, ScenePreset.fromId(1))
        assertEquals(ScenePreset.SpectrumWave, ScenePreset.fromId(2))
        assertEquals(null, ScenePreset.fromId(20))
    }

    @Test
    fun everySceneProducesFourValidRgbColorsAcrossTime() {
        val samples = listOf(0f, 0.1f, 0.5f, 1.3f, 2.7f, 8.4f)

        ScenePreset.entries.forEach { scene ->
            samples.forEach { seconds ->
                val colors = colorsForScene(scene, seconds, favorites)
                assertEquals("${scene.name} ring count", 4, colors.size)
                colors.forEach { color ->
                    assertTrue("${scene.name} red", color.red in 0f..1f)
                    assertTrue("${scene.name} green", color.green in 0f..1f)
                    assertTrue("${scene.name} blue", color.blue in 0f..1f)
                }
            }
        }
    }

    @Test
    fun signalScenesUseDeliberateRingGroups() {
        val hazardOn = colorsForScene(ScenePreset.HazardFlash, 0.1f, favorites)
        val hazardOff = colorsForScene(ScenePreset.HazardFlash, 0.7f, favorites)
        assertTrue(hazardOn.all(Color::isLit))
        assertTrue(hazardOff.none(Color::isLit))

        val outer = colorsForScene(ScenePreset.InnerOuterAmber, 0.1f, favorites)
        val inner = colorsForScene(ScenePreset.InnerOuterAmber, 0.6f, favorites)
        assertEquals(listOf(true, false, false, true), outer.map(Color::isLit))
        assertEquals(listOf(false, true, true, false), inner.map(Color::isLit))

        val left = colorsForScene(ScenePreset.LeftAmber, 0.1f, favorites)
        val right = colorsForScene(ScenePreset.RightAmber, 0.1f, favorites)
        assertEquals(listOf(true, true, false, false), left.map(Color::isLit))
        assertEquals(listOf(false, false, true, true), right.map(Color::isLit))
    }

    @Test
    fun favoriteCarouselStartsFromAndTransitionsBetweenSavedColors() {
        val first = colorsForScene(ScenePreset.FavoriteCarousel, 0f, favorites)
        val second = colorsForScene(ScenePreset.FavoriteCarousel, 2.6f, favorites)

        assertTrue(first.all { it.toHex() == Color.Red.toHex() })
        assertTrue(second.all { it.toHex() == Color.Blue.toHex() })
    }
}

private fun Color.isLit(): Boolean = red + green + blue > 0.01f
