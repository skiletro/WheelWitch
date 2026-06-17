package com.skiletro.wheelwitch.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val buttonShape = RoundedCornerShape(14.dp)
val sectionShape = RoundedCornerShape(20.dp)

@Composable
fun Modifier.focusBorder(
    isFocused: Boolean,
    shape: RoundedCornerShape = buttonShape,
    color: Color = MaterialTheme.colorScheme.primary
): Modifier = this.then(
    if (isFocused) Modifier.border(width = 3.dp, color = color, shape = shape)
    else Modifier
)
