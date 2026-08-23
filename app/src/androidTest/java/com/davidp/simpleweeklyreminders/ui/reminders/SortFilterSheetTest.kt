package com.davidp.simpleweeklyreminders.ui.reminders

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.davidp.simpleweeklyreminders.data.model.Importance
import com.davidp.simpleweeklyreminders.data.model.SortDirection
import com.davidp.simpleweeklyreminders.data.model.SortMode
import com.davidp.simpleweeklyreminders.ui.theme.CalendarAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private val ALL = setOf(Importance.LOW, Importance.MEDIUM, Importance.HIGH)

class SortFilterSheetTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun directionLabelsFollowTheSortMode() {
        // Each mode names its own directions — "Ascending" is meaningless for importance
        compose.setContent {
            CalendarAppTheme {
                Sheet(sortMode = SortMode.TITLE)
            }
        }

        compose.onNodeWithText("A to Z").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Z to A").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun directionIsDisabledForManualOrder() {
        // Drag order has no reverse; the control stays visible so the sheet doesn't resize
        compose.setContent {
            CalendarAppTheme {
                Sheet(sortMode = SortMode.MANUAL)
            }
        }

        compose.onNodeWithText("Ascending").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun theConfirmButtonCountsMatches() {
        compose.setContent {
            CalendarAppTheme {
                Sheet(matchCount = 4)
            }
        }

        compose.onNodeWithText("Show 4 reminders").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun theConfirmButtonSingularisesOne() {
        compose.setContent {
            CalendarAppTheme {
                Sheet(matchCount = 1)
            }
        }

        compose.onNodeWithText("Show 1 reminder").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun clearingTheLastImportanceRestoresAllThree() {
        // An empty selection would show "no matches" for no visible reason
        var reported: Set<Importance>? = null
        compose.setContent {
            CalendarAppTheme {
                Sheet(
                    selectedImportances = setOf(Importance.HIGH),
                    onImportancesChange = { reported = it }
                )
            }
        }

        compose.onNodeWithText("High").performScrollTo().performClick()

        assertEquals(ALL, reported)
    }

    @Test
    fun resetIsOfferedOnlyWhenSomethingIsNonDefault() {
        compose.setContent {
            CalendarAppTheme {
                Sheet(hidePaused = true)
            }
        }
        compose.onNodeWithText("Reset").assertIsDisplayed()
    }

    @Test
    fun resetIsHiddenWhenEverythingIsDefault() {
        compose.setContent {
            CalendarAppTheme { Sheet() }
        }
        compose.onNodeWithText("Reset").assertDoesNotExist()
    }

    @Test
    fun resetClearsEveryFilter() {
        var mode: SortMode? = null
        var importances: Set<Importance>? = null
        var query: String? = null
        var paused: Boolean? = null

        compose.setContent {
            CalendarAppTheme {
                Sheet(
                    sortMode = SortMode.TITLE,
                    searchQuery = "meds",
                    hidePaused = true,
                    selectedImportances = setOf(Importance.HIGH),
                    onSortModeChange = { mode = it },
                    onImportancesChange = { importances = it },
                    onSearchQueryChange = { query = it },
                    onHidePausedChange = { paused = it }
                )
            }
        }

        compose.onNodeWithText("Reset").performClick()

        assertEquals(SortMode.MANUAL, mode)
        assertEquals(ALL, importances)
        assertEquals("", query)
        assertTrue(paused == false)
    }
}

/** Every parameter defaulted, so each test states only what it cares about. */
@androidx.compose.runtime.Composable
private fun Sheet(
    sortMode: SortMode = SortMode.MANUAL,
    onSortModeChange: (SortMode) -> Unit = {},
    sortDirection: SortDirection = SortDirection.ASCENDING,
    onSortDirectionChange: (SortDirection) -> Unit = {},
    selectedImportances: Set<Importance> = ALL,
    onImportancesChange: (Set<Importance>) -> Unit = {},
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    hidePaused: Boolean = false,
    onHidePausedChange: (Boolean) -> Unit = {},
    matchCount: Int = 3,
    onDismiss: () -> Unit = {}
) {
    SortFilterSheet(
        sortMode = sortMode,
        onSortModeChange = onSortModeChange,
        sortDirection = sortDirection,
        onSortDirectionChange = onSortDirectionChange,
        selectedImportances = selectedImportances,
        onImportancesChange = onImportancesChange,
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        hidePaused = hidePaused,
        onHidePausedChange = onHidePausedChange,
        matchCount = matchCount,
        onDismiss = onDismiss
    )
}
