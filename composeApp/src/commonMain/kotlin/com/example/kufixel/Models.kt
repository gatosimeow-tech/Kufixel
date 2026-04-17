package com.example.kufixel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

sealed class CellContent {
    data class Solid(val color: Int) : CellContent()
    data class Stamped(
        val stampName: String,
        val color: Int,
        val backgroundColor: Int? = null,
        val rotation: Float = 0f
    ) : CellContent()
}

data class Project(
    val id: String,
    val name: String,
    val width: Int?,
    val height: Int?,
    val gridSize: Int = 20,
    val showGrid: Boolean = true,
    val drawings: Map<Pair<Int, Int>, CellContent> = emptyMap()
) {
    val gridSizeString: String get() = "${width ?: "∞"}x${height ?: "∞"}"
}

data class Stamp(
    val name: String,
    val gridSize: Int = 1,
    val pattern: List<List<Int?>> // 0 for solid (current color), null for transparent
)

val HamzahV1 = Stamp(
    name = "HamzahV1",
    gridSize = 1,
    pattern = listOf(
        listOf(0, 0, 0),
        listOf(0, null, null),
        listOf(0, 0, 0)
    )
)

val HamzahV2 = Stamp(
    name = "HamzahV2",
    gridSize = 1,
    pattern = listOf(
        "55000",
        "55050",
        "55050",
        "55055",
        "00000"
    ).map { row ->
        row.map { char -> if (char == '0') 0 else null }
    }
)

val Kaaf = Stamp(
    name = "Kaaf",
    gridSize = 3,
    pattern = listOf(
        "55555",
        "50000",
        "55555",
        "00005",
        "55555"
    ).map { row ->
        row.map { char -> if (char == '5') 0 else null }
    }
)

/**
 * App Logo Pattern based on the attached design.
 * This is a 15x15 Square Kufic pattern representing "KUFIXEL".
 */
val KufixelLogoPattern = listOf(
    "555555505050555",
    "500000500000505",
    "505550555555505",
    "505050500000505",
    "505050505555505",
    "505000000000005",
    "505555505050505",
    "500050005050505",
    "505055555555555",
    "500000000000000",
    "505550555055555",
    "505050505050000",
    "505550555055555",
    "500050005000005",
    "555550555555555"
).map { row ->
    row.map { char -> if (char == '5') 0 else null }
}

val KufixelLogo = Stamp(
    name = "Kufixel",
    gridSize = 5,
    pattern = KufixelLogoPattern
)

val StampsRegistry = mapOf(
    "Kaaf" to Kaaf,
    "HamzahV1" to HamzahV1,
    "HamzahV2" to HamzahV2,
    "Kufixel" to KufixelLogo
)
