package com.punto.app.presentation.components

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.punto.app.domain.model.AccentColor
import com.punto.app.domain.model.ThemePreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSegmentedRow(selected: ThemePreference, onSelect: (ThemePreference) -> Unit, modifier: Modifier = Modifier) {
    val options = listOf(
        ThemePreference.SYSTEM to "Automático",
        ThemePreference.LIGHT to "Claro",
        ThemePreference.DARK to "Oscuro",
    )
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = selected == value,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) { Text(label, maxLines = 1) }
        }
    }
}

private fun swatchBrush(accent: AccentColor): Brush = when (accent) {
    AccentColor.TEAL -> SolidColor(Color(0xFF0B6E6B))
    AccentColor.BLUE -> SolidColor(Color(0xFF0B5FA5))
    AccentColor.VIOLET -> SolidColor(Color(0xFF6750A4))
    AccentColor.ROSE -> SolidColor(Color(0xFFA8305E))
    AccentColor.AMBER -> SolidColor(Color(0xFFB36A00))
    AccentColor.DYNAMIC -> Brush.sweepGradient(
        listOf(Color(0xFFE53935), Color(0xFFFDD835), Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFF8E24AA), Color(0xFFE53935)),
    )
}

@Composable
fun AccentSwatches(selected: AccentColor, onSelect: (AccentColor) -> Unit, modifier: Modifier = Modifier) {
    val options = AccentColor.entries.filter { it != AccentColor.DYNAMIC || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEach { accent ->
            val isSelected = accent == selected
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.12f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "swatchScale",
            )
            val checkProgress by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                label = "swatchCheck",
            )
            Box(
                Modifier
                    .size(40.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(swatchBrush(accent), CircleShape)
                    .then(if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                    .clickable(role = Role.RadioButton) { onSelect(accent) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            alpha = checkProgress
                            scaleX = checkProgress
                            scaleY = checkProgress
                        },
                )
            }
        }
    }
}
