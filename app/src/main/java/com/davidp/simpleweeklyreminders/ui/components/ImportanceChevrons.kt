package com.davidp.simpleweeklyreminders.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.davidp.simpleweeklyreminders.data.model.Importance
import com.davidp.simpleweeklyreminders.ui.theme.reminderColors

/**
 * Shared importance marker (list row, calendar day list): rank chevrons, one stripe per
 * level. Custom-drawn rather than a repurposed arrow icon so the stripes interlock like
 * rank insignia. Count is the primary signal, color reinforces it (see ReminderColors).
 */
@Composable
fun ImportanceChevrons(
    importance: Importance,
    modifier: Modifier = Modifier,
    chevronWidth: Dp = 16.dp,
    chevronHeight: Dp = 6.dp,
    strokeWidth: Dp = 2.dp
) {
    val color = with(MaterialTheme.reminderColors) {
        when (importance) {
            Importance.LOW -> importanceLow
            Importance.MEDIUM -> importanceMedium
            Importance.HIGH -> importanceHigh
        }
    }
    val count = when (importance) {
        Importance.LOW -> 1
        Importance.MEDIUM -> 2
        Importance.HIGH -> 3
    }
    val label = when (importance) {
        Importance.LOW -> "Low importance"
        Importance.MEDIUM -> "Medium importance"
        Importance.HIGH -> "High importance"
    }

    Column(
        modifier = modifier.semantics { contentDescription = label },
        verticalArrangement = Arrangement.spacedBy(-(strokeWidth))
    ) {
        repeat(count) {
            Canvas(modifier = Modifier.size(width = chevronWidth, height = chevronHeight)) {
                val strokePx = strokeWidth.toPx()
                val path = Path().apply {
                    moveTo(0f, size.height)
                    lineTo(size.width / 2f, 0f)
                    lineTo(size.width, size.height)
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}
