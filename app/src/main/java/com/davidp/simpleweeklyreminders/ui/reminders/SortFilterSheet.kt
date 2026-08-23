package com.davidp.simpleweeklyreminders.ui.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.davidp.simpleweeklyreminders.data.model.Importance
import com.davidp.simpleweeklyreminders.data.model.SortDirection
import com.davidp.simpleweeklyreminders.data.model.SortMode
import com.davidp.simpleweeklyreminders.ui.theme.appShapes
import com.davidp.simpleweeklyreminders.ui.theme.dimensions

/** Sort-order + importance-filter + title-search bottom sheet for the Reminders tab. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortFilterSheet(
    sortMode: SortMode,
    onSortModeChange: (SortMode) -> Unit,
    sortDirection: SortDirection,
    onSortDirectionChange: (SortDirection) -> Unit,
    selectedImportances: Set<Importance>,
    onImportancesChange: (Set<Importance>) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    focusSearchOnOpen: Boolean = false,
    onDismiss: () -> Unit
) {
    val searchFocus = remember { FocusRequester() }
    LaunchedEffect(focusSearchOnOpen) {
        if (focusSearchOnOpen) searchFocus.requestFocus()
    }
    val sortOptions = listOf(
        SortMode.MANUAL to "Manual (drag)",
        SortMode.NEXT_OCCURRENCE to "Next occurrence",
        SortMode.IMPORTANCE to "Importance",
        SortMode.DATE_ADDED to "Date added"
    )
    val importanceOptions = listOf(
        Importance.HIGH to "High",
        Importance.MEDIUM to "Medium",
        Importance.LOW to "Low"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("Search title") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.appShapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(searchFocus)
            )

            Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))
            Text("Sort by", style = MaterialTheme.typography.titleSmall)
            Column(Modifier.selectableGroup()) {
                sortOptions.forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = sortMode == mode,
                                onClick = { onSortModeChange(mode) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = sortMode == mode, onClick = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Direction has no meaning for drag order — disabled (not hidden) for Manual so
            // the sheet's height stays constant across every sort mode.
            val directionEnabled = sortMode != SortMode.MANUAL
            val (ascendingLabel, descendingLabel) = when (sortMode) {
                SortMode.IMPORTANCE -> "Low first" to "High first"
                SortMode.NEXT_OCCURRENCE -> "Soonest first" to "Latest first"
                SortMode.DATE_ADDED -> "Oldest first" to "Newest first"
                SortMode.MANUAL -> "Ascending" to "Descending"
            }
            Spacer(modifier = Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = directionEnabled && sortDirection == SortDirection.ASCENDING,
                    onClick = { onSortDirectionChange(SortDirection.ASCENDING) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    enabled = directionEnabled,
                    icon = {}
                ) {
                    Text(ascendingLabel)
                }
                SegmentedButton(
                    selected = directionEnabled && sortDirection == SortDirection.DESCENDING,
                    onClick = { onSortDirectionChange(SortDirection.DESCENDING) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    enabled = directionEnabled,
                    icon = {}
                ) {
                    Text(descendingLabel)
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))
            Text("Filter by importance", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                importanceOptions.forEach { (importance, label) ->
                    val selected = selectedImportances.contains(importance)
                    FilterChip(
                        selected = selected,
                        onClick = {
                            val updated = if (selected) selectedImportances - importance else selectedImportances + importance
                            // Never allow an empty selection — that would just show "no matches"
                            // for no clear reason, so treat it the same as "all selected".
                            onImportancesChange(
                                if (updated.isEmpty()) setOf(Importance.LOW, Importance.MEDIUM, Importance.HIGH) else updated
                            )
                        },
                        label = { Text(label) },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                }
            }
        }
    }
}
