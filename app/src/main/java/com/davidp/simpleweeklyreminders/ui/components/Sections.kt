package com.davidp.simpleweeklyreminders.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.davidp.simpleweeklyreminders.ui.theme.appShapes
import com.davidp.simpleweeklyreminders.ui.theme.appTypography

/**
 * Uppercase heading above a group of rows — the form, the sort sheet and Settings all use it.
 *
 * The spacing lives here rather than at the call sites: the three copies this replaced had
 * drifted 2-4dp apart from each other for no design reason.
 */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.appTypography.sectionLabel,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 4.dp, top = 16.dp, bottom = 7.dp)
    )
}

/**
 * Rounded neutral container holding a section's rows, divided by hairlines.
 *
 * [shape] is [medium][com.davidp.simpleweeklyreminders.ui.theme.AppShapes.medium] by default;
 * Settings uses `large` because its containers are full-width cards rather than inline groups.
 */
@Composable
fun GroupSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.appShapes.medium,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(contentPadding),
        content = content
    )
}
