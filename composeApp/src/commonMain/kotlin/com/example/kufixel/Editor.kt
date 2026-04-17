package com.example.kufixel

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

enum class Tool { Pencil, Eraser, Stamp, Fill }

@Composable
fun StampIcon(stampName: String, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val stamp = StampsRegistry[stampName] ?: HamzahV1
        val pattern = stamp.pattern
        val rows = pattern.size
        val cols = pattern[0].size
        val cellW = size.width / cols
        val cellH = size.height / rows
        
        pattern.forEachIndexed { rowIdx, row ->
            row.forEachIndexed { colIdx, value ->
                if (value != null) {
                    drawRect(
                        color = color,
                        topLeft = Offset(colIdx * cellW, rowIdx * cellH),
                        size = Size(cellW, cellH)
                    )
                }
            }
        }
    }
}

fun Color.toHex(): String = toArgb().toUInt().toString(16).padStart(8, '0').uppercase()

fun String.toColor(): Color? {
    return try {
        val longVal = if (startsWith("#")) substring(1).toLong(16) else toLong(16)
        Color(longVal.toInt())
    } catch (e: Exception) {
        null
    }
}

@Composable
fun Editor(project: Project, onBack: () -> Unit, onSave: (Project) -> Unit) {
    // Canvas Settings
    var canvasWidthStr by remember { mutableStateOf(project.width?.toString() ?: "") }
    var canvasHeightStr by remember { mutableStateOf(project.height?.toString() ?: "") }
    var gridSizeStr by remember { mutableStateOf(project.gridSize.toString()) }
    var showGrid by remember { mutableStateOf(project.showGrid) }
    var isSolidBackground by remember { mutableStateOf(true) }
    
    // Tools State
    var selectedTool by remember { mutableStateOf(Tool.Pencil) }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var selectedStamp by remember { mutableStateOf("Kaaf") }
    
    // Drawing Data
    val drawings = remember { mutableStateMapOf<Pair<Int, Int>, CellContent>().apply { putAll(project.drawings) } }
    val history = remember { mutableStateListOf<Map<Pair<Int, Int>, CellContent>>() }
    val redoStack = remember { mutableStateListOf<Map<Pair<Int, Int>, CellContent>>() }

    // Transformation State (Pan, Zoom, Rotation)
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotation by remember { mutableStateOf(0f) }
    var workspaceSize by remember { mutableStateOf(Size.Zero) }

    // Pattern Size Logic
    val patternSize = remember(drawings.size) {
        if (drawings.isEmpty()) "0 x 0"
        else {
            val keys = drawings.keys
            val minX = keys.minOf { it.first }
            val maxX = keys.maxOf { it.first }
            val minY = keys.minOf { it.second }
            val maxY = keys.maxOf { it.second }
            "${(maxX - minX + 1)} x ${(maxY - minY + 1)}"
        }
    }

    // Dialog State
    var showSaveDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showColorPickerDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf<String?>(null) } // PNG, JPG, SVG
    var showAboutDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var projectName by remember { mutableStateOf(project.name) }

    var exportName by remember { mutableStateOf(project.name) }
    var exportWidth by remember { mutableStateOf("") }
    var exportHeight by remember { mutableStateOf("") }
    var keepRatio by remember { mutableStateOf(true) }

    val gSize = gridSizeStr.toIntOrNull() ?: 20
    val cWidth = canvasWidthStr.toIntOrNull() ?: 1
    val cHeight = canvasHeightStr.toIntOrNull() ?: 1

    LaunchedEffect(showExportDialog) {
        if (showExportDialog != null) {
            exportName = project.name
            exportWidth = (cWidth * gSize * 4).toString()
            exportHeight = (cHeight * gSize * 4).toString()
        }
    }

    fun saveToHistory() {
        history.add(drawings.toMap())
        if (history.size > 100) history.removeAt(0)
        redoStack.clear()
    }

    fun undo() {
        if (history.isNotEmpty()) {
            redoStack.add(drawings.toMap())
            val previous = history.removeAt(history.size - 1)
            drawings.clear()
            drawings.putAll(previous)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            history.add(drawings.toMap())
            val next = redoStack.removeAt(redoStack.size - 1)
            drawings.clear()
            drawings.putAll(next)
        }
    }

    // Screen-fit logic
    fun scaleToFitCanvas(parentWidth: Float, parentHeight: Float) {
        if (cWidth > 0 && cHeight > 0 && parentWidth > 0 && parentHeight > 0) {
            val canvasW = cWidth.toFloat() * gSize
            val canvasH = cHeight.toFloat() * gSize
            val scaleW = (parentWidth * 0.9f) / canvasW
            val scaleH = (parentHeight * 0.9f) / canvasH
            scale = min(scaleW, scaleH)
            rotation = 0f
            offset = Offset.Zero
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        
        LaunchedEffect(cWidth, cHeight, gSize, workspaceSize) {
            if (workspaceSize.width > 0 && workspaceSize.height > 0) {
                scaleToFitCanvas(workspaceSize.width, workspaceSize.height)
            }
        }

        if (showClearConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearConfirmDialog = false },
                title = { Text("Clear Canvas", color = PrimaryTextIcons) },
                text = { Text("Are you sure you want to clear the entire canvas? This action can be undone.", color = PrimaryTextIcons) },
                confirmButton = {
                    LabeledButton(onClick = { showClearConfirmDialog = false; saveToHistory(); drawings.clear() }, label = "Confirm Clear", colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                        Text("Clear", color = Color.White)
                    }
                },
                dismissButton = {
                    LabeledTextButton(onClick = { showClearConfirmDialog = false }, label = "Cancel") { Text("Cancel", color = PrimaryTextIcons) }
                }
            )
        }

        if (showExportDialog != null) {
            val imageSaver = getImageSaver()

            AlertDialog(
                onDismissRequest = { showExportDialog = null },
                title = { Text("Export as ${showExportDialog}", color = PrimaryTextIcons) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = exportName, onValueChange = { exportName = it }, label = { Text("File Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = exportWidth,
                                onValueChange = { 
                                    exportWidth = it.filter { c -> c.isDigit() }
                                    if (keepRatio && exportWidth.isNotEmpty()) {
                                        val w = exportWidth.toIntOrNull() ?: 1
                                        exportHeight = (w * cHeight / cWidth).toString()
                                    }
                                },
                                label = { Text("Width") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = exportHeight,
                                onValueChange = { 
                                    exportHeight = it.filter { c -> c.isDigit() }
                                    if (keepRatio && exportHeight.isNotEmpty()) {
                                        val h = exportHeight.toIntOrNull() ?: 1
                                        exportWidth = (h * cWidth / cHeight).toString()
                                    }
                                },
                                label = { Text("Height") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = keepRatio, onCheckedChange = { keepRatio = it })
                            Text("Keep Ratio", color = PrimaryTextIcons)
                        }
                    }
                },
                confirmButton = {
                    LabeledButton(onClick = { 
                        val finalW = exportWidth.toIntOrNull() ?: (cWidth * gSize)
                        val finalGSize = max(1, finalW / cWidth)
                        val success = imageSaver.saveImage(
                            name = exportName,
                            drawings = drawings.toMap(),
                            width = cWidth,
                            height = cHeight,
                            gridSize = finalGSize,
                            format = showExportDialog!!
                        )
                        showExportDialog = null 
                        if (success) showSuccessDialog = true
                    }, label = "Export Now", colors = ButtonDefaults.buttonColors(containerColor = PrimaryTextIcons)) {
                        Text("Export", color = Color.White)
                    }
                },
                dismissButton = {
                    LabeledTextButton(onClick = { showExportDialog = null }, label = "Cancel") { Text("Cancel", color = PrimaryTextIcons) }
                }
            )
        }

        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showSuccessDialog = false },
                title = { Text("Export Successful", color = PrimaryTextIcons) },
                text = { Text("Your artwork has been saved successfully.", color = PrimaryTextIcons) },
                confirmButton = {
                    TextButton(onClick = { showSuccessDialog = false }) {
                        Text("OK", color = PrimaryTextIcons)
                    }
                }
            )
        }

        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                title = { Text("About KUFIXEL", color = PrimaryTextIcons) },
                text = {
                    Column {
                        Text(
                            "KUFIXEL is a place where you can draw square kufic or pixels. You can easily create, save and export your work.",
                            color = PrimaryTextIcons
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Copyright © 2026 KUFIXEL by Fahmi Jamalludin. All rights reserved.",
                            fontSize = 12.sp,
                            color = PrimaryTextIcons.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "KUFIXEL v1.0",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = PrimaryTextIcons
                        )
                    }
                },
                confirmButton = {
                    LabeledTextButton(onClick = { showAboutDialog = false }, label = "Close") {
                        Text("OK", color = PrimaryTextIcons)
                    }
                }
            )
        }

        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text("Save Project", color = PrimaryTextIcons) },
                text = {
                    OutlinedTextField(
                        value = projectName,
                        onValueChange = { projectName = it },
                        label = { Text("Project Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    LabeledButton(
                        onClick = {
                            showSaveDialog = false
                            onSave(project.copy(
                                name = projectName,
                                width = cWidth,
                                height = cHeight,
                                gridSize = gSize,
                                showGrid = showGrid,
                                drawings = drawings.toMap()
                            ))
                        },
                        label = "Save",
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTextIcons)
                    ) {
                        Text("OK", color = Color.White)
                    }
                },
                dismissButton = {
                    LabeledTextButton(onClick = { showSaveDialog = false }, label = "Cancel") {
                        Text("CANCEL", color = PrimaryTextIcons)
                    }
                }
            )
        }

        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = { Text("Canvas Settings", color = PrimaryTextIcons) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = canvasWidthStr,
                                onValueChange = { canvasWidthStr = it },
                                label = { Text("Width") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = canvasHeightStr,
                                onValueChange = { canvasHeightStr = it },
                                label = { Text("Height") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        OutlinedTextField(
                            value = gridSizeStr,
                            onValueChange = { gridSizeStr = it },
                            label = { Text("Grid Cell Size") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    LabeledButton(
                        onClick = { showSettingsDialog = false },
                        label = "Close Settings",
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTextIcons)
                    ) {
                        Text("Close", color = Color.White)
                    }
                }
            )
        }

        if (showColorPickerDialog) {
            var tempColor by remember { mutableStateOf(selectedColor) }
            var hexString by remember { mutableStateOf(tempColor.toHex()) }
            
            AlertDialog(
                onDismissRequest = { showColorPickerDialog = false },
                title = { Text("Color Picker", color = PrimaryTextIcons) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. Preview Color Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .background(tempColor, RoundedCornerShape(8.dp))
                                .border(2.dp, BorderColor, RoundedCornerShape(8.dp))
                        )

                        // 2. Hex value (editable)
                        OutlinedTextField(
                            value = hexString,
                            onValueChange = { newValue: String ->
                                val filtered = newValue.uppercase().filter { it in "0123456789ABCDEF" }.take(8)
                                hexString = filtered
                                if (filtered.length == 8) {
                                    filtered.toColor()?.let { tempColor = it }
                                }
                            },
                            label = { Text("Hex Value (AARRGGBB)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // 3. Instant Color (Palette)
                        Text("Instant Colors", style = MaterialTheme.typography.labelMedium, color = PrimaryTextIcons)
                        val palette = listOf(
                            Color.Black, Color.White, Color.Gray, Color.Red, 
                            Color.Green, Color.Blue, Color.Yellow, Color.Cyan, 
                            Color.Magenta, Color(0xFFFFA500), Color(0xFF800080), Color(0xFF8B4513)
                        )
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            palette.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .size(36.dp)
                                        .background(color, CircleShape)
                                        .border(1.dp, BorderColor, CircleShape)
                                        .clickable { 
                                            tempColor = color
                                            hexString = color.toHex()
                                        }
                                )
                            }
                        }

                        // 4. HSV Sliders
                        Text("Fine Tune", style = MaterialTheme.typography.labelMedium, color = PrimaryTextIcons)
                        
                        val hsv = remember(tempColor) {
                            val r = tempColor.red
                            val g = tempColor.green
                            val b = tempColor.blue
                            val max = maxOf(r, maxOf(g, b))
                            val min = minOf(r, minOf(g, b))
                            val delta = max - min
                            
                            var h = 0f
                            if (delta != 0f) {
                                h = when (max) {
                                    r -> 60 * (((g - b) / delta) % 6)
                                    g -> 60 * (((b - r) / delta) + 2)
                                    else -> 60 * (((r - g) / delta) + 4)
                                }
                            }
                            if (h < 0) h += 360f
                            
                            val s = if (max == 0f) 0f else delta / max
                            val v = max
                            Triple(h, s, v)
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Hue: ${hsv.first.toInt()}°", style = MaterialTheme.typography.labelSmall)
                            Slider(
                                value = hsv.first,
                                onValueChange = { 
                                    tempColor = Color.hsv(it, hsv.second, hsv.third)
                                    hexString = tempColor.toHex()
                                },
                                valueRange = 0f..360f,
                                colors = SliderDefaults.colors(thumbColor = tempColor, activeTrackColor = tempColor)
                            )
                            
                            Text("Saturation: ${(hsv.second * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                            Slider(
                                value = hsv.second,
                                onValueChange = { 
                                    tempColor = Color.hsv(hsv.first, it, hsv.third)
                                    hexString = tempColor.toHex()
                                },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(thumbColor = tempColor, activeTrackColor = tempColor)
                            )

                            Text("Value: ${(hsv.third * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                            Slider(
                                value = hsv.third,
                                onValueChange = { 
                                    tempColor = Color.hsv(hsv.first, hsv.second, it)
                                    hexString = tempColor.toHex()
                                },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(thumbColor = tempColor, activeTrackColor = tempColor)
                            )
                        }
                    }
                },
                confirmButton = {
                    LabeledButton(
                        onClick = { 
                            selectedColor = tempColor
                            showColorPickerDialog = false 
                        }, 
                        label = "Done", 
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTextIcons)
                    ) {
                        Text("Done", color = Color.White)
                    }
                },
                dismissButton = {
                    LabeledTextButton(
                        onClick = { showColorPickerDialog = false }, 
                        label = "Cancel"
                    ) {
                        Text("Cancel", color = PrimaryTextIcons)
                    }
                }
            )
        }

