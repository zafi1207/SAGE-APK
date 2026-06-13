package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Bill
import com.example.data.LivingFund
import com.example.data.User
import com.example.viewmodel.SageViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: SageViewModel,
    onNavigateToAllocate: () -> Unit
) {
    val lang by viewModel.currentLanguage.collectAsState()
    val isCouple by viewModel.isCoupleMode.collectAsState()
    val activeTab by viewModel.activeUserTab.collectAsState()
    val users by viewModel.users.collectAsState()
    val bills by viewModel.bills.collectAsState()
    val lfState by viewModel.livingFund.collectAsState()
    val settingsState by viewModel.settings.collectAsState()
    val activeUser = users.find { it.id == activeTab && it.isActive } ?: users.find { it.id == 1 }
    val simulatedCalendar = Calendar.getInstance()

    // Currencies Formatter Indonesian Rupiah format
    val rupiahFormatter = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    var showQuickSpendDialog by remember { mutableStateOf(false) }
    var spendAmountStr by remember { mutableStateOf("") }
    var spendIsFree by remember { mutableStateOf(false) } // true for Free Spending, false for Living Fund

    var showQuickKosDialog by remember { mutableStateOf(false) }
    var kosAmountStr by remember { mutableStateOf("") }

    var showEditKosTargetDialog by remember { mutableStateOf(false) }
    var editKosTargetStr by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_scroll_column"),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // --- FINANCIAL OVERVIEW MODULE ---
        item {
            val calendarOverview = Calendar.getInstance()
            val curMo = calendarOverview.get(Calendar.MONTH) + 1
            val curYr = calendarOverview.get(Calendar.YEAR)
            var nxtMo = curMo + 1
            var nxtYr = curYr
            if (nxtMo > 12) {
                nxtMo = 1
                nxtYr += 1
            }

            val activeUserBills = bills.filter { it.ownerId == (activeUser?.id ?: 1) }

            val upcomingThisMonthActive = activeUserBills.filter {
                !it.isPaid && !it.isCompleted && (it.year < curYr || (it.year == curYr && it.month <= curMo))
            }.sumOf { it.remainingAmount }

            val upcomingNextMonthActive = activeUserBills.filter {
                !it.isPaid && !it.isCompleted && it.year == nxtYr && it.month == nxtMo
            }.sumOf { it.remainingAmount }

            val totalDebtActive = activeUserBills.filter { !it.isPaid && !it.isCompleted }.sumOf { it.remainingAmount }

            val endingSoonCountActive = activeUserBills.count {
                it.installmentIndex != null && it.installmentTotal != null && (it.installmentTotal - it.installmentIndex <= 1) && !it.isCompleted
            }

            val totalSavingsVal = lfState?.totalSavings ?: 0.0

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("financial_overview_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = "Overview icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Financial Overview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Upcoming This Month
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Upcoming Bills (This Month)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = rupiahFormatter.format(upcomingThisMonthActive),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // 2. Upcoming Next Month
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Upcoming,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Upcoming Bills (Next Month)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = rupiahFormatter.format(upcomingNextMonthActive),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // 3. Total Debt Remaining
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Total Debt Remaining",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = rupiahFormatter.format(totalDebtActive),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    // 4. Bills Ending Soon
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.HourglassBottom,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Bills Ending Soon",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = if (endingSoonCountActive > 0) "$endingSoonCountActive bill${if (endingSoonCountActive > 1) "s" else ""}" else "None",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (endingSoonCountActive > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // 5. Savings Progress
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Savings Progress",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = rupiahFormatter.format(totalSavingsVal),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // --- TWIN DASHBOARD LEDGER TABS ---
        if (isCouple) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(4.dp)
                ) {
                    val usersAvailable = users.filter { it.isActive }
                    val ramaUser = usersAvailable.find { it.id == 1 }
                    val nadiyaUser = usersAvailable.find { it.id == 2 }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activeTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { viewModel.setActiveUserTab(1) }
                            .padding(vertical = 12.dp)
                            .testTag("tab_rama"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ramaUser?.name ?: "Rama",
                            color = if (activeTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (nadiyaUser != null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (activeTab == 2) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { viewModel.setActiveUserTab(2) }
                                .padding(vertical = 12.dp)
                                .testTag("tab_nadiya"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                               text = nadiyaUser.name,
                                color = if (activeTab == 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        if (activeUser != null) {
            val isNadiya = activeUser.id == 2

            // Filters & Calculations relative to active user & simulated date
            val userBills = bills.filter { it.ownerId == activeUser.id && !it.isCompleted && !it.isPaid }
            val completedBillsCount = bills.count { it.ownerId == activeUser.id && (it.isCompleted || it.isPaid) }
            val currentSimulatedMonth = simulatedCalendar.get(Calendar.MONTH) + 1
            val currentSimulatedYear = simulatedCalendar.get(Calendar.YEAR)

            // Current Priority Bill: Nearest due, unpaid, unfilled
            val priorityBill = userBills
                .filter { !it.isPaid && !it.isCompleted }
                .sortedWith(compareBy<Bill> { it.year }.thenBy { it.month }.thenBy { it.day })
                .firstOrNull()

            // Safe to Use logic
            // Safe = current balance - (needed rent portion) - (unallocated sum of bills due this current month)
            val rentPortionNeeded = (activeUser.rentContribution - (if (activeUser.id == 1) (settingsState?.kosRamaContribution ?: 0.0) else (settingsState?.kosNadiyaContribution ?: 0.0))).coerceAtLeast(0.0)
            val upcomingBillsThisMonthUnallocated = userBills
                .filter { it.month == currentSimulatedMonth && it.year == currentSimulatedYear }
                .sumOf { it.remainingAmount }
            val safeToUse = (activeUser.currentBalance - rentPortionNeeded - upcomingBillsThisMonthUnallocated).coerceAtLeast(0.0)

            // Days until payday
            val daysUntilPayday = viewModel.getSimulatedDaysUntilPayday(activeUser.id, simulatedCalendar)

            // Living Fund values (shared/central wallet)
            val lfAvailable = lfState?.availableBalance ?: 0.0
            
            // Suggested daily allowance
            val suggestedDaily = if (daysUntilPayday > 0) lfAvailable / daysUntilPayday else lfAvailable

            // Weekly commits calculations
            var thisWeekSum = 0.0
            var nextWeekSum = 0.0
            var nextMonthSum = 0.0

            val sdfDay = simulatedCalendar.get(Calendar.DAY_OF_YEAR)
            val sdfYear = simulatedCalendar.get(Calendar.YEAR)

            for (bill in userBills) {
                // Approximate days difference
                val bCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, bill.year)
                    set(Calendar.MONTH, bill.month - 1)
                    set(Calendar.DAY_OF_MONTH, bill.day)
                }
                val diffMills = bCal.timeInMillis - simulatedCalendar.timeInMillis
                val diffDays = (diffMills / (1000 * 60 * 60 * 24)).toInt()

                if (diffDays in 0..7) {
                    thisWeekSum += bill.remainingAmount
                } else if (diffDays in 8..14) {
                    nextWeekSum += bill.remainingAmount
                } else if (diffDays in 15..45) {
                    nextMonthSum += bill.remainingAmount
                }
            }

            // --- HERO STATE: SAFE TO USE ---
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = SageLocalizer.t("safe_to_use", lang).uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                )
                                Text(
                                    text = rupiahFormatter.format(safeToUse),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            IconButton(
                                onClick = { onNavigateToAllocate() },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.onPrimary,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("action_quick_allocate")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Quick Allocate")
                            }
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Current Wallet Balance",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = rupiahFormatter.format(activeUser.currentBalance),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = SageLocalizer.t("free_spending", lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = rupiahFormatter.format(lfState?.totalFreeSpending ?: 0.0),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.clickable {
                                        spendIsFree = true
                                        showQuickSpendDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // --- PRIORITY BILL BANNER ---
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("priority_bill_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PriorityHigh,
                                contentDescription = "Priority",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = SageLocalizer.t("next_bill_due", lang),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (priorityBill != null) {
                                Text(
                                    text = priorityBill.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val billDayFormatted = String.format("%02d", priorityBill.day)
                                val billMonthName = getMonthName(priorityBill.month, lang).take(3)
                                val dueDateText = "$billDayFormatted $billMonthName ${priorityBill.year}"
                                Text(
                                    text = dueDateText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = rupiahFormatter.format(priorityBill.amount),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    text = SageLocalizer.t("no_priority", lang),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // --- LIVING FUND STATUS ---
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Kitchen,
                                    contentDescription = "Living Fund",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = SageLocalizer.t("living_fund_remaining", lang),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = {
                                    spendIsFree = false
                                    showQuickSpendDialog = true
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = "Log Expense",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Text(
                            text = rupiahFormatter.format(lfAvailable),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = SageLocalizer.t("suggested_daily", lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = rupiahFormatter.format(suggestedDaily),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = SageLocalizer.t("days_to_payday", lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "$daysUntilPayday " + SageLocalizer.t("days", lang),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // --- SHARED KOS (RENT) COMPONENT ---
            item {
                val rContribution = settingsState?.kosRamaContribution ?: 0.0
                val nContribution = settingsState?.kosNadiyaContribution ?: 0.0
                val kosTarget = settingsState?.kosTarget ?: 2000000.0
                val kosSum = rContribution + nContribution
                val kosRemaining = (kosTarget - kosSum).coerceAtLeast(0.0)
                val progress = if (kosTarget > 0) (kosSum / kosTarget).toFloat() else 0f

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Rent",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = SageLocalizer.t("kos_shared", lang),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clickable {
                                                editKosTargetStr = kosTarget.toInt().toString()
                                                showEditKosTargetDialog = true
                                            }
                                            .testTag("edit_kos_target_row")
                                    ) {
                                        Text(
                                            text = "Target: ${rupiahFormatter.format(kosTarget)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Rent Target",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }

                            if (kosRemaining > 0) {
                                Button(
                                    onClick = { showQuickKosDialog = true },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Add", fontSize = 11.sp)
                                }
                            } else {
                                IconButton(
                                    onClick = { viewModel.payRent() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Clear Rent Payment", tint = Color.Green)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${SageLocalizer.t("progress", lang)}: ${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "${SageLocalizer.t("remaining", lang)}: ${rupiahFormatter.format(kosRemaining)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${SageLocalizer.t("rama_contrib", lang)}: ${rupiahFormatter.format(rContribution)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isCouple) {
                                Text(
                                    text = "${SageLocalizer.t("nadiya_contrib", lang)}: ${rupiahFormatter.format(nContribution)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // --- NADIYA: UPCOMING BILLS BEFORE PAYDAY ---
            if (isNadiya) {
                // Collect Nadiya unpaid bills coming before next payday (next weekly pay cycle)
                val isNadiyaUpcomingNum = userBills.filter { bill ->
                    val bCal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, bill.year)
                        set(Calendar.MONTH, bill.month - 1)
                        set(Calendar.DAY_OF_MONTH, bill.day)
                    }
                    val diffDays = ((bCal.timeInMillis - simulatedCalendar.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                    diffDays in 0..daysUntilPayday
                }

                if (isNadiyaUpcomingNum.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = SageLocalizer.t("upcoming_before_payday", lang),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                for (bill in isNadiyaUpcomingNum) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = bill.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = rupiahFormatter.format(bill.remainingAmount),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- WEEKLY COMMITMENT SUMMARY (MONEY ONLY) ---
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = SageLocalizer.t("weekly_commitment", lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = SageLocalizer.t("this_week", lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = rupiahFormatter.format(thisWeekSum),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (thisWeekSum > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = SageLocalizer.t("next_week", lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = rupiahFormatter.format(nextWeekSum),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = SageLocalizer.t("next_month", lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = rupiahFormatter.format(nextMonthSum),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // --- BILLS ENDING SOON MODULE ---
            val finishedSoonBills = bills.filter {
                it.ownerId == activeUser.id &&
                it.installmentIndex != null &&
                it.installmentTotal != null &&
                (it.installmentTotal - it.installmentIndex <= 1) &&
                !it.isCompleted
            }

            if (finishedSoonBills.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = SageLocalizer.t("bills_ending_soon", lang).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        for (bill in finishedSoonBills) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = bill.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Installment ${bill.installmentIndex} of ${bill.installmentTotal}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    Text(
                                        text = rupiahFormatter.format(bill.amount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Spend Dialog
    if (showQuickSpendDialog) {
        AlertDialog(
            onDismissRequest = { showQuickSpendDialog = false },
            title = { Text(if (spendIsFree) "Deduct Free Spending" else "Spend from Living Fund") },
            text = {
                Column {
                    Text(
                        text = if (spendIsFree) "Enter the amount spent from your Free Spending wallet." else "Enter the cash amount spent from your Living Fund wallet pool.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = spendAmountStr,
                        onValueChange = { spendAmountStr = it },
                        label = { Text("Amount (Rp)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("spend_amt_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = spendAmountStr.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            if (spendIsFree) {
                                viewModel.spendFreeSpending(amt)
                            } else {
                                viewModel.spendFromLivingFund(amt)
                            }
                        }
                        spendAmountStr = ""
                        showQuickSpendDialog = false
                    },
                    modifier = Modifier.testTag("spend_confirm_btn")
                ) {
                    Text(SageLocalizer.t("confirm", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickSpendDialog = false }) {
                    Text(SageLocalizer.t("cancel", lang))
                }
            }
        )
    }

    // Kos Contribution Dialog
    if (showQuickKosDialog) {
        AlertDialog(
            onDismissRequest = { showQuickKosDialog = false },
            title = { Text("Allocate Rent (Kos) Contribution") },
            text = {
                Column {
                    Text(
                        text = "Reserve money from your current wallet balance to the Kos Rent pool.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = kosAmountStr,
                        onValueChange = { kosAmountStr = it },
                        label = { Text("Amount (Rp)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("kos_amt_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = kosAmountStr.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            viewModel.allocateKosRent(activeTab, amt)
                        }
                        kosAmountStr = ""
                        showQuickKosDialog = false
                    },
                    modifier = Modifier.testTag("kos_confirm_btn")
                ) {
                    Text(SageLocalizer.t("confirm", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickKosDialog = false }) {
                    Text(SageLocalizer.t("cancel", lang))
                }
            }
        )
    }

    // Edit Kos Rent Target Dialog
    if (showEditKosTargetDialog) {
        AlertDialog(
            onDismissRequest = { showEditKosTargetDialog = false },
            title = { Text("Edit Rent (Kos) Target") },
            text = {
                Column {
                    Text(
                        text = "Update the overall monthly target price for the room. Future allocations will adapt to this new price.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editKosTargetStr,
                        onValueChange = { editKosTargetStr = it },
                        label = { Text("Rent Target (Rp)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_kos_target_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetVal = editKosTargetStr.toDoubleOrNull() ?: 0.0
                        if (targetVal > 0) {
                            viewModel.updateKosTarget(targetVal)
                        }
                        showEditKosTargetDialog = false
                    },
                    modifier = Modifier.testTag("edit_kos_target_confirm_btn")
                ) {
                    Text(SageLocalizer.t("save", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditKosTargetDialog = false }) {
                    Text(SageLocalizer.t("cancel", lang))
                }
            }
        )
    }
}
