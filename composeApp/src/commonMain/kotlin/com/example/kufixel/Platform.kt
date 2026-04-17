package com.example.kufixel

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

interface ImageSaver {
    fun saveImage(
        name: String,
        drawings: Map<Pair<Int, Int>, CellContent>,
        width: Int,
        height: Int,
        gridSize: Int,
        format: String
    ): Boolean
}

expect fun getImageSaver(): ImageSaver
