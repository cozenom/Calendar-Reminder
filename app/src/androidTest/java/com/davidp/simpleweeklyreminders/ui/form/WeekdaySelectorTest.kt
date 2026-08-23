package com.davidp.simpleweeklyreminders.ui.form

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToString
import com.davidp.simpleweeklyreminders.data.settings.AppSettingsState
import com.davidp.simpleweeklyreminders.data.settings.WeekStart
import com.davidp.simpleweeklyreminders.ui.theme.CalendarAppTheme
import com.davidp.simpleweeklyreminders.ui.theme.LocalAppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The day circles are single letters that repeat (T/T, S/S) and their order depends on the
 * week-start setting, so both are worth pinning down.
 */
class WeekdaySelectorTest {

    @get:Rule
    val compose = createComposeRule()

    private fun setContent(
        weekStart: WeekStart,
        selected: Set<Int>,
        onChanged: (Set<Int>) -> Unit = {}
    ) {
        compose.setContent {
            CalendarAppTheme {
                CompositionLocalProvider(
                    LocalAppSettings provides AppSettingsState(weekStart = weekStart)
                ) {
                    WeekdaySelector(selectedDays = selected, onDaysChanged = onChanged)
                }
            }
        }
    }

    @Test
    fun repeatedLettersAreDistinguishableByName() {
        // "T" appears twice and "S" twice, so the letter alone can't identify a day
        setContent(WeekStart.MONDAY, selected = setOf(2))

        compose.onNodeWithContentDescription("Tuesday").assertIsOn()
        compose.onNodeWithContentDescription("Thursday").assertIsOff()
        compose.onNodeWithContentDescription("Saturday").assertIsOff()
        compose.onNodeWithContentDescription("Sunday").assertIsOff()
    }

    @Test
    fun tappingADayReportsIt() {
        var reported: Set<Int>? = null
        setContent(WeekStart.MONDAY, selected = setOf(1)) { reported = it }

        compose.onNodeWithContentDescription("Friday").performClick()

        assertEquals(setOf(1, 5), reported)
    }

    @Test
    fun deselectingTheLastDayIsRefused() {
        // A reminder with no days would silently never fire
        var reported: Set<Int>? = null
        setContent(WeekStart.MONDAY, selected = setOf(3)) { reported = it }

        compose.onNodeWithContentDescription("Wednesday").performClick()

        assertEquals(null, reported)
    }

    @Test
    fun sundayLeadsWhenWeekStartsOnSunday() {
        setContent(WeekStart.SUNDAY, selected = setOf(7))

        // The rendered tree lists nodes in layout order, so Sunday must appear before Monday
        val tree = compose.onRoot().printToString()
        assertTrue(
            "Sunday should be the first column when the week starts on Sunday",
            tree.indexOf("Sunday") < tree.indexOf("Monday")
        )
    }

    @Test
    fun mondayLeadsWhenWeekStartsOnMonday() {
        setContent(WeekStart.MONDAY, selected = setOf(1))

        val tree = compose.onRoot().printToString()
        assertTrue(
            "Monday should be the first column when the week starts on Monday",
            tree.indexOf("Monday") < tree.indexOf("Sunday")
        )
    }
}
