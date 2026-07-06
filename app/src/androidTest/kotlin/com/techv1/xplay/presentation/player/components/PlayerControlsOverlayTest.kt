package com.techv1.xplay.presentation.player.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerControlsOverlayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun playPauseButton_togglesCallback() {
        var playPauseClicked = false
        composeTestRule.setContent {
            PlayerControlsOverlay(
                isVisible = true,
                title = "Test Video",
                isPlaying = false,
                isFullscreen = false,
                positionMs = 10_000,
                durationMs = 60_000,
                bufferedPositionMs = 20_000,
                playbackSpeed = 1f,
                onBackClick = {},
                onPlayPauseClick = { playPauseClicked = true },
                onSeek = {},
                onToggleFullscreen = {},
                onTrackSelectorClick = {},
                onSpeedClick = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Play")
            .assertIsDisplayed()
            .performClick()

        assert(playPauseClicked)
    }

    @Test
    fun fullscreenButton_togglesCallback() {
        var fullscreenClicked = false
        composeTestRule.setContent {
            PlayerControlsOverlay(
                isVisible = true,
                title = "Test Video",
                isPlaying = true,
                isFullscreen = false,
                positionMs = 10_000,
                durationMs = 60_000,
                bufferedPositionMs = 20_000,
                playbackSpeed = 1f,
                onBackClick = {},
                onPlayPauseClick = {},
                onSeek = {},
                onToggleFullscreen = { fullscreenClicked = true },
                onTrackSelectorClick = {},
                onSpeedClick = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Enter fullscreen")
            .assertIsDisplayed()
            .performClick()

        assert(fullscreenClicked)
    }

    @Test
    fun controlsHidden_doNotShowPlayButton() {
        composeTestRule.setContent {
            PlayerControlsOverlay(
                isVisible = false,
                title = "Test Video",
                isPlaying = false,
                isFullscreen = false,
                positionMs = 10_000,
                durationMs = 60_000,
                bufferedPositionMs = 20_000,
                playbackSpeed = 1f,
                onBackClick = {},
                onPlayPauseClick = {},
                onSeek = {},
                onToggleFullscreen = {},
                onTrackSelectorClick = {},
                onSpeedClick = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Play")
            .assertDoesNotExist()
    }
}
