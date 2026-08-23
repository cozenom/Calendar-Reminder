package com.davidp.simpleweeklyreminders.ui.form

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
// Aliased: LazyRow and LazyVerticalGrid each export an `items`, so both can't be imported plainly
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.davidp.simpleweeklyreminders.data.model.ReminderIconCategories
import com.davidp.simpleweeklyreminders.data.model.ReminderIconOption
import com.davidp.simpleweeklyreminders.ui.theme.appShapes
import com.davidp.simpleweeklyreminders.ui.theme.appTypography

/** Sentinel for the "all categories" chip, which is what a search runs against. */
private const val ALL_CATEGORIES = "All"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPickerSheet(
    currentKey: String?,
    onIconSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Selection is provisional until "Use" is tapped, so browsing away doesn't overwrite
    // the reminder's icon behind the sheet.
    var pendingKey by remember { mutableStateOf(currentKey) }
    var category by remember { mutableStateOf(ALL_CATEGORIES) }
    var query by remember { mutableStateOf("") }

    val allOptions = remember { ReminderIconCategories.flatMap { it.icons } }
    val visible = remember(category, query) {
        val inCategory = if (category == ALL_CATEGORIES) allOptions
        else ReminderIconCategories.first { it.name == category }.icons
        if (query.isBlank()) inCategory
        else allOptions.filter { it.label.contains(query, ignoreCase = true) ||
            it.key.contains(query, ignoreCase = true) }
    }
    val selected = allOptions.firstOrNull { it.key == pendingKey }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            Text(
                "Choose an icon",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 20.dp, bottom = 12.dp)
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search ${allOptions.size} icons") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.appShapes.medium,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            // A search spans every category, so the chips stop applying while one is active
            if (query.isBlank()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    items(listOf(ALL_CATEGORIES) + ReminderIconCategories.map { it.name }) { name ->
                        CategoryChip(
                            label = name,
                            selected = category == name,
                            onClick = { category = name }
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (visible.isEmpty()) {
                Text(
                    "No icons match “$query”",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 32.dp)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 56.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 320.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    gridItems(visible, key = { it.key }) { option ->
                        IconCell(
                            option = option,
                            isSelected = option.key == pendingKey,
                            onClick = { pendingKey = option.key }
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(MaterialTheme.appShapes.medium)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    selected?.let {
                        Icon(
                            imageVector = it.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "SELECTED",
                        style = MaterialTheme.appTypography.sectionLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        selected?.label ?: "None",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Button(
                    onClick = {
                        pendingKey?.let(onIconSelected)
                        onDismiss()
                    },
                    enabled = pendingKey != null,
                    shape = MaterialTheme.appShapes.medium
                ) {
                    Text("Use", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(MaterialTheme.appShapes.medium)
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .then(
                if (selected) Modifier
                else Modifier.border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.appShapes.medium)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 8.dp)
    )
}

@Composable
private fun IconCell(
    option: ReminderIconOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.appShapes.medium)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = option.icon,
            contentDescription = option.label,
            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(23.dp)
        )
    }
}
