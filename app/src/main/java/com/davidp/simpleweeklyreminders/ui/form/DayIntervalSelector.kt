package com.davidp.simpleweeklyreminders.ui.form

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davidp.simpleweeklyreminders.ui.theme.dimensions

@Composable
fun DayIntervalSelector(interval: Int, onIntervalChange: (Int) -> Unit) {
    var inputText by remember { mutableStateOf(interval.toString()) }
    LaunchedEffect(interval) { inputText = interval.toString() }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Repeat every",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { if (interval > 1) onIntervalChange(interval - 1) },
                modifier = Modifier.width(MaterialTheme.dimensions.frequencyButtonWidth),
                contentPadding = PaddingValues(0.dp)
            ) { Text(text = "-", fontSize = 20.sp) }
            OutlinedTextField(
                value = inputText,
                onValueChange = { text ->
                    inputText = text
                    val parsed = text.toIntOrNull()
                    if (parsed != null && parsed >= 1) onIntervalChange(parsed)
                },
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .width(72.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
            )
            Button(
                onClick = { onIntervalChange(interval + 1) },
                modifier = Modifier.width(MaterialTheme.dimensions.frequencyButtonWidth),
                contentPadding = PaddingValues(0.dp)
            ) { Text(text = "+", fontSize = 20.sp) }
            Spacer(modifier = Modifier.width(8.dp))
            Text("days")
        }
    }
}
