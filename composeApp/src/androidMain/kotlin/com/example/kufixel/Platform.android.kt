package com.example.kufixel

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.provider.MediaStore
import java.io.OutputStream

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

object AndroidImageSaver : ImageSaver {
    var context: Context? = null

    override fun saveImage(
        name: String,
        drawings: Map<Pair<Int, Int>, CellContent>,
        width: Int,
        height: Int,
        gridSize: Int,
        format: String
    ): Boolean {
        val ctx = context ?: return false

        if (format == "SVG") {
            return saveSvg(ctx, name, drawings, width, height, gridSize)
        }
        
        val bitmap = Bitmap.createBitmap(width * gridSize, height * gridSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        // Background
        paint.color = android.graphics.Color.WHITE
        canvas.drawRect(0f, 0f, (width * gridSize).toFloat(), (height * gridSize).toFloat(), paint)

        drawings.forEach { (coord, content) ->
            val left = (coord.first * gridSize).toFloat()
            val top = (coord.second * gridSize).toFloat()
            
            when (content) {
                is CellContent.Solid -> {
                    paint.color = content.color
                    canvas.drawRect(left, top, left + gridSize, top + gridSize, paint)
                }
                is CellContent.Stamped -> {
                    val stamp = StampsRegistry[content.stampName] ?: return@forEach
                    val pattern = stamp.pattern
                    val multiplier = stamp.gridSize.toFloat()
                    val subCellW = (gridSize.toFloat() * multiplier) / pattern[0].size
                    val subCellH = (gridSize.toFloat() * multiplier) / pattern.size
                    
                    canvas.save()
                    canvas.rotate(-content.rotation, left + (gridSize * multiplier) / 2f, top + (gridSize * multiplier) / 2f)
                    
                    content.backgroundColor?.let { bgColor ->
                        paint.color = bgColor
                        canvas.drawRect(left, top, left + gridSize * multiplier, top + gridSize * multiplier, paint)
                    }

                    paint.color = content.color
                    pattern.forEachIndexed { r, row ->
                        row.forEachIndexed { c, v ->
                            if (v != null) {
                                canvas.drawRect(
                                    left + c * subCellW,
                                    top + r * subCellH,
                                    left + (c + 1) * subCellW + 0.5f,
                                    top + (r + 1) * subCellH + 0.5f,
                                    paint
                                )
                            }
                        }
                    }
                    canvas.restore()
                }
            }
        }

        val compressFormat = if (format == "PNG") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        val mimeType = if (format == "PNG") "image/png" else "image/jpeg"
        val fileName = "$name.${format.lowercase()}"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/Kufixel")
            }
        }

        val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        return if (uri != null) {
            ctx.contentResolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(compressFormat, 100, outputStream)
            }
            true
        } else {
            false
        }
    }

    private fun saveSvg(ctx: Context, name: String, drawings: Map<Pair<Int, Int>, CellContent>, width: Int, height: Int, gridSize: Int): Boolean {
        val svg = StringBuilder()
        val w = width * gridSize
        val h = height * gridSize
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"$w\" height=\"$h\" viewBox=\"0 0 $w $h\">\n")
        svg.append("<rect width=\"100%\" height=\"100%\" fill=\"white\"/>\n")
        
        drawings.forEach { (coord, content) ->
            val left = coord.first * gridSize
            val top = coord.second * gridSize
            when (content) {
                is CellContent.Solid -> {
                    val colorHex = String.format("#%06X", (0xFFFFFF and content.color))
                    svg.append("<rect x=\"$left\" y=\"$top\" width=\"$gridSize\" height=\"$gridSize\" fill=\"$colorHex\"/>\n")
                }
                is CellContent.Stamped -> {
                    val stamp = StampsRegistry[content.stampName] ?: return@forEach
                    val pattern = stamp.pattern
                    val multiplier = stamp.gridSize.toFloat()
                    val subCellW = (gridSize.toFloat() * multiplier) / pattern[0].size
                    val subCellH = (gridSize.toFloat() * multiplier) / pattern.size
                    val colorHex = String.format("#%06X", (0xFFFFFF and content.color))
                    
                    val centerX = left + (gridSize * multiplier) / 2f
                    val centerY = top + (gridSize * multiplier) / 2f
                    
                    svg.append("<g transform=\"rotate(${-content.rotation} $centerX $centerY)\">\n")
                    
                    content.backgroundColor?.let { bgColor ->
                        val bgHex = String.format("#%06X", (0xFFFFFF and bgColor))
                        svg.append("<rect x=\"$left\" y=\"$top\" width=\"${gridSize * multiplier}\" height=\"${gridSize * multiplier}\" fill=\"$bgHex\"/>\n")
                    }

                    pattern.forEachIndexed { r, row ->
                        row.forEachIndexed { c, v ->
                            if (v != null) {
                                val sl = left + c * subCellW
                                val st = top + r * subCellH
                                svg.append("<rect x=\"$sl\" y=\"$st\" width=\"${subCellW + 0.5f}\" height=\"${subCellH + 0.5f}\" fill=\"$colorHex\"/>\n")
                            }
                        }
                    }
                    svg.append("</g>\n")
                }
            }
        }
        svg.append("</svg>")

        val fileName = "$name.svg"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/svg+xml")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/Kufixel")
            }
        }

        val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        return if (uri != null) {
            ctx.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(svg.toString().toByteArray())
            }
            true
        } else {
            false
        }
    }
}

actual fun getImageSaver(): ImageSaver = AndroidImageSaver
