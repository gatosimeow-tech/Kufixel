package com.example.kufixel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val PrimaryTextIcons = Color(0xFF1A1A1A)
val MainWorkspaceBackground = Color(0xFFF0F2F5)
val CanvasSurround = Color(0xFFE5E7EB)
val HeaderPanels = Color.White
val BorderColor = Color(0xFFE5E7EB)

@Composable
fun KufixelLogo(
    color: Color = Color.White,
    size: Dp = 100.dp
) {
    Canvas(modifier = Modifier.size(size)) {
        val cellSize = this.size.width / 15f
        KufixelLogoPattern.forEachIndexed { rowIdx, row ->
            row.forEachIndexed { colIdx, value ->
                if (value != null) {
                    drawRect(
                        color = color,
                        topLeft = Offset(colIdx * cellSize, rowIdx * cellSize),
                        size = Size(cellSize, cellSize)
                    )
                }
            }
        }
    }
}

@Composable
fun KufixelBrand(
    logoSize: Dp = 100.dp,
    fontSize: TextUnit = 32.sp,
    textColor: Color = PrimaryTextIcons,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    spacing: Dp = 8.dp
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment
    ) {
        Box(
            modifier = Modifier
                .size(logoSize)
                .clip(RoundedCornerShape(size = logoSize * 0.2f))
                .background(Color.Black)
                .padding(logoSize * 0.15f),
            contentAlignment = Alignment.Center
        ) {
            KufixelLogo(color = Color.White, size = logoSize)
        }
        Spacer(modifier = Modifier.height(spacing))
        Text(
            text = "KUFIXEL",
            color = textColor,
            fontSize = fontSize,
            fontWeight = FontWeight.Black
        )
    }
}
