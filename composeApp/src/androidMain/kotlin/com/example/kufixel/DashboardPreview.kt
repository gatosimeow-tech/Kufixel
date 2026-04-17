package com.example.kufixel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun DashboardAndroidPreview() {
    val projects = remember { mutableStateListOf<Project>() }
    Dashboard(projects = projects)
}
