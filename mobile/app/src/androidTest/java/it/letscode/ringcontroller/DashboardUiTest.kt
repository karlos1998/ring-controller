package it.letscode.ringcontroller

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test

class DashboardUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun appliesFavoriteToAnIndividuallySelectedRing() {
        composeRule.setContent { RingControllerApp() }

        composeRule.onNodeWithContentDescription(
            "Front view of a 2013 Dodge Challenger with four interactive halo rings",
        ).assertExists()
        composeRule.onNodeWithContentDescription("Ring 1").performClick()
        composeRule.onNodeWithText("FAVORITES").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Apply #FF304E").performClick()

        composeRule.onNodeWithText("RING 1").assertExists()
        composeRule.onNodeWithContentDescription("Apply #FF304E").assertIsSelected()
    }

    @Test
    fun navigationSeparatesScenesAndConfiguration() {
        composeRule.setContent { RingControllerApp() }

        composeRule.onNodeWithContentDescription("Open scenes").performClick()
        composeRule.onNodeWithText("Show modes").assertExists()
        composeRule.onNodeWithText("Amber chase").assertExists()

        composeRule.onNodeWithContentDescription("Open configuration").performClick()
        composeRule.onNodeWithText("Inputs & automation").assertExists()
        composeRule.onNodeWithText("Cabin button").assertExists()
        composeRule.onNodeWithText("Vehicle light signal").assertExists()
    }
}
