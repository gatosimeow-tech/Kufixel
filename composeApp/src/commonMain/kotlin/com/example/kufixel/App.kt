package com.example.kufixel

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

sealed class Screen {
    object Dashboard : Screen()
    data class Editor(val project: Project) : Screen()
}

@Composable
fun App(onOpenStorage: () -> Unit = {}) {
    MaterialTheme {
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
        val savedProjects = remember { mutableStateListOf<Project>() }

        when (val screen = currentScreen) {
            is Screen.Dashboard -> {
                Dashboard(
                    projects = savedProjects,
                    onNavigateToEditor = { width, height ->
                        val newProject = Project(
                            id = (0..1000000).random().toString(),
                            name = "New Pattern",
                            width = width,
                            height = height
                        )
                        currentScreen = Screen.Editor(newProject)
                    },
                    onOpenProject = { project ->
                        currentScreen = Screen.Editor(project)
                    },
                    onOpenStorage = onOpenStorage
                )
            }
            is Screen.Editor -> {
                Editor(
                    project = screen.project,
                    onBack = { currentScreen = Screen.Dashboard },
                    onSave = { updatedProject ->
                        val index = savedProjects.indexOfFirst { it.id == updatedProject.id }
                        if (index != -1) {
                            savedProjects[index] = updatedProject
                        } else {
                            savedProjects.add(0, updatedProject)
                        }
                        currentScreen = Screen.Dashboard
                    }
                )
            }
        }
    }
}
