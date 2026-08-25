package it.letscode.ringcontroller

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DashboardUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun appliesFavoriteToAnIndividuallySelectedRing() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val favoriteHex = "#FF304E"
        composeRule.setContent { RingControllerApp() }

        composeRule.onNodeWithContentDescription(
            context.getString(R.string.challenger_preview_description),
        ).assertExists()
        composeRule.onNodeWithContentDescription(context.getString(R.string.ring_description, 1)).performClick()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.open_color_editor_description),
        ).performScrollTo().performClick()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.brightness_slider_description),
        ).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.favorites_tab).uppercase()).performClick()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.apply_color_description, favoriteHex),
        ).performClick()
        composeRule.onNodeWithText(context.getString(R.string.save_action)).performClick()

        composeRule.onNodeWithText(context.getString(R.string.ring_label, 1).uppercase()).assertExists()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.open_color_editor_description),
        ).performClick()
        composeRule.onNodeWithText(context.getString(R.string.favorites_tab).uppercase()).performClick()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.apply_color_description, favoriteHex),
        ).assertIsSelected()
    }

    @Test
    fun cancelingColorEditorDiscardsTheDraftColor() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val originalHex = "#00E5E5"
        val draftHex = "#FF304E"
        composeRule.setContent { RingControllerApp() }

        composeRule.onNodeWithContentDescription(
            context.getString(R.string.open_color_editor_description),
        ).performScrollTo().performClick()
        composeRule.onNodeWithText(context.getString(R.string.favorites_tab).uppercase()).performClick()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.apply_color_description, draftHex),
        ).performClick()
        composeRule.onNodeWithText(context.getString(R.string.cancel_action)).performClick()

        composeRule.onNodeWithContentDescription(
            context.getString(R.string.open_color_editor_description),
        ).performClick()
        composeRule.onNodeWithText(context.getString(R.string.favorites_tab).uppercase()).performClick()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.apply_color_description, originalHex),
        ).assertIsSelected()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.apply_color_description, draftHex),
        ).assertIsNotSelected()
    }

    @Test
    fun navigationSeparatesScenesAndConfiguration() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.setContent { RingControllerApp() }

        composeRule.onNodeWithContentDescription(context.getString(R.string.nav_scenes_description)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.show_modes)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.scene_category_signals).uppercase()).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.scene_amber_chase)).assertExists()

        composeRule.onNodeWithContentDescription(context.getString(R.string.nav_config_description)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.input_rules)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.cabin_button)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.vehicle_light_signal)).assertExists()
    }

    @Test
    fun switchesToInteractiveSimplifiedPreviewFromConfiguration() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.setContent { RingControllerApp() }

        composeRule.onNodeWithContentDescription(context.getString(R.string.nav_config_description)).performClick()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.simplified_preview_toggle_description),
        ).performScrollTo().performClick()
        composeRule.onNodeWithContentDescription(context.getString(R.string.nav_drive_description)).performClick()

        composeRule.onNodeWithContentDescription(
            context.getString(R.string.simplified_preview_description),
        ).assertExists()
        composeRule.onNodeWithContentDescription(context.getString(R.string.ring_description, 3)).performClick()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.ring_description, 3),
        ).assertIsSelected()
    }

    @Test
    fun opensTheMomentBasedCustomSceneStudioAboveBuiltInScenes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.setContent { RingControllerApp() }

        composeRule.onNodeWithContentDescription(context.getString(R.string.nav_scenes_description)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.your_scenes).uppercase()).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.create_first_scene)).performClick()

        composeRule.onNodeWithText(context.getString(R.string.create_scene_title)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.scene_timeline_title)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.moment_number, 1)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.moment_number, 2)).assertExists()
    }

    @Test
    fun opensDeleteConfirmationForACustomScene() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scene = testCustomScene()
        composeRule.setContent { RingControllerApp(initialCustomScenes = listOf(scene)) }

        composeRule.onNodeWithContentDescription(context.getString(R.string.nav_scenes_description)).performClick()
        composeRule.onNodeWithText(scene.name).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.delete_action)).performScrollTo().performClick()

        composeRule.onNodeWithText(context.getString(R.string.delete_scene_title)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.delete_scene_message, scene.name)).assertExists()
    }

    @Test
    fun customSceneMetadataAndTimelineRoundTripThroughLocalStorage() {
        val original = testCustomScene()

        val restored = CustomSceneJson.decode(CustomSceneJson.encode(listOf(original))).single()

        assertEquals(original.slot, restored.slot)
        assertEquals(original.name, restored.name)
        assertEquals(original.description, restored.description)
        assertEquals(original.moments.map { it.durationMs }, restored.moments.map { it.durationMs })
        assertEquals(original.moments.map { it.transition }, restored.moments.map { it.transition })
        assertEquals(
            original.moments.map { moment -> moment.colors.map(Color::toHex) },
            restored.moments.map { moment -> moment.colors.map(Color::toHex) },
        )
    }

    private fun testCustomScene(): CustomScene = CustomScene(
        slot = 2,
        name = "Orange heartbeat",
        description = "Outer and inner rings trade a warm pulse",
        moments = listOf(
            CustomSceneMoment(List(4) { Color(0xFFFF6A00) }, 450, CustomTransition.Smooth),
            CustomSceneMoment(List(4) { Color.Black }, 700, CustomTransition.Jump),
        ),
    )
}
