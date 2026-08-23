package com.davidp.simpleweeklyreminders.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.davidp.simpleweeklyreminders.data.settings.ThemePack
import com.davidp.simpleweeklyreminders.ui.theme.LocalIsDarkTheme
import com.davidp.simpleweeklyreminders.ui.theme.ThemePackPalette
import com.davidp.simpleweeklyreminders.ui.theme.appShapes

/**
 * Accent picker. Material You sits in the same list rather than as a separate switch — from
 * the user's side it is one choice: "what colours this app".
 */
@Composable
fun ThemePackScreen(
    selected: ThemePack,
    dynamicColor: Boolean,
    onSelectPack: (ThemePack) -> Unit,
    onSelectDynamic: () -> Unit,
    onBack: () -> Unit
) {
    val dark = LocalIsDarkTheme.current
    // Material You needs Android 12+; on older devices the row would do nothing
    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Theme pack", style = MaterialTheme.typography.headlineSmall)
        }
        Text(
            "The accent colours completed days in your calendar",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 57.dp, end = 20.dp, bottom = 12.dp)
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            items(ThemePack.entries) { pack ->
                val tones = ThemePackPalette.getValue(pack)
                PackRow(
                    label = tones.label,
                    description = tones.description,
                    isSelected = !dynamicColor && pack == selected,
                    onClick = { onSelectPack(pack) }
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Swatch(
                            color = if (dark) tones.accentDark else tones.accentLight,
                            shape = RoundedCornerShape(topStart = 7.dp, bottomStart = 7.dp, topEnd = 3.dp, bottomEnd = 3.dp),
                            width = 20.dp
                        )
                        Swatch(
                            color = if (dark) tones.containerDark else tones.deep,
                            shape = RoundedCornerShape(0.dp),
                            width = 13.dp
                        )
                        Swatch(
                            color = if (dark) tones.pale else tones.containerLight,
                            shape = RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 7.dp, bottomEnd = 7.dp),
                            width = 20.dp
                        )
                    }
                }
            }

            if (dynamicAvailable) {
                item {
                    PackRow(
                        label = "Material You",
                        description = "From your wallpaper",
                        isSelected = dynamicColor,
                        onClick = onSelectDynamic
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 56.dp, height = 30.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF7A5C9E),
                                            Color(0xFFC4A0D8),
                                            Color(0xFFE9DEF6)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Wallpaper,
                                contentDescription = null,
                                tint = Color(0xFF2B1B38),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PackRow(
    label: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    swatches: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.appShapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.appShapes.large
            )
            .selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        swatches()
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
private fun Swatch(color: Color, shape: androidx.compose.ui.graphics.Shape, width: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(width = width, height = 30.dp)
            .clip(shape)
            .background(color)
    )
}
