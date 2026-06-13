package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.viewmodel.SageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SageApp(
    viewModel: SageViewModel = viewModel()
) {
    val lang by viewModel.currentLanguage.collectAsState()
    val requestedTab by viewModel.requestedTab.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        showSplash = false
    }

    LaunchedEffect(requestedTab) {
        selectedTab = requestedTab
    }

    if (showSplash) {
        SplashScreen()
    } else {
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
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B4D3E)), // Elegant Sage Dark Green Theme
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.sage_logo),
                    contentDescription = "Sage Splash Screen Logo",
                    modifier = Modifier
                        .size(132.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "SAGE",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = Color(0xFFF1F8E9)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Life Finance Companion",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFA5D6A7),
                fontWeight = FontWeight.Light,
                letterSpacing = 1.5.sp
            )
        }
    }
}
