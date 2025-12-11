package com.example.drum.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BackgroundTheme(
    val name: String,
    val brush: Brush
)

@Composable
fun DrumScreen(
    onMenuClick: () -> Unit = {},
    onBackgroundThemeSelected: (BackgroundTheme) -> Unit = {}
) {
    var selectedBackground by remember { mutableStateOf(0) }
    var showBackgroundMenu by remember { mutableStateOf(false) }
    var activePadIndex by remember { mutableStateOf<Int?>(null) }

    val backgroundThemes = listOf(
        BackgroundTheme(
            name = "Ocean Blue",
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF1e3a8a),
                    Color(0xFF3b82f6),
                    Color(0xFF60a5fa)
                )
            )
        ),
        BackgroundTheme(
            name = "Sunset",
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF7c2d12),
                    Color(0xFFea580c),
                    Color(0xFFfbbf24)
                )
            )
        ),
        BackgroundTheme(
            name = "Forest Green",
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF14532d),
                    Color(0xFF15803d),
                    Color(0xFF4ade80)
                )
            )
        ),
        BackgroundTheme(
            name = "Purple Night",
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF3f0f5c),
                    Color(0xFF7c3aed),
                    Color(0xFFc084fc)
                )
            )
        ),
        BackgroundTheme(
            name = "Dark Slate",
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0f172a),
                    Color(0xFF1e293b),
                    Color(0xFF334155)
                )
            )
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundThemes[selectedBackground].brush)
    ) {
        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar with Menu and Background Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hamburger Menu Button
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Menu",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Title
                Text(
                    text = "Drum Pad",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Background Theme Button
                Button(
                    onClick = { showBackgroundMenu = !showBackgroundMenu },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Text(
                        text = "🎨",
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Drum Pads Grid (4x2)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(2) { rowIndex ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(4) { columnIndex ->
                            val padIndex = rowIndex * 4 + columnIndex
                            val padLabels = listOf(
                                "Kick", "Snare", "Hi-Hat", "Tom",
                                "Clap", "Perc", "Cymbal", "Bell"
                            )

                            DrumPad(
                                label = padLabels[padIndex],
                                isActive = activePadIndex == padIndex,
                                onClick = {
                                    activePadIndex = padIndex
                                    // Simulate pad press animation
                                    vibratePad()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Background Theme Selection Menu
            if (showBackgroundMenu) {
                BackgroundThemeMenu(
                    themes = backgroundThemes,
                    selectedIndex = selectedBackground,
                    onThemeSelected = { index ->
                        selectedBackground = index
                        onBackgroundThemeSelected(backgroundThemes[index])
                        showBackgroundMenu = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
fun DrumPad(
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isActive) {
        Color(0xFFfbbf24)
    } else {
        Color.White.copy(alpha = 0.2f)
    }

    val scale = if (isActive) 0.95f else 1f

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(12.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        onClick()
                        tryAwaitRelease()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isActive) Color.Black else Color.White,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
fun BackgroundThemeMenu(
    themes: List<BackgroundTheme>,
    selectedIndex: Int,
    onThemeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Select Background Theme",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            themes.forEachIndexed { index, theme ->
                ThemeOptionItem(
                    theme = theme,
                    isSelected = index == selectedIndex,
                    onClick = { onThemeSelected(index) }
                )
            }
        }
    }
}

@Composable
fun ThemeOptionItem(
    theme: BackgroundTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) {
                Color.White.copy(alpha = 0.3f)
            } else {
                Color.White.copy(alpha = 0.1f)
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, Color.White)
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = theme.name,
                fontSize = 14.sp,
                color = Color.White,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )

            if (isSelected) {
                Text(
                    text = "✓",
                    fontSize = 16.sp,
                    color = Color(0xFFfbbf24)
                )
            }
        }
    }
}

/**
 * Simulates haptic feedback for drum pad press
 * In a real implementation, this would trigger device vibration
 */
private fun vibratePad() {
    // TODO: Implement actual haptic feedback using VibrationEffect
    // This would require context access and permission in AndroidManifest.xml
}

@Composable
fun DrumScreenPreview() {
    DrumScreen(
        onMenuClick = {},
        onBackgroundThemeSelected = {}
    )
}
