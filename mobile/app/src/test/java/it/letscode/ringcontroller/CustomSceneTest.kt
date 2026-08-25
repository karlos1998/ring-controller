package it.letscode.ringcontroller

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomSceneTest {
    private val red = Color(0xFFFF0000)
    private val blue = Color(0xFF0000FF)

    @Test
    fun smoothMomentBlendsTowardTheNextMoment() {
        val scene = scene(
            CustomSceneMoment(List(4) { red }, 1_000, CustomTransition.Smooth),
            CustomSceneMoment(List(4) { blue }, 1_000, CustomTransition.Smooth),
        )

        val halfway = colorsForCustomScene(scene, 0.5f).first()

        assertTrue(halfway.red in 0.45f..0.55f)
        assertTrue(halfway.blue in 0.45f..0.55f)
    }

    @Test
    fun jumpMomentHoldsItsColorsUntilItsDurationEnds() {
        val scene = scene(
            CustomSceneMoment(List(4) { red }, 500, CustomTransition.Jump),
            CustomSceneMoment(List(4) { blue }, 500, CustomTransition.Jump),
        )

        assertEquals("#FF0000", colorsForCustomScene(scene, 0.49f).first().toHex())
        assertEquals("#0000FF", colorsForCustomScene(scene, 0.50f).first().toHex())
        assertEquals("#FF0000", colorsForCustomScene(scene, 1.00f).first().toHex())
    }

    @Test
    fun findsTheFirstFreeControllerSlot() {
        val scenes = listOf(scene(slot = 0), scene(slot = 2), scene(slot = 1))

        assertEquals(3, nextCustomSceneSlot(scenes))
    }

    private fun scene(
        vararg moments: CustomSceneMoment,
        slot: Int = 0,
    ): CustomScene = CustomScene(
        slot = slot,
        name = "Test",
        description = "",
        moments = moments.toList().ifEmpty {
            listOf(
                CustomSceneMoment(List(4) { red }, 500, CustomTransition.Jump),
                CustomSceneMoment(List(4) { blue }, 500, CustomTransition.Jump),
            )
        },
    )
}
