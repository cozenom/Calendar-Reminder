package com.davidp.simpleweeklyreminders.ui.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.davidp.simpleweeklyreminders.data.model.Importance
import com.davidp.simpleweeklyreminders.data.model.SortDirection
import com.davidp.simpleweeklyreminders.data.model.SortMode
import com.davidp.simpleweeklyreminders.ui.theme.appShapes
import com.davidp.simpleweeklyreminders.ui.theme.appTypography
import com.davidp.simpleweeklyreminders.ui.theme.dimensions

private val ALL_IMPORTANCES = setOf(Importance.LOW, Importance.MEDIUM, Importance.HIGH)

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
    hidePaused: Boolean,
    onHidePausedChange: (Boolean) -> Unit,
    matchCount: Int,
    focusSearchOnOpen: Boolean = false,
    onDismiss: () -> Unit
) {
    val searchFocus = remember { FocusRequester() }
    LaunchedEffect(focusSearchOnOpen) {
        if (focusSearchOnOpen) searchFocus.requestFocus()
    }

    val sortOptions = listOf(
        SortMode.MANUAL to "Manual order",
        SortMode.NEXT_OCCURRENCE to "Next occurrence",
        SortMode.IMPORTANCE to "Importance",
        SortMode.DATE_ADDED to "Recently added",
        SortMode.TITLE to "Title"
    )
    val importanceOptions = listOf(
        Importance.HIGH to "High",
        Importance.MEDIUM to "Medium",
        Importance.LOW to "Low"
    )
    val isDefault = sortMode == SortMode.MANUAL && selectedImportances == ALL_IMPORTANCES &&
        searchQuery.isBlank() && !hidePaused

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sort & filter", style = MaterialTheme.typography.titleLarge)
                if (!isDefault) {
                    TextButton(onClick = {
                        onSortModeChange(SortMode.MANUAL)
                        onImportancesChange(ALL_IMPORTANCES)
                        onSearchQueryChange("")
                        onHidePausedChange(false)
                    }) {
                        Text("Reset", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search titles") },
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

            SectionLabel("Sort by")
            Column(
                modifier = Modifier
                    .clip(MaterialTheme.appShapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .selectableGroup()
            ) {
                sortOptions.forEachIndexed { index, (mode, label) ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = sortMode == mode,
                                onClick = { onSortModeChange(mode) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = sortMode == mode, onClick = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
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
                SortMode.TITLE -> "A to Z" to "Z to A"
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
                    // The label slot is a single-child container, not a Row — without this
                    // Row the arrow draws on top of the text instead of beside it.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(ascendingLabel, style = MaterialTheme.typography.labelLarge)
                    }
                }
                SegmentedButton(
                    selected = directionEnabled && sortDirection == SortDirection.DESCENDING,
                    onClick = { onSortDirectionChange(SortDirection.DESCENDING) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    enabled = directionEnabled,
                    icon = {}
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(descendingLabel, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            SectionLabel("Importance")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                importanceOptions.forEach { (importance, label) ->
                    val selected = selectedImportances.contains(importance)
                    ImportanceFilter(
                        label = label,
                        selected = selected,
                        onClick = {
                            val updated = if (selected) selectedImportances - importance
                            else selectedImportances + importance
                            // Never allow an empty selection — that would just show "no matches"
                            // for no clear reason, so treat it the same as "all selected".
                            onImportancesChange(if (updated.isEmpty()) ALL_IMPORTANCES else updated)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.appShapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onHidePausedChange(!hidePaused) }
                    .padding(start = 14.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Hide paused", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = hidePaused, onCheckedChange = onHidePausedChange)
            }

            Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.appShapes.medium
            ) {
                Text(
                    text = if (matchCount == 1) "Show 1 reminder" else "Show $matchCount reminders",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
            Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingMedium))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.appTypography.sectionLabel,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 7.dp)
    )
}

/** Filled when on, hairline outline when off — the same fill-not-hue rule used elsewhere. */
@Composable
private fun ImportanceFilter(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.appShapes.small)
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            .then(
                if (selected) Modifier
                else Modifier.border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.appShapes.small)
            )
            .selectable(selected = selected, onClick = onClick, role = Role.Checkbox)
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
