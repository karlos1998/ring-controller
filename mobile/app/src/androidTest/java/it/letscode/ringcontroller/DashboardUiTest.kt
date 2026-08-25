package it.letscode.ringcontroller

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class DashboardUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun appliesFavoriteToAnIndividuallySelectedRing() {
        composeRule.setContent { RingControllerApp() }

        composeRule.onNodeWithContentDescription("Ring 1").performClick()
        composeRule.onNodeWithContentDescription("Apply Blue").performClick()

        composeRule.onNodeWithText("RING 1").assertExists()
        composeRule.onNodeWithContentDescription("Apply Blue").assertIsSelected()
    }
}
