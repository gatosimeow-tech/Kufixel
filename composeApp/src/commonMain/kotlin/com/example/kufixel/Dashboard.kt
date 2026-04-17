package com.example.kufixel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

@Composable
fun Dashboard(
    projects: MutableList<Project>,
    onNavigateToEditor: (Int, Int) -> Unit = { _, _ -> },
    onOpenProject: (Project) -> Unit = {},
    onOpenStorage: () -> Unit = {}
) {
    var showSizeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var projectToRename by remember { mutableStateOf<Project?>(null) }
    var projectToRemove by remember { mutableStateOf<Project?>(null) }

    if (showSizeDialog) {
        var width by remember { mutableStateOf("32") }
        var height by remember { mutableStateOf("32") }

        AlertDialog(
            onDismissRequest = { showSizeDialog = false },
            title = { Text("New Project", color = PrimaryTextIcons) },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedTextField(
                        value = width,
                        onValueChange = { if (it.length <= 3) width = it.filter { c -> c.isDigit() } },
                        label = { Text("Width") },
                        modifier = Modifier.width(80.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Text("x", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PrimaryTextIcons)
                    OutlinedTextField(
                        value = height,
                        onValueChange = { if (it.length <= 3) height = it.filter { c -> c.isDigit() } },
                        label = { Text("Height") },
                        modifier = Modifier.width(80.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                LabeledButton(
                    onClick = {
                        val w = width.toIntOrNull() ?: 32
                        val h = height.toIntOrNull() ?: 32
                        showSizeDialog = false
                        onNavigateToEditor(w, h)
                    },
                    label = "Create Project",
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTextIcons)
                ) {
                    Text("Create", color = Color.White)
                }
            },
            dismissButton = {
                LabeledTextButton(onClick = { showSizeDialog = false }, label = "Cancel") {
                    Text("Cancel", color = PrimaryTextIcons)
                }
            }
        )
    }

    if (projectToRename != null) {
        var newName by remember { mutableStateOf(projectToRename!!.name) }
        AlertDialog(
            onDismissRequest = { projectToRename = null },
            title = { Text("Rename Project", color = PrimaryTextIcons) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                LabeledButton(
                    onClick = {
                        val index = projects.indexOfFirst { it.id == projectToRename!!.id }
                        if (index != -1) {
                            projects[index] = projects[index].copy(name = newName)
                        }
                        projectToRename = null
                    },
                    label = "Rename",
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTextIcons)
                ) {
                    Text("Rename", color = Color.White)
                }
            },
            dismissButton = {
                LabeledTextButton(onClick = { projectToRename = null }, label = "Cancel") {
                    Text("Cancel", color = PrimaryTextIcons)
                }
            }
        )
    }

    if (projectToRemove != null) {
        AlertDialog(
            onDismissRequest = { projectToRemove = null },
            title = { Text("Delete Project", color = PrimaryTextIcons) },
            text = {
                Text("Are you sure you want to delete '${projectToRemove!!.name}'? This action cannot be undone.", color = PrimaryTextIcons)
            },
            confirmButton = {
                LabeledButton(
                    onClick = {
                        projects.removeAll { it.id == projectToRemove!!.id }
                        projectToRemove = null
                    },
                    label = "Delete",
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                LabeledTextButton(onClick = { projectToRemove = null }, label = "Cancel") {
                    Text("Cancel", color = PrimaryTextIcons)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MainWorkspaceBackground)
            .systemBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // (1 & 2) Logo and Title
        KufixelBrand(logoSize = 120.dp, fontSize = 36.sp)

        Spacer(modifier = Modifier.height(32.dp))

        // (3) Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ActionButton(
                icon = Icons.Default.Add,
                label = "New",
                backgroundColor = PrimaryTextIcons,
                onClick = { showSizeDialog = true }
            )
            ActionButton(
                icon = Icons.Default.Folder,
                label = "Open",
                backgroundColor = PrimaryTextIcons,
                onClick = { onOpenStorage() }
            )
            ActionButton(
                icon = Icons.Default.Info,
                label = "About",
                backgroundColor = PrimaryTextIcons,
                onClick = { showAboutDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // (4) Recent Project
        Text(
            text = "Recent Project",
            modifier = Modifier.align(Alignment.Start),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryTextIcons
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (projects.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("No projects yet", color = PrimaryTextIcons.copy(alpha = 0.5f))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(projects, key = { it.id }) { project ->
                    ProjectItem(
                        project = project,
                        onClick = { onOpenProject(project) },
                        onRemove = { projectToRemove = project },
                        onRename = { projectToRename = project }
                    )
                }
            }
        }
    }
}

@Composable
fun ActionButton(icon: ImageVector, label: String, backgroundColor: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LabeledIconButton(
            onClick = onClick,
            label = label,
            modifier = Modifier
                .size(56.dp)
                .background(backgroundColor, RoundedCornerShape(12.dp)),
            colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = Color.White)
        }
        Text(text = label, fontSize = 12.sp, color = PrimaryTextIcons)
    }
}

@Composable
fun ProjectItem(
    project: Project,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onRename: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        Column {
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            ) {
                if (project.drawings.isEmpty()) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = CanvasSurround,
                        modifier = Modifier.size(48.dp).align(Alignment.Center)
                    )
                } else {
                    ProjectThumbnail(project.drawings, modifier = Modifier.fillMaxSize().padding(8.dp))
                }

                // Overlay buttons
                Row(
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SmallOverlayButton(
                        icon = Icons.Default.Edit,
                        onClick = onRename,
                        label = "Rename",
                        color = Color.Gray
                    )
                    SmallOverlayButton(
                        icon = Icons.Default.Delete,
                        onClick = onRemove,
                        label = "Remove",
                        color = Color.Red.copy(alpha = 0.7f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = project.name,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = PrimaryTextIcons,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SmallOverlayButton(
    icon: ImageVector,
    onClick: () -> Unit,
    label: String,
    color: Color
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = color,
        modifier = Modifier.size(24.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun ProjectThumbnail(drawings: Map<Pair<Int, Int>, CellContent>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (drawings.isEmpty()) return@Canvas

        val minX = drawings.keys.minOf { it.first }
        val maxX = drawings.keys.maxOf { it.first }
        val minY = drawings.keys.minOf { it.second }
        val maxY = drawings.keys.maxOf { it.second }

        val width = maxX - minX + 1
        val height = maxY - minY + 1

        val drawAreaSize = size
        val cellSize = min(drawAreaSize.width / width, drawAreaSize.height / height)

        val offsetX = (drawAreaSize.width - width * cellSize) / 2f
        val offsetY = (drawAreaSize.height - height * cellSize) / 2f

        drawings.forEach { (coord, content) ->
            val rectTopLeft = Offset(
                offsetX + (coord.first - minX) * cellSize,
                offsetY + (coord.second - minY) * cellSize
            )

            when (content) {
                is CellContent.Solid -> {
                    drawRect(
                        color = Color(content.color),
                        topLeft = rectTopLeft,
                        size = Size(cellSize, cellSize)
                    )
                }
                is CellContent.Stamped -> {
                    val stamp = StampsRegistry[content.stampName] ?: HamzahV1
                    val pattern = stamp.pattern
                    val pRows = pattern.size
                    val pCols = pattern[0].size
                    
                    val multiplier = stamp.gridSize.toFloat()
                    val subCellSize = (cellSize * multiplier) / max(pRows, pCols)

                    // Draw background if present
                    content.backgroundColor?.let { bgColor ->
                        drawRect(
                            color = Color(bgColor),
                            topLeft = rectTopLeft,
                            size = Size(cellSize * multiplier, cellSize * multiplier)
                        )
                    }

                    // Simplified stamp drawing for thumbnail (no rotation)
                    pattern.forEachIndexed { rIdx, row ->
                        row.forEachIndexed { cIdx, value ->
                            if (value != null) {
                                drawRect(
                                    color = Color(content.color),
                                    topLeft = rectTopLeft + Offset(cIdx * subCellSize, rIdx * subCellSize),
                                    size = Size(subCellSize + 0.5f, subCellSize + 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
