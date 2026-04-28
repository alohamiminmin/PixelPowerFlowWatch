package com.example.pixelpowerflowwatch.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pixelpowerflowwatch.presentation.theme.PixelPowerFlowWatchTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit test for the WearApp Composable.
 */
@RunWith(AndroidJUnit4::class)
class WearAppTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun wearApp_displaysGreetingWithProvidedName() {
        val testName = "TestUser"
        
        composeTestRule.setContent {
            PixelPowerFlowWatchTheme {
                WearApp(greetingName = testName)
            }
        }

        // Check if the greeting "Hello, TestUser!" is displayed.
        composeTestRule.onNodeWithText("Hello, $testName", substring = true).assertIsDisplayed()
    }

    @Test
    fun wearApp_displaysButtons() {
        composeTestRule.setContent {
            PixelPowerFlowWatchTheme {
                WearApp(greetingName = "Android")
            }
        }

        composeTestRule.onNodeWithText("Button A").assertIsDisplayed()
        composeTestRule.onNodeWithText("Button B").assertIsDisplayed()
        composeTestRule.onNodeWithText("Button C").assertIsDisplayed()
    }
}
