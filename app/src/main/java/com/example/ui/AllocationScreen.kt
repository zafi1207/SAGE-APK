package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Bill
import com.example.viewmodel.SageViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllocationScreen(
    viewModel: SageViewModel,
    onAllocationSuccess: () -> Unit
) {
    val lang by viewModel.currentLanguage.collectAsState()
    val isCouple by viewModel.isCoupleMode.collectAsState()
    val bills by viewModel.bills.collectAsState()
    val settingsState by viewModel.settings.collectAsState()

    // Rupiah formatter
    val rupiahFormatter = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    var incomeInput by remember { mutableStateOf("") }
    var selectedUser by remember { mutableStateOf(1) } // 1 for Rama, 2 for Nadiya
    var incomeSourceType by remember { mutableStateOf("Monthly Salary") }

    val parsedIncome = incomeInput.toDoubleOrNull() ?: 0.0

    // Priority calculated suggestions
    val suggestLaundry = if (selectedUser == 2) 50000.0 else 0.0
    val suggestLivingFund = if (selectedUser == 2) 390000.0 else 1660000.0 // 390k for Nadiya weekly, 1.66M for Rama monthly (50k * 30 + 40k * 4)
    val rentTarget = settingsState?.kosTarget ?: 2000000.0
    val suggestRent = if (selectedUser == 1) 1000000.0 else 1000000.0

    // Mutable overrides for allocation worksheet
    var allocLaundry by remember { mutableStateOf("") }
    var allocLivingFund by remember { mutableStateOf("") }
    var allocRent by remember { mutableStateOf("") }
    var allocSavings by remember { mutableStateOf("") }

    // Bill allocations map: BillID -> Allocation Amount
    val billAllocations = remember { mutableStateMapOf<Int, String>() }
    var showAllBillsState by remember { mutableStateOf(false) }

    var lastSuggestedUser by remember { mutableStateOf<Int?>(null) }
    var wasIncomeZero by remember { mutableStateOf(true) }

    // Watch selected user changes or major positive transitions to pre-populate suggested defaults
    LaunchedEffect(selectedUser, parsedIncome) {
        val incomeNowPositive = parsedIncome > 0
        val userChanged = lastSuggestedUser != selectedUser
        
        if (userChanged) {
            incomeSourceType = if (selectedUser == 1) "Monthly Salary" else "Weekly client business"
        }

        if (incomeNowPositive && (wasIncomeZero || userChanged)) {
            allocLaundry = if (selectedUser == 2) "50000" else "0"
            allocLivingFund = if (selectedUser == 2) "390000" else "1660000"
            allocRent = "1000000"
            allocSavings = "0"
            lastSuggestedUser = selectedUser
            wasIncomeZero = false
        } else if (!incomeNowPositive) {
            wasIncomeZero = true
        }
    }

    // Unpaid & uncompleted bills of selected user
    val userBills = bills
        .filter { it.ownerId == selectedUser && !it.isPaid && !it.isCompleted }
        .sortedWith(compareBy<Bill> { it.year }.thenBy { it.month }.thenBy { it.day })

    // Calculations of running remaining residual
    val curLaundry = allocLaundry.toDoubleOrNull() ?: 0.0
    val curLivingFund = allocLivingFund.toDoubleOrNull() ?: 0.0
    val curRent = allocRent.toDoubleOrNull() ?: 0.0
    val curSavings = allocSavings.toDoubleOrNull() ?: 0.0

    // Sum of manual bill allocations
    val billsAllocSum = userBills.sumOf { bill ->
        val input = billAllocations[bill.id] ?: ""
        input.toDoubleOrNull() ?: 0.0
    }

    val totalDeducted = curLaundry + curLivingFund + curRent + curSavings + billsAllocSum
    val remainingResidual = (parsedIncome - totalDeducted).coerceAtLeast(0.0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("allocation_scroll_column"),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = SageLocalizer.t("add_income_title", lang),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = "Sage utilizes a strict priority-based allocation engine. Incoming wages are automatically divided in sequence to secure essentials first.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }

        // 1. Selector of recipient
        if (isCouple) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedUser == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { selectedUser = 1 }
                            .padding(vertical = 12.dp)
                            .testTag("alloc_user_rama"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Rama (Monthly)",
                            color = if (selectedUser == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedUser == 2) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { selectedUser = 2 }
                            .padding(vertical = 12.dp)
                            .testTag("alloc_user_nadiya"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nadiya (Weekly)",
                            color = if (selectedUser == 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Input Income Amount
        item {
            OutlinedTextField(
                value = incomeInput,
                onValueChange = { incomeInput = it },
                label = { Text(SageLocalizer.t("amount_to_allocate", lang)) },
                placeholder = { Text("e.g. 1500000") },
                singleLine = true,
                prefix = { Text("Rp ") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("income_amount_input"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Selection of Income Source for Rama / Nadiya
        item {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Text(
                    text = "INCOME SOURCE TYPE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedUser == 1) {
                        // Rama: Monthly Salary or Ojol Side Job
                        val options = listOf(
                            "Monthly Salary" to 4500000.0,
                            "Courier Side Job (Ojol)" to 0.0
                        )
                        options.forEach { (label, value) ->
                            val isSelected = (incomeSourceType == label)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable {
                                        incomeSourceType = label
                                        if (value > 0) {
                                            incomeInput = value.toInt().toString()
                                        } else {
                                            incomeInput = ""
                                        }
                                    }
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // Nadiya: Weekly Client Business
                        val options = listOf(
                            "Weekly Client Income" to 0.0
                        )
                        options.forEach { (label, value) ->
                            val isSelected = true
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- GUIDED STEPS (Priority List) ---
        if (parsedIncome > 0) {
            item {
                Text(
                    text = SageLocalizer.t("priority_allocation_flow", lang).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                )
            }

            // Step 1: Laundry Reserve
            item {
                PriorityAllocationStepCard(
                    title = SageLocalizer.t("step_1_laundry", lang),
                    suggestedValue = suggestLaundry,
                    rupiahFormatter = rupiahFormatter,
                    value = allocLaundry,
                    onValueChange = { allocLaundry = it },
                    testTag = "alloc_step_laundry"
                )
            }

            // Step 2: Living Fund
            item {
                PriorityAllocationStepCard(
                    title = SageLocalizer.t("step_2_living", lang),
                    suggestedValue = suggestLivingFund,
                    rupiahFormatter = rupiahFormatter,
                    value = allocLivingFund,
                    onValueChange = { allocLivingFund = it },
                    testTag = "alloc_step_living"
                )
            }

            // Step 3: Shared rent (Kos) target
            item {
                PriorityAllocationStepCard(
                    title = SageLocalizer.t("step_3_rent", lang),
                    suggestedValue = suggestRent,
                    rupiahFormatter = rupiahFormatter,
                    value = allocRent,
                    onValueChange = { allocRent = it },
                    testTag = "alloc_step_rent"
                )
            }

            // Step 4: Suggested nearest bills
            if (userBills.isNotEmpty()) {
                val calendarReal = Calendar.getInstance()
                
                // Helper to score recommended bills
                fun getRecommendationScore(bill: Bill, allUnpaid: List<Bill>): Int {
                    val bCal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, bill.year)
                        set(Calendar.MONTH, bill.month - 1)
                        set(Calendar.DAY_OF_MONTH, bill.day)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val todayCal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val diffDays = ((bCal.timeInMillis - todayCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()

                    val isOverdue = diffDays < 0
                    val isSoon7 = diffDays in 0..7
                    val isSoon14 = diffDays in 0..14
                    val isLargest = allUnpaid.isNotEmpty() && bill.id == allUnpaid.maxByOrNull { it.amount }?.id
                    val isKos = bill.name.contains("kos", ignoreCase = true) || bill.name.contains("rent", ignoreCase = true)

                    if (isOverdue) return 1
                    if (isSoon7) return 2
                    if (isSoon14) return 3
                    if (isLargest) return 4
                    if (isKos) return 5
                    return 6 // Other
                }

                val recommendedBills = userBills.filter { getRecommendationScore(it, userBills) <= 5 }
                    .sortedBy { getRecommendationScore(it, userBills) }
                val otherBills = userBills.filter { getRecommendationScore(it, userBills) > 5 }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = "Due Bills Suggestion",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Render Recommended Bills
                items(recommendedBills) { bill ->
                    val billVal = billAllocations[bill.id] ?: ""
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.5f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = bill.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "RECOMMENDED",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    text = "Due day ${bill.day}/${bill.month}/${bill.year} | Total: ${rupiahFormatter.format(bill.amount)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "Remaining required: ${rupiahFormatter.format(bill.remainingAmount)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            OutlinedTextField(
                                value = billVal,
                                onValueChange = { billAllocations[bill.id] = it },
                                label = { Text("Alloc") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("alloc_bill_input_${bill.id}")
                            )
                        }
                    }
                }

                // Collapsible Show All bills card
                if (otherBills.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { showAllBillsState = !showAllBillsState }
                                .testTag("toggle_all_bills_btn"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (showAllBillsState) "Hide Remaining Bills" else "Show All Bills (${otherBills.size} more)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Icon(
                                    imageVector = if (showAllBillsState) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Toggle remaining bills"
                                )
                            }
                        }
                    }

                    if (showAllBillsState) {
                        items(otherBills) { bill ->
                            val billVal = billAllocations[bill.id] ?: ""
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.5f)) {
                                        Text(
                                            text = bill.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Due day ${bill.day}/${bill.month}/${bill.year} | Total: ${rupiahFormatter.format(bill.amount)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Text(
                                            text = "Remaining required: ${rupiahFormatter.format(bill.remainingAmount)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    OutlinedTextField(
                                        value = billVal,
                                        onValueChange = { billAllocations[bill.id] = it },
                                        label = { Text("Alloc") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("alloc_bill_input_${bill.id}")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Step 5: Savings reserve
            item {
                PriorityAllocationStepCard(
                    title = "Savings",
                    suggestedValue = 0.0,
                    rupiahFormatter = rupiahFormatter,
                    value = allocSavings,
                    onValueChange = { allocSavings = it },
                    testTag = "alloc_step_savings"
                )
            }

            // Step 6: Free spending
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Free Money",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Enjoy your Money!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = rupiahFormatter.format(remainingResidual),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // SUBMIT BUTTON MODULE
            item {
                val hasError = totalDeducted > parsedIncome
                if (hasError) {
                    Text(
                        text = "Total allocations (${rupiahFormatter.format(totalDeducted)}) exceed total income!",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }

                Button(
                    onClick = {
                        val finalBillAllocs = mutableMapOf<Int, Double>()
                        for (bill in userBills) {
                            val v = billAllocations[bill.id] ?: ""
                            val amt = v.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                finalBillAllocs[bill.id] = amt
                            }
                        }

                        viewModel.manualAllocation(
                            userId = selectedUser,
                            totalIncome = parsedIncome,
                            livingFundAlloc = curLivingFund,
                            laundryAlloc = curLaundry,
                            kosAlloc = curRent,
                            savingsAlloc = curSavings,
                            freeSpendingAlloc = remainingResidual,
                            billAllocations = finalBillAllocs,
                            incomeType = incomeSourceType
                        )
                        onAllocationSuccess()
                    },
                    enabled = !hasError && parsedIncome > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .height(54.dp)
                        .testTag("confirm_allocation_btn")
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(SageLocalizer.t("allocate_btn", lang))
                }
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Enter income to start priority allocation flow",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PriorityAllocationStepCard(
    title: String,
    suggestedValue: Double,
    rupiahFormatter: NumberFormat,
    value: String,
    onValueChange: (String) -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.5f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                if (suggestedValue > 0) {
                    Text(
                        text = "Suggested Default: ${rupiahFormatter.format(suggestedValue)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("Alloc (Rp)") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag(testTag)
            )
        }
    }
}
