package com.davidp.simpleweeklyreminders.ui.settings

import android.os.Build
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.davidp.simpleweeklyreminders.data.settings.ThemePack
import com.davidp.simpleweeklyreminders.ui.theme.CalendarAppTheme
import com.davidp.simpleweeklyreminders.ui.theme.ThemePackPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/**
 * Material You shares the pack list rather than being a separate switch, so "which row is
 * selected" spans two different settings. That interaction is what these cover.
 */
class ThemePackScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun setContent(
        selected: ThemePack = ThemePack.PAPER,
        dynamicColor: Boolean = false,
        onSelectPack: (ThemePack) -> Unit = {},
        onSelectDynamic: () -> Unit = {},
        onBack: () -> Unit = {}
    ) {
        compose.setContent {
            CalendarAppTheme {
                ThemePackScreen(
                    selected = selected,
                    dynamicColor = dynamicColor,
                    onSelectPack = onSelectPack,
                    onSelectDynamic = onSelectDynamic,
                    onBack = onBack
                )
            }
        }
    }

    @Test
    fun everyPackIsListed() {
        setContent()
        ThemePack.entries.forEach { pack ->
            compose.onNodeWithText(ThemePackPalette.getValue(pack).label)
                .performScrollTo()
                .assertExists()
        }
    }

    @Test
    fun theStoredPackShowsAsSelected() {
        setContent(selected = ThemePack.PLUM)
        compose.onNodeWithText("Plum").performScrollTo().assertIsSelected()
    }

    @Test
    fun tappingAPackReportsIt() {
        var chosen: ThemePack? = null
        setContent(selected = ThemePack.PAPER, onSelectPack = { chosen = it })

        compose.onNodeWithText("Moss").performScrollTo().performClick()

        assertEquals(ThemePack.MOSS, chosen)
    }

    @Test
    fun noPackIsSelectedWhileMaterialYouIsOn() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        // dynamicColor overrides the pack, so showing both as selected would be a lie
        setContent(selected = ThemePack.PAPER, dynamicColor = true)

        compose.onNodeWithText("Paper").performScrollTo().assertIsNotSelected()
        compose.onNodeWithText("Material You").performScrollTo().assertIsSelected()
    }

    @Test
    fun tappingMaterialYouReportsIt() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        var dynamicChosen = false
        setContent(dynamicColor = false, onSelectDynamic = { dynamicChosen = true })

        compose.onNodeWithText("Material You").performScrollTo().performClick()

        assertTrue(dynamicChosen)
    }

    @Test
    fun materialYouIsHiddenBelowAndroid12() {
        // The row would do nothing there, so it isn't offered
        assumeTrue(Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
        setContent()

        compose.onNodeWithText("Material You").assertDoesNotExist()
    }

    @Test
    fun backIsReachable() {
        var wentBack = false
        setContent(onBack = { wentBack = true })

        compose.onNodeWithContentDescription("Back").performClick()

        assertTrue(wentBack)
    }
}