@OptIn(ExperimentalMaterial3Api::class)
        Scaffold(
            topBar = {
                Surface(color = HeaderPanels, tonalElevation = 2.dp) {
                    Column(modifier = Modifier.statusBarsPadding()) {
                        TopAppBar(
                            title = { 
                                KufixelBrand(
                                    logoSize = 32.dp,
                                    fontSize = 11.sp,
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    spacing = 0.dp
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                actionIconContentColor = PrimaryTextIcons
                            ),
                            actions = {
                                LabeledIconButton(onClick = { /* TODO */ }, label = "Book") {
                                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Book")
                                }
                                LabeledIconButton(onClick = { isSolidBackground = !isSolidBackground }, label = "Canvas Mode") {
                                    Icon(if (isSolidBackground) Icons.Default.Square else Icons.Default.Grid4x4, contentDescription = "Canvas Type")
                                }
                                LabeledIconButton(onClick = { showGrid = !showGrid }, label = "Grid Toggle") {
                                    Icon(if (showGrid) Icons.Default.GridOn else Icons.Default.GridOff, contentDescription = "Grid Toggle")
                                }
                                LabeledIconButton(onClick = { showSettingsDialog = true }, label = "Settings") {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                                }
                                LabeledIconButton(onClick = { showClearConfirmDialog = true }, label = "Clear") {
                                    Icon(Icons.Default.Delete, contentDescription = "Clear")
                                }
                                
                                var showMoreMenu by remember { mutableStateOf(false) }
                                LabeledIconButton(onClick = { showMoreMenu = true }, label = "More") {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                                    DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }, modifier = Modifier.background(HeaderPanels)) {
                                        DropdownMenuItem(text = { Text("Save Project", color = PrimaryTextIcons) }, onClick = { showMoreMenu = false; showSaveDialog = true })
                                        DropdownMenuItem(text = { Text("Close Project", color = PrimaryTextIcons) }, onClick = { showMoreMenu = false; onBack() })
                                        HorizontalDivider(color = BorderColor)
                                        DropdownMenuItem(text = { Text("Export PNG", color = PrimaryTextIcons) }, onClick = { showMoreMenu = false; showExportDialog = "PNG" })
                                        DropdownMenuItem(text = { Text("Export JPG", color = PrimaryTextIcons) }, onClick = { showMoreMenu = false; showExportDialog = "JPG" })
                                        DropdownMenuItem(text = { Text("Export SVG", color = PrimaryTextIcons) }, onClick = { showMoreMenu = false; showExportDialog = "SVG" })
                                        HorizontalDivider(color = BorderColor)
                                        DropdownMenuItem(text = { Text("About", color = PrimaryTextIcons) }, onClick = { showMoreMenu = false; showAboutDialog = true })
                                    }
                                }
                            }
                        )
                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), contentAlignment = Alignment.Center) {
                            Surface(color = CanvasSurround, shape = RoundedCornerShape(4.dp)) {
                                Text(text = "Pattern Size: $patternSize", modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = PrimaryTextIcons)
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Surface(
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                    color = HeaderPanels,
                    contentColor = PrimaryTextIcons
                ) {
                    Column {
                        HorizontalDivider(color = BorderColor)
                        // Top layer: Navigation & View Controls
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp).fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabeledIconButton(onClick = { undo() }, label = "Undo", enabled = history.isNotEmpty()) {
                                Icon(Icons.AutoMirrored.Filled.Undo, null)
                            }
                            LabeledIconButton(onClick = { redo() }, label = "Redo", enabled = redoStack.isNotEmpty()) {
                                Icon(Icons.AutoMirrored.Filled.Redo, null)
                            }
                            VerticalDivider(modifier = Modifier.height(24.dp), color = BorderColor)
                            LabeledIconButton(onClick = { scale *= 1.2f }, label = "Zoom In") { Icon(Icons.Default.ZoomIn, null) }
                            LabeledIconButton(onClick = { scaleToFitCanvas(workspaceSize.width, workspaceSize.height) }, label = "Scale to Fit") { 
                                Icon(Icons.Default.Fullscreen, null) 
                            }
                            LabeledIconButton(onClick = { scale /= 1.2f }, label = "Zoom Out") { Icon(Icons.Default.ZoomOut, null) }
                            VerticalDivider(modifier = Modifier.height(24.dp), color = BorderColor)
                            LabeledIconButton(onClick = { rotation = (rotation - 45f) % 360f }, label = "Rotate Left") { 
                                Icon(Icons.AutoMirrored.Filled.RotateLeft, null) 
                            }
                            LabeledIconButton(onClick = { rotation = (rotation + 45f) % 360f }, label = "Rotate Right") { 
                                Icon(Icons.AutoMirrored.Filled.RotateRight, null) 
                            }
                        }

                        HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))

                        // Bottom layer: Drawing Tools
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(selectedColor).border(2.dp, if (selectedColor == Color.White) Color.Black else Color.White, CircleShape).clickable { showColorPickerDialog = true })

                            LabeledIconButton(
                                onClick = { selectedTool = Tool.Pencil },
                                label = "Pencil",
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (selectedTool == Tool.Pencil) Color.Black else Color.Transparent,
                                    contentColor = if (selectedTool == Tool.Pencil) Color.White else PrimaryTextIcons
                                )
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Pencil")
                            }

                            var showStampMenu by remember { mutableStateOf(false) }
                            Box {
                                LabeledIconButton(
                                    onClick = { 
                                        if (selectedTool == Tool.Stamp) showStampMenu = true 
                                        else { selectedTool = Tool.Stamp; showStampMenu = true }
                                    },
                                    label = "Stamp",
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = if (selectedTool == Tool.Stamp) Color.Black else Color.Transparent,
                                        contentColor = if (selectedTool == Tool.Stamp) Color.White else PrimaryTextIcons
                                    )
                                ) {
                                    StampIcon(selectedStamp, color = if (selectedTool == Tool.Stamp) Color.White else PrimaryTextIcons)
                                }
                                DropdownMenu(expanded = showStampMenu, onDismissRequest = { showStampMenu = false }, modifier = Modifier.background(HeaderPanels)) {
                                    StampsRegistry.keys.forEach { name ->
                                        DropdownMenuItem(text = { Text(name, color = PrimaryTextIcons) }, leadingIcon = { StampIcon(name, PrimaryTextIcons) }, onClick = { selectedStamp = name; showStampMenu = false } )
                                    }
                                }
                            }

                            LabeledIconButton(
                                onClick = { selectedTool = Tool.Fill },
                                label = "Fill Bucket",
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (selectedTool == Tool.Fill) Color.Black else Color.Transparent,
                                    contentColor = if (selectedTool == Tool.Fill) Color.White else PrimaryTextIcons
                                )
                            ) {
                                Icon(Icons.Default.FormatColorFill, contentDescription = "Fill Bucket")
                            }

                            LabeledIconButton(
                                onClick = { selectedTool = Tool.Eraser },
                                label = "Eraser",
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (selectedTool == Tool.Eraser) Color.Black else Color.Transparent,
                                    contentColor = if (selectedTool == Tool.Eraser) Color.White else PrimaryTextIcons
                                )
                            ) {
                                Icon(Icons.Default.CleaningServices, contentDescription = "Eraser")
                            }
                        }
                    }
                }
            },
            containerColor = MainWorkspaceBackground
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(CanvasSurround)
                    .clipToBounds()
                    .onGloballyPositioned { coordinates ->
                        workspaceSize = Size(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())
                    }
                    .pointerInput(selectedTool, gSize, cWidth, cHeight, selectedStamp, selectedColor, isSolidBackground) {
                        awaitEachGesture {
                            var isTransforming = false
                            var drawingStartedInThisGesture = false
                            while (true) {
                                val event = awaitPointerEvent()
                                val pointers = event.changes.filter { it.pressed }
                                
                                if (pointers.size >= 2 || event.changes.size >= 2) {
                                    if (!isTransforming && drawingStartedInThisGesture) {
                                        undo()
                                        drawingStartedInThisGesture = false
                                    }
                                    isTransforming = true
                                }

                                if (isTransforming) {
                                    val zoomChange = event.calculateZoom()
                                    val rotationChange = event.calculateRotation()
                                    val panChange = event.calculatePan()
                                    
                                    if (zoomChange != 1f || rotationChange != 0f || panChange != Offset.Zero) {
                                        scale = (scale * zoomChange).coerceIn(0.05f, 20f)
                                        rotation = (rotation + rotationChange) % 360f
                                        offset += panChange
                                    }
                                    event.changes.forEach { it.consume() }
                                } else if (pointers.size == 1) {
                                    val change = pointers[0]
                                    if (change.changedToDown()) {
                                        saveToHistory()
                                        drawingStartedInThisGesture = true
                                    }
                                    
                                    val localPos = screenToLocalCoordinate(change.position, scale, offset, rotation, workspaceSize, cWidth, cHeight, gSize)
                                    handleDrawingInput(localPos, gSize, cWidth, cHeight, rotation, selectedTool, selectedColor, selectedStamp, isSolidBackground, drawings)
                                    change.consume()
                                }

                                if (pointers.isEmpty()) {
                                    if (isTransforming) {
                                        rotation = (round(rotation / 45f) * 45f) % 360f
                                    }
                                    isTransforming = false
                                    break
                                }
                            }
                        }
                    }
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y,
                            rotationZ = rotation,
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        )
                ) {
                    val canvasW = cWidth.toFloat() * gSize
                    val canvasH = cHeight.toFloat() * gSize
                    val startX = (size.width - canvasW) / 2f
                    val startY = (size.height - canvasH) / 2f

                    drawRect(
                        color = if (isSolidBackground) Color.White else Color.Transparent,
                        topLeft = Offset(startX, startY),
                        size = Size(canvasW, canvasH)
                    )

                    if (showGrid) {
                        val gridColor = Color.LightGray.copy(alpha = 0.5f)
                        for (i in 0..cWidth) drawLine(gridColor, Offset(startX + i * gSize, startY), Offset(startX + i * gSize, startY + canvasH), strokeWidth = 1f / scale)
                        for (i in 0..cHeight) drawLine(gridColor, Offset(startX, startY + i * gSize), Offset(startX + canvasW, startY + i * gSize), strokeWidth = 1f / scale)
                    }

                    // Draw background solid cells first
                    drawings.forEach { (coord, content) ->
                        if (content is CellContent.Solid) {
                            val topLeft = Offset(startX + coord.first * gSize, startY + coord.second * gSize)
                            drawRect(color = Color(content.color), topLeft = topLeft, size = Size(gSize.toFloat(), gSize.toFloat()))
                        }
                    }

                    // Draw stamps on top of solid cells
                    drawings.forEach { (coord, content) ->
                        if (content is CellContent.Stamped) {
                            val topLeft = Offset(startX + coord.first * gSize, startY + coord.second * gSize)
                            val stamp = StampsRegistry[content.stampName] ?: HamzahV1
                            val pattern = stamp.pattern
                            val multiplier = stamp.gridSize.toFloat()
                            val subCellW = (gSize.toFloat() * multiplier) / pattern[0].size
                            val subCellH = (gSize.toFloat() * multiplier) / pattern.size
                            rotate(-content.rotation, pivot = topLeft + Offset((gSize * multiplier) / 2f, (gSize * multiplier) / 2f)) {
                                // Draw background if present
                                content.backgroundColor?.let { bgColor ->
                                    drawRect(
                                        color = Color(bgColor),
                                        topLeft = topLeft,
                                        size = Size(gSize.toFloat() * multiplier, gSize.toFloat() * multiplier)
                                    )
                                }

                                pattern.forEachIndexed { r, row ->
                                    row.forEachIndexed { c, v ->
                                        if (v != null) {
                                            drawRect(
                                                color = Color(content.color),
                                                topLeft = topLeft + Offset(c * subCellW, r * subCellH),
                                                size = Size(subCellW + 0.5f, subCellH + 0.5f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun screenToLocalCoordinate(screenPos: Offset, scale: Float, offset: Offset, rotation: Float, workspaceSize: Size, cWidth: Int, cHeight: Int, gSize: Int): Offset {
    val pivot = Offset(workspaceSize.width / 2f, workspaceSize.height / 2f)
    val p = screenPos - offset - pivot
    val rad = -rotation * PI.toFloat() / 180f
    val localFromPivot = Offset((p.x * cos(rad) - p.y * sin(rad)) / scale, (p.x * sin(rad) + p.y * cos(rad)) / scale)
    val localInWorkspace = localFromPivot + pivot
    val startX = (workspaceSize.width - cWidth * gSize) / 2f
    val startY = (workspaceSize.height - cHeight * gSize) / 2f
    return Offset(localInWorkspace.x - startX, localInWorkspace.y - startY)
}

private fun handleDrawingInput(pos: Offset, gSize: Int, cWidth: Int?, cHeight: Int?, rotation: Float, tool: Tool, color: Color, stampName: String, isSolidBackground: Boolean, drawings: MutableMap<Pair<Int, Int>, CellContent>) {
    val stamp = StampsRegistry[stampName]
    val offsetCenter = if (stamp != null) (stamp.gridSize - 1) / 2 else 0
    val gridX = floor(pos.x / gSize).toInt() - offsetCenter
    val gridY = floor(pos.y / gSize).toInt() - offsetCenter
    if (gridX >= -2 && gridY >= -2 && (cWidth == null || gridX < (cWidth ?: 1000) + 1) && (cHeight == null || gridY < (cHeight ?: 1000) + 1)) {
        when (tool) {
            Tool.Pencil -> {
                val gx = floor(pos.x / gSize).toInt()
                val gy = floor(pos.y / gSize).toInt()
                if (gx >= 0 && gy >= 0 && (cWidth == null || gx < cWidth) && (cHeight == null || gy < cHeight)) drawings[gx to gy] = CellContent.Solid(color.toArgb())
            }
            Tool.Eraser -> {
                val gx = floor(pos.x / gSize).toInt()
                val gy = floor(pos.y / gSize).toInt()
                drawings.remove(gx to gy)
            }
            Tool.Stamp -> drawings[gridX to gridY] = CellContent.Stamped(stampName, color.toArgb(), backgroundColor = if (isSolidBackground) null else Color.White.toArgb(), rotation = rotation)
            Tool.Fill -> {
                val gx = floor(pos.x / gSize).toInt()
                val gy = floor(pos.y / gSize).toInt()
                if (cWidth != null && cHeight != null && gx in 0 until cWidth && gy in 0 until cHeight) {
                    val target = drawings[gx to gy]
                    val replacement = CellContent.Solid(color.toArgb())

                    if (target != replacement) {
                        val stack = mutableListOf(gx to gy)
                        val visited = mutableSetOf<Pair<Int, Int>>()
                        while (stack.isNotEmpty()) {
                            val p = stack.removeAt(stack.size - 1)
                            if (p.first in 0 until cWidth && p.second in 0 until cHeight && drawings[p.first to p.second] == target && p !in visited) {
                                drawings[p.first to p.second] = replacement
                                visited.add(p)
                                stack.add(p.first + 1 to p.second)
                                stack.add(p.first - 1 to p.second)
                                stack.add(p.first to p.second + 1)
                                stack.add(p.first to p.second - 1)
                            }
                        }
                    }
                }
            }
        }
    }
}
