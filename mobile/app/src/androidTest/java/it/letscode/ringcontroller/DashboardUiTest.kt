package it.letscode.ringcontroller

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
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
        composeRule.onNodeWithText(context.getString(R.string.favorites_tab).uppercase()).performScrollTo().performClick()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.apply_color_description, favoriteHex),
        ).performClick()

        composeRule.onNodeWithText(context.getString(R.string.ring_label, 1).uppercase()).assertExists()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.apply_color_description, favoriteHex),
        ).assertIsSelected()
    }

    @Test
    fun navigationSeparatesScenesAndConfiguration() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.setContent { RingControllerApp() }

        composeRule.onNodeWithContentDescription(context.getString(R.string.nav_scenes_description)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.show_modes)).assertExists()
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
}
