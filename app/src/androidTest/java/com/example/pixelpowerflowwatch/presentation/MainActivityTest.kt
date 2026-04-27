package com.example.pixelpowerflowwatch.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for MainActivity, testing UI components using Compose Test Rule.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun greeting_isDisplayed() {
        // MainActivity calls WearApp("Android") in onCreate.
        // The greeting "Hello, Android!" should be displayed.
        composeTestRule.onNodeWithText("Hello, Android!", substring = true).assertIsDisplayed()
    }

    @Test
    fun buttons_areDisplayed() {
        // Verify that the buttons defined in WearApp are present.
        composeTestRule.onNodeWithText("Button A").assertIsDisplayed()
        composeTestRule.onNodeWithText("Button B").assertIsDisplayed()
        composeTestRule.onNodeWithText("Button C").assertIsDisplayed()
    }

    @Test
    fun edgeButton_isDisplayed() {
        // Verify the "More" edge button is displayed.
        composeTestRule.onNodeWithText("More").assertIsDisplayed()
    }
}
