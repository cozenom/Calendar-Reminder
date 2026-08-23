package com.davidp.simpleweeklyreminders.ui.calendar

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.davidp.simpleweeklyreminders.data.model.Importance
import com.davidp.simpleweeklyreminders.data.model.OccurrenceStatus
import com.davidp.simpleweeklyreminders.data.model.ReminderLog
import com.davidp.simpleweeklyreminders.data.model.iconFromKey
import com.davidp.simpleweeklyreminders.data.settings.timePattern
import com.davidp.simpleweeklyreminders.ui.components.ImportanceChevrons
import com.davidp.simpleweeklyreminders.ui.theme.LocalAppSettings
import com.davidp.simpleweeklyreminders.ui.theme.appShapes
import com.davidp.simpleweeklyreminders.ui.theme.reminderAccent
import com.davidp.simpleweeklyreminders.ui.theme.reminderColors
import com.davidp.simpleweeklyreminders.ui.theme.reminderWash
import java.time.format.DateTimeFormatter

@Composable
fun ReminderEventItem(
    log: ReminderLog,
    status: OccurrenceStatus,
    iconKey: String?,
    colorKey: String?,
    importance: Importance,
    onToggle: () -> Unit
) {
    val colors = MaterialTheme.reminderColors
    val timePattern = LocalAppSettings.current.timeFormat.timePattern(LocalContext.current)
    val done = status == OccurrenceStatus.DONE
    // Falls back to the theme accent when per-reminder colours are off or none is set
    val accent = reminderAccent(colorKey)

    // Completed work recedes: its icon tile takes the accent wash and the title drops to the
    // muted ink, leaving anything still outstanding at full strength.
    val tileColor by animateColorAsState(
        if (done) reminderWash(accent) else colors.neutral,
        label = "eventTileColor"
    )
    val iconTint by animateColorAsState(
        if (done) accent else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "eventIconTint"
    )
    val titleColor by animateColorAsState(
        if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        label = "eventTitleColor"
    )

    val (markIcon, markTint, markLabel) = when (status) {
        OccurrenceStatus.DONE -> Triple(Icons.Filled.CheckCircle, accent, "Done")
        OccurrenceStatus.MISSED -> Triple(Icons.Filled.ErrorOutline, colors.missed, "Missed")
        OccurrenceStatus.PENDING -> Triple(Icons.Filled.RadioButtonUnchecked, colors.hairline, "Not done yet")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.appShapes.small)
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(MaterialTheme.appShapes.small)
                .background(tileColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconFromKey(iconKey).icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = log.logDateTime.format(DateTimeFormatter.ofPattern(timePattern)),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(12.dp))
        // Chevrons lead the title so they align in one column down the list
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            ImportanceChevrons(importance = importance, chevronWidth = 13.dp, chevronHeight = 5.dp)
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = log.title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        val snoozedUntil = log.snoozedUntil
        if (snoozedUntil != null && !log.completed) {
            Icon(
                imageVector = Icons.Filled.Snooze,
                contentDescription = "Snoozed",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = snoozedUntil.format(DateTimeFormatter.ofPattern(timePattern)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Icon(
            imageVector = markIcon,
            contentDescription = markLabel,
            tint = markTint,
            modifier = Modifier.size(24.dp)
        )
    }
}
