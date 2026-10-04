package com.davidp.simpleweeklyreminders.ui.form

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import com.davidp.simpleweeklyreminders.data.model.SILENT_SOUND
import com.davidp.simpleweeklyreminders.ui.components.GroupSurface

/**
 * Tone row. Opens Android's own tone picker, so there's no list of sounds to maintain.
 * [sound] is a ringtone URI, [SILENT_SOUND], or null for the importance level's tone.
 */
@Composable
fun SoundSelector(sound: String?, onChanged: (String?) -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val picked = result.data?.let {
            IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        }
        // The picker returns no URI when "None" is chosen
        onChanged(picked?.toString() ?: SILENT_SOUND)
    }
    val label = remember(sound) { soundLabel(context, sound) }

    GroupSurface(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { launcher.launch(pickerIntent(sound)) }
                .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sound", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (sound == null) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface
            )
            if (sound != null) {
                TextButton(onClick = { onChanged(null) }) { Text("Clear") }
            }
        }
    }
}

private fun pickerIntent(sound: String?): Intent =
    Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        // Alarm tones too: High's default tone comes from the alarm slot
        .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_ALARM)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Reminder sound")
        // "Default" is our Clear button (= level tone), not the picker's own default entry
        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)
        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
        .putExtra(
            RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
            sound?.takeIf { it != SILENT_SOUND }?.toUri()
        )

private fun soundLabel(context: Context, sound: String?): String = when (sound) {
    null -> "Default"
    SILENT_SOUND -> "None"
    // getTitle can throw for a tone the app can't read; the name is cosmetic
    else -> runCatching { RingtoneManager.getRingtone(context, sound.toUri())?.getTitle(context) }
        .getOrNull() ?: "Custom"
}
