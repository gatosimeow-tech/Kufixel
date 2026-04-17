package com.example.kufixel

import platform.UIKit.UIDevice

import platform.UIKit.*
import platform.Foundation.*
import platform.CoreGraphics.*
import platform.Photos.*
import kotlinx.cinterop.*
import kotlin.math.PI

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

@OptIn(ExperimentalForeignApi::class)
object IosImageSaver : ImageSaver {
    override fun saveImage(
        name: String,
        drawings: Map<Pair<Int, Int>, CellContent>,
        width: Int,
        height: Int,
        gridSize: Int,
        format: String
    ): Boolean {
        if (format == "SVG") {
            return saveSvg(name, drawings, width, height, gridSize)
        }

        val totalWidth = (width * gridSize).toDouble()
        val totalHeight = (height * gridSize).toDouble()

        UIGraphicsBeginImageContextWithOptions(CGSizeMake(totalWidth, totalHeight), true, 1.0)
        val context = UIGraphicsGetCurrentContext() ?: return false

        // Background
        CGContextSetFillColorWithColor(context, UIColor.whiteColor.CGColor)
        CGContextFillRect(context, CGRectMake(0.0, 0.0, totalWidth, totalHeight))

        drawings.forEach { (coord, content) ->
            val left = (coord.first * gridSize).toDouble()
            val top = (coord.second * gridSize).toDouble()

            when (content) {
                is CellContent.Solid -> {
                    CGContextSetFillColorWithColor(context, content.color.toUIColor().CGColor)
                    CGContextFillRect(context, CGRectMake(left, top, gridSize.toDouble(), gridSize.toDouble()))
                }
                is CellContent.Stamped -> {
                    val stamp = StampsRegistry[content.stampName] ?: return@forEach
                    val pattern = stamp.pattern
                    val multiplier = stamp.gridSize.toDouble()
                    val subCellW = (gridSize.toDouble() * multiplier) / pattern[0].size
                    val subCellH = (gridSize.toDouble() * multiplier) / pattern.size

                    CGContextSaveGState(context)
                    
                    val centerX = left + (gridSize.toDouble() * multiplier) / 2.0
                    val centerY = top + (gridSize.toDouble() * multiplier) / 2.0
                    CGContextTranslateCTM(context, centerX, centerY)
                    CGContextRotateCTM(context, (-content.rotation * PI / 180.0))
                    CGContextTranslateCTM(context, -centerX, -centerY)

                    content.backgroundColor?.let { bgColor ->
                        CGContextSetFillColorWithColor(context, bgColor.toUIColor().CGColor)
                        CGContextFillRect(context, CGRectMake(left, top, gridSize.toDouble() * multiplier, gridSize.toDouble() * multiplier))
                    }

                    CGContextSetFillColorWithColor(context, content.color.toUIColor().CGColor)
                    pattern.forEachIndexed { r, row ->
                        row.forEachIndexed { c, v ->
                            if (v != null) {
                                CGContextFillRect(
                                    context,
                                    CGRectMake(
                                        left + c * subCellW,
                                        top + r * subCellH,
                                        subCellW + 0.5,
                                        subCellH + 0.5
                                    )
                                )
                            }
                        }
                    }
                    CGContextRestoreGState(context)
                }
            }
        }

        val image = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()

        if (image == null) return false
        val uiImage = image

        // Save to Photos
        PHPhotoLibrary.requestAuthorization { status ->
            if (status == PHAuthorizationStatusAuthorized) {
                PHPhotoLibrary.sharedPhotoLibrary().performChanges({
                    PHAssetChangeRequest.creationRequestForAssetFromImage(uiImage)
                }, completionHandler = { success, error ->
                    if (!success) {
                        println("Error saving image: ${error?.localizedDescription}")
                    }
                })
            }
        }

        return true
    }

    private fun saveSvg(name: String, drawings: Map<Pair<Int, Int>, CellContent>, width: Int, height: Int, gridSize: Int): Boolean {
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
                    val colorHex = content.color.toHex6()
                    svg.append("<rect x=\"$left\" y=\"$top\" width=\"$gridSize\" height=\"$gridSize\" fill=\"$colorHex\"/>\n")
                }
                is CellContent.Stamped -> {
                    val stamp = StampsRegistry[content.stampName] ?: return@forEach
                    val pattern = stamp.pattern
                    val multiplier = stamp.gridSize.toFloat()
                    val subCellW = (gridSize.toFloat() * multiplier) / pattern[0].size
                    val subCellH = (gridSize.toFloat() * multiplier) / pattern.size
                    val colorHex = content.color.toHex6()
                    
                    val centerX = left + (gridSize * multiplier) / 2f
                    val centerY = top + (gridSize * multiplier) / 2f
                    
                    svg.append("<g transform=\"rotate(${-content.rotation} $centerX $centerY)\">\n")
                    
                    content.backgroundColor?.let { bgColor ->
                        val bgHex = bgColor.toHex6()
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
        val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        val documentsDirectory = paths.first() as String
        val filePath = "$documentsDirectory/$fileName"
        
        return try {
            (svg.toString() as NSString).writeToFile(filePath, true, NSUTF8StringEncoding, null)
            println("SVG saved to: $filePath")
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun Int.toUIColor(): UIColor {
        val alpha = ((this shr 24) and 0xFF) / 255.0
        val red = ((this shr 16) and 0xFF) / 255.0
        val green = ((this shr 8) and 0xFF) / 255.0
        val blue = (this and 0xFF) / 255.0
        return UIColor.colorWithRed(red, green, blue, alpha)
    }

    private fun Int.toHex6(): String {
        val r = (this shr 16) and 0xFF
        val g = (this shr 8) and 0xFF
        val b = this and 0xFF
        return "#${r.toHex()}${g.toHex()}${b.toHex()}"
    }

    private fun Int.toHex(): String = this.toString(16).padStart(2, '0').uppercase()
}


actual fun getImageSaver(): ImageSaver = IosImageSaver

