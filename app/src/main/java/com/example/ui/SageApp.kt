package com.example.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.viewmodel.SageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SageApp(
    viewModel: SageViewModel = viewModel()
) {
    val lang by viewModel.currentLanguage.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("app_bottom_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = null) },
                    label = { Text(SageLocalizer.t("dashboard", lang)) },
                    modifier = Modifier.testTag("nav_dashboard")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text(SageLocalizer.t("bills", lang)) },
                    modifier = Modifier.testTag("nav_bills")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                    label = { Text(SageLocalizer.t("allocate", lang)) },
                    modifier = Modifier.testTag("nav_allocate")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(SageLocalizer.t("profile", lang)) },
                    modifier = Modifier.testTag("nav_profile")
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToAllocate = { selectedTab = 2 }
                )
                1 -> BillsScreen(viewModel = viewModel)
                2 -> AllocationScreen(
                    viewModel = viewModel,
                    onAllocationSuccess = { selectedTab = 0 }
                )
                3 -> ProfileScreen(viewModel = viewModel)
            }
        }
    }
}
