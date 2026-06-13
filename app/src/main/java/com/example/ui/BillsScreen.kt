package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Bill
import com.example.data.User
import com.example.viewmodel.SageViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillsScreen(
    viewModel: SageViewModel
) {
    val lang by viewModel.currentLanguage.collectAsState()
    val isCouple by viewModel.isCoupleMode.collectAsState()
    val bills by viewModel.bills.collectAsState()
    val users by viewModel.users.collectAsState()

    // Real Date instead of Simulated
    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH) + 1
    val currentYear = calendar.get(Calendar.YEAR)
    val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

    // Rupiah formatter
    val rupiahFormatter = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    // Tab state (1 for Rama, 2 for Nadiya)
    var selectedOwnerId by remember { mutableStateOf(1) }

    // Dialog state
    var showAddBillDialog by remember { mutableStateOf(false) }
    var selectedBillForManage by remember { mutableStateOf<Bill?>(null) }

    // Add Form fields
    var billName by remember { mutableStateOf("") }
    var billAmount by remember { mutableStateOf("") }
    var billOwner by remember { mutableStateOf(1) } // 1 for Rama, 2 for Nadiya
    var billDay by remember { mutableStateOf("") }
    var billMonth by remember { mutableStateOf("") }
    var billYear by remember { mutableStateOf("") }
    var billIsRecurring by remember { mutableStateOf(false) }
    var billInstallmentTotal by remember { mutableStateOf("") }
    var billRemainingPayments by remember { mutableStateOf("") }
    var billIsPriority by remember { mutableStateOf(false) }
    var billNotes by remember { mutableStateOf("") }
    var billType by remember { mutableStateOf("one_time") } // "one_time", "recurring", "installment"

    // Manage/Edit Form fields
    var editName by remember { mutableStateOf("") }
    var editAmount by remember { mutableStateOf("") }
    var editOwnerId by remember { mutableStateOf(1) }
    var editDay by remember { mutableStateOf("") }
    var editMonth by remember { mutableStateOf("") }
    var editYear by remember { mutableStateOf("") }
    var editInstallmentTotal by remember { mutableStateOf("") }
    var editRemainingPayments by remember { mutableStateOf("") }
    var editIsRecurring by remember { mutableStateOf(false) }
    var editNotes by remember { mutableStateOf("") }

    // Load active editing states when a bill is clicked
    LaunchedEffect(selectedBillForManage) {
        selectedBillForManage?.let { b ->
            editName = b.name
            editAmount = b.amount.toLong().toString()
            editOwnerId = b.ownerId
            editDay = b.day.toString()
            editMonth = b.month.toString()
            editYear = b.year.toString()
            editInstallmentTotal = b.installmentTotal?.toString() ?: ""
            editRemainingPayments = if (b.installmentTotal != null && b.installmentIndex != null) {
                (b.installmentTotal - b.installmentIndex + 1).toString()
            } else ""
            editIsRecurring = b.isRecurring
            editNotes = b.notes
        }
    }

    // Expanded / collapsed state of folders
    val expandedFolders = remember { mutableStateMapOf<Pair<Int, Int>, Boolean>() }
    var completedExpanded by remember { mutableStateOf(false) }

    // Filter bills belonging to selected owner
    val ownerBills = bills.filter { it.ownerId == selectedOwnerId }

    // Categorized unpaid bills grouped by year and month
    val unpaidBills = ownerBills.filter { !it.isPaid && !it.isCompleted }
    
    // Group and sort bills chronologically and prioritarily
    val groupedBills = remember(unpaidBills, currentYear, currentMonth, currentDay) {
        val tempMap = mutableMapOf<Pair<Int, Int>, MutableList<Bill>>()
        for (bill in unpaidBills) {
            val isPastUnpaid = (bill.year < currentYear) || (bill.year == currentYear && bill.month < currentMonth)
            val key = if (isPastUnpaid) {
                Pair(currentYear, currentMonth)
            } else {
                Pair(bill.year, bill.month)
            }
            tempMap.getOrPut(key) { mutableListOf() }.add(bill)
        }

        val sortedKeysMap = tempMap.toSortedMap(compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second })

        sortedKeysMap.mapValues { (key, billsList) ->
            val (yr, mo) = key
            if (yr == currentYear && mo == currentMonth) {
                // Priority: Overdue, Today, Next 7 days, Remaining month
                billsList.sortedWith(compareBy<Bill> { bill ->
                    val isOverdue = (bill.year < currentYear) || 
                                    (bill.year == currentYear && bill.month < currentMonth) || 
                                    (bill.year == currentYear && bill.month == currentMonth && bill.day < currentDay)
                    val isToday = bill.year == currentYear && bill.month == currentMonth && bill.day == currentDay
                    val isNext7Days = bill.year == currentYear && bill.month == currentMonth && bill.day in (currentDay + 1)..(currentDay + 7)

                    when {
                        isOverdue -> 0
                        isToday -> 1
                        isNext7Days -> 2
                        else -> 3
                    }
                }.thenBy { it.year }.thenBy { it.month }.thenBy { it.day })
            } else {
                // Standard Sort: chronological due date ascending
                billsList.sortedWith(compareBy<Bill> { it.year }.thenBy { it.month }.thenBy { it.day })
            }
        }
    }

    // Completed/Paid bills - sorted chronologically (nearest first)
    val completedBills = ownerBills.filter { it.isPaid || it.isCompleted }
        .sortedWith(compareBy<Bill> { it.year }.thenBy { it.month }.thenBy { it.day })

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    billName = ""
                    billAmount = ""
                    billOwner = selectedOwnerId
                    billDay = calendar.get(Calendar.DAY_OF_MONTH).toString()
                    billMonth = currentMonth.toString()
                    billYear = currentYear.toString()
                    billIsRecurring = false
                    billInstallmentTotal = ""
                    billRemainingPayments = ""
                    billIsPriority = false
                    billNotes = ""
                    billType = "one_time"
                    showAddBillDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(bottom = 70.dp)
                    .testTag("add_bill_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Bill")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("bills_list_view"),
            contentPadding = PaddingValues(16.dp)
        ) {
            // BACKEND ├── Rama / Nadiya Tab Selector
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
                            .background(if (selectedOwnerId == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { selectedOwnerId = 1 }
                            .padding(vertical = 12.dp)
                            .testTag("bills_tab_rama"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Rama",
                            color = if (selectedOwnerId == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedOwnerId == 2) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { selectedOwnerId = 2 }
                            .padding(vertical = 12.dp)
                            .testTag("bills_tab_nadiya"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nadiya",
                            color = if (selectedOwnerId == 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // MONTHLY COLLAPSIBLE FOLDERS
            if (groupedBills.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "No unpaid bills logged for this user",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                groupedBills.forEach { (key, monthBills) ->
                    val (yr, mo) = key
                    val isCurrent = (yr == currentYear && mo == currentMonth)
                    
                    // Default state: Current Month expanded, Future collapsed
                    val folderExpanded = expandedFolders.getOrDefault(key, isCurrent)
                    val totalAmountRequired = monthBills.sumOf { it.remainingAmount }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable { expandedFolders[key] = !folderExpanded }
                                .testTag("folder_header_${yr}_${mo}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(14.dp),
                            border = if (!isCurrent) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${getMonthName(mo, lang)} $yr",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isCurrent) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(MaterialTheme.colorScheme.primary)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "CURRENT",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Total Required: ${rupiahFormatter.format(totalAmountRequired)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                    )
                                }

                                Icon(
                                    imageVector = if (folderExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (folderExpanded) "Collapse" else "Expand",
                                    tint = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    if (folderExpanded) {
                        items(monthBills) { bill ->
                            Box(modifier = Modifier.padding(start = 8.dp, end = 4.dp)) {
                                BillItemRow(
                                    bill = bill,
                                    isCoupleMode = isCouple,
                                    users = users,
                                    rupiahFormatter = rupiahFormatter,
                                    onClick = { selectedBillForManage = bill }
                                )
                            }
                        }
                    }
                }
            }

            // COMPLETED / ARCHIVED COLLAPSIBLE FOLDER
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 6.dp)
                        .clickable { completedExpanded = !completedExpanded }
                        .testTag("completed_folder_header"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Completed Bills",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Total items: ${completedBills.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Icon(
                            imageVector = if (completedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (completedExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            if (completedExpanded) {
                if (completedBills.isEmpty()) {
                    item {
                        Text(
                            text = "No completed bills yet",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    items(completedBills) { bill ->
                        Box(modifier = Modifier.padding(start = 8.dp, end = 4.dp)) {
                            BillItemRow(
                                bill = bill,
                                isCoupleMode = isCouple,
                                users = users,
                                rupiahFormatter = rupiahFormatter,
                                onClick = { selectedBillForManage = bill }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(90.dp)) }
        }
    }

    // Manage AND FULL EDIT Bill Dialog
    if (selectedBillForManage != null) {
        val manageBill = selectedBillForManage!!

        AlertDialog(
            onDismissRequest = { selectedBillForManage = null },
            title = { Text("Manage & Edit Bill") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .testTag("manage_bill_dialog_content")
                ) {
                    // 1. Edit Name
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Bill Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_bill_name")
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Edit Amount
                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { editAmount = it },
                        label = { Text("Amount (Rp)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_bill_amount")
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. Edit Select User
                    Text("Select User:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = editOwnerId == 1,
                            onClick = { editOwnerId = 1 }
                        )
                        Text("Rama", modifier = Modifier.clickable { editOwnerId = 1 }, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(
                            selected = editOwnerId == 2,
                            onClick = { editOwnerId = 2 }
                        )
                        Text("Nadiya", modifier = Modifier.clickable { editOwnerId = 2 }, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // 4. Edit Due Date (Hari / Bulan / Tahun)
                    Text("Due Date (Day / Month / Year):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = editDay,
                            onValueChange = { editDay = it },
                            label = { Text("Day") },
                            modifier = Modifier.weight(1f).testTag("edit_bill_day")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedTextField(
                            value = editMonth,
                            onValueChange = { editMonth = it },
                            label = { Text("Month") },
                            modifier = Modifier.weight(1f).testTag("edit_bill_month")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedTextField(
                            value = editYear,
                            onValueChange = { editYear = it },
                            label = { Text("Year") },
                            modifier = Modifier.weight(1.3f).testTag("edit_bill_year")
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // 5. Edit Installment Count & Remaining Payments
                    Text("Installment details (Optional):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = editInstallmentTotal,
                            onValueChange = { editInstallmentTotal = it },
                            label = { Text("Total Count") },
                            placeholder = { Text("e.g. 12") },
                            modifier = Modifier.weight(1f).testTag("edit_bill_inst_total")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedTextField(
                            value = editRemainingPayments,
                            onValueChange = { editRemainingPayments = it },
                            label = { Text("Remaining Count") },
                            placeholder = { Text("e.g. 5") },
                            modifier = Modifier.weight(1f).testTag("edit_bill_inst_remaining")
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // 6. Edit Select Priority / Recurring Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = editIsRecurring,
                            onCheckedChange = { editIsRecurring = it }
                        )
                        Text("Recurring Monthly (Priority)", modifier = Modifier.clickable { editIsRecurring = !editIsRecurring }, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // 7. Edit Notes
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Notes & Priorities") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_bill_notes")
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            viewModel.deleteBill(manageBill)
                            selectedBillForManage = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("manage_bill_delete_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(SageLocalizer.t("delete", lang))
                    }

                    Row {
                        if (!manageBill.isPaid && !manageBill.isCompleted) {
                            TextButton(
                                onClick = {
                                    viewModel.markBillPaid(manageBill, true)
                                    selectedBillForManage = null
                                },
                                modifier = Modifier.testTag("manage_bill_pay_btn")
                            ) {
                                Text("Bayar")
                            }
                        } else {
                            TextButton(
                                onClick = {
                                    viewModel.markBillPaid(manageBill, false)
                                    selectedBillForManage = null
                                },
                                modifier = Modifier.testTag("manage_bill_reset_btn")
                            ) {
                                Text("Reset")
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(6.dp))

                        Button(
                            onClick = {
                                val amountVal = editAmount.toDoubleOrNull() ?: manageBill.amount
                                val dayVal = editDay.toIntOrNull() ?: manageBill.day
                                val monthVal = editMonth.toIntOrNull() ?: manageBill.month
                                val yearVal = editYear.toIntOrNull() ?: manageBill.year
                                val instTotalVal = editInstallmentTotal.toIntOrNull()
                                val instRemainingVal = editRemainingPayments.toIntOrNull()

                                // Compute backward installmentIndex
                                val instIndexVal = if (instTotalVal != null && instRemainingVal != null) {
                                    (instTotalVal - instRemainingVal + 1).coerceIn(1, instTotalVal)
                                } else {
                                    manageBill.installmentIndex
                                }

                                val updated = manageBill.copy(
                                    name = editName,
                                    amount = amountVal,
                                    ownerId = editOwnerId,
                                    day = dayVal,
                                    month = monthVal,
                                    year = yearVal,
                                    installmentTotal = instTotalVal,
                                    installmentIndex = instIndexVal,
                                    isRecurring = editIsRecurring,
                                    notes = editNotes
                                )
                                viewModel.editBill(updated)
                                selectedBillForManage = null
                            },
                            modifier = Modifier.testTag("manage_bill_save_btn")
                        ) {
                            Text("Simpan")
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedBillForManage = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Add Bill Dialog
    if (showAddBillDialog) {
         AlertDialog(
             onDismissRequest = { showAddBillDialog = false },
             title = { Text(SageLocalizer.t("add_bill", lang)) },
             text = {
                 Column(
                     modifier = Modifier
                         .fillMaxWidth()
                         .testTag("add_bill_form")
                 ) {
                     OutlinedTextField(
                         value = billName,
                         onValueChange = { billName = it },
                         label = { Text("Name") },
                         singleLine = true,
                         modifier = Modifier.fillMaxWidth()
                     )

                     Spacer(modifier = Modifier.height(8.dp))

                     OutlinedTextField(
                         value = billAmount,
                         onValueChange = { billAmount = it },
                         label = { Text("Amount (Rp)") },
                         singleLine = true,
                         modifier = Modifier.fillMaxWidth()
                     )

                     if (isCouple) {
                         Spacer(modifier = Modifier.height(8.dp))
                         Text("Owner:", style = MaterialTheme.typography.bodySmall)
                         Row(verticalAlignment = Alignment.CenterVertically) {
                             RadioButton(
                                 selected = billOwner == 1,
                                 onClick = { billOwner = 1 }
                             )
                             Text("Rama", modifier = Modifier.clickable { billOwner = 1 })
                             Spacer(modifier = Modifier.width(16.dp))
                             RadioButton(
                                 selected = billOwner == 2,
                                 onClick = { billOwner = 2 }
                             )
                             Text("Nadiya", modifier = Modifier.clickable { billOwner = 2 })
                         }
                     }

                     Spacer(modifier = Modifier.height(12.dp))
                     Text("Bill Type:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                     Row(
                         modifier = Modifier.fillMaxWidth(),
                         horizontalArrangement = Arrangement.SpaceBetween,
                         verticalAlignment = Alignment.CenterVertically
                     ) {
                         Row(verticalAlignment = Alignment.CenterVertically) {
                             RadioButton(selected = billType == "one_time", onClick = { billType = "one_time" })
                             Text("One Time", style = MaterialTheme.typography.bodySmall, modifier = Modifier.clickable { billType = "one_time" })
                         }
                         Row(verticalAlignment = Alignment.CenterVertically) {
                             RadioButton(selected = billType == "recurring", onClick = { billType = "recurring" })
                             Text("Monthly", style = MaterialTheme.typography.bodySmall, modifier = Modifier.clickable { billType = "recurring" })
                         }
                         Row(verticalAlignment = Alignment.CenterVertically) {
                             RadioButton(selected = billType == "installment", onClick = { billType = "installment" })
                             Text("Installment", style = MaterialTheme.typography.bodySmall, modifier = Modifier.clickable { billType = "installment" })
                         }
                     }

                     Spacer(modifier = Modifier.height(8.dp))
                     Row {
                         OutlinedTextField(
                             value = billDay,
                             onValueChange = { billDay = it },
                             label = { Text("Day") },
                             modifier = Modifier.weight(1f)
                         )
                         Spacer(modifier = Modifier.width(8.dp))
                         OutlinedTextField(
                             value = billMonth,
                             onValueChange = { billMonth = it },
                             label = { Text(if (billType == "installment") "Start Month" else "Month") },
                             modifier = Modifier.weight(1f)
                         )
                         Spacer(modifier = Modifier.width(8.dp))
                         OutlinedTextField(
                             value = billYear,
                             onValueChange = { billYear = it },
                             label = { Text(if (billType == "installment") "Start Year" else "Year") },
                             modifier = Modifier.weight(1.5f)
                         )
                     }

                     if (billType == "installment") {
                         Spacer(modifier = Modifier.height(8.dp))
                         Text("Installment details:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                         Row(modifier = Modifier.fillMaxWidth()) {
                             OutlinedTextField(
                                 value = billInstallmentTotal,
                                 onValueChange = { billInstallmentTotal = it },
                                 label = { Text("Total Installments") },
                                 placeholder = { Text("e.g. 12") },
                                 modifier = Modifier.weight(1f).testTag("add_bill_inst_total")
                             )
                             Spacer(modifier = Modifier.width(6.dp))
                             OutlinedTextField(
                                 value = billRemainingPayments,
                                 onValueChange = { billRemainingPayments = it },
                                 label = { Text("Remaining Count") },
                                 placeholder = { Text("e.g. 5") },
                                 modifier = Modifier.weight(1f).testTag("add_bill_inst_remaining")
                             )
                         }
                     }

                     Spacer(modifier = Modifier.height(8.dp))
                     Row(verticalAlignment = Alignment.CenterVertically) {
                         Checkbox(
                             checked = billIsPriority,
                             onCheckedChange = { billIsPriority = it }
                         )
                         Text("High Priority Status", modifier = Modifier.clickable { billIsPriority = !billIsPriority })
                     }

                     Spacer(modifier = Modifier.height(8.dp))
                     OutlinedTextField(
                         value = billNotes,
                         onValueChange = { billNotes = it },
                         label = { Text("Notes & description") },
                         modifier = Modifier.fillMaxWidth()
                     )
                 }
             },
             confirmButton = {
                 Button(
                     onClick = {
                         val amount = billAmount.toDoubleOrNull() ?: 0.0
                         val day = billDay.toIntOrNull() ?: currentMonth
                         val month = billMonth.toIntOrNull() ?: currentMonth
                         val year = billYear.toIntOrNull() ?: currentYear
                         val isRec = billType == "recurring"
                         val isInst = billType == "installment"

                         val instTotalVal = if (isInst) billInstallmentTotal.toIntOrNull() else null
                         val instRemainingVal = if (isInst) billRemainingPayments.toIntOrNull() else null
                         val instIndexVal = if (instTotalVal != null && instRemainingVal != null) {
                             (instTotalVal - instRemainingVal + 1).coerceIn(1, instTotalVal)
                         } else null

                         if (billName.isNotEmpty() && amount > 0) {
                             viewModel.addBill(
                                 ownerId = billOwner,
                                 name = billName,
                                 amount = amount,
                                 day = day,
                                 month = month,
                                 year = year,
                                 isRecurring = isRec,
                                 notes = billNotes,
                                 installmentIndex = instIndexVal,
                                 installmentTotal = instTotalVal,
                                 isPriority = billIsPriority
                             )
                         }
                         showAddBillDialog = false
                     },
                     modifier = Modifier.testTag("submit_add_bill_btn")
                 ) {
                     Text(SageLocalizer.t("save", lang))
                 }
             },
             dismissButton = {
                 TextButton(onClick = { showAddBillDialog = false }) {
                     Text(SageLocalizer.t("cancel", lang))
                 }
             }
         )
    }
}

fun getMonthName(month: Int, lang: String): String {
    val monthsEn = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    val monthsId = listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
    val idx = (month - 1).coerceIn(0, 11)
    return if (lang == "id") monthsId[idx] else monthsEn[idx]
}

@Composable
fun BillItemRow(
    bill: Bill,
    isCoupleMode: Boolean,
    users: List<User>,
    rupiahFormatter: NumberFormat,
    onClick: () -> Unit
) {
    val progress = bill.progressPercent / 100f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() }
            .testTag("bill_item_${bill.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (bill.isPaid) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = bill.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (bill.isPaid) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                        )
                        
                        val curCalendar = Calendar.getInstance()
                        val curDay = curCalendar.get(Calendar.DAY_OF_MONTH)
                        val curMonth = curCalendar.get(Calendar.MONTH) + 1
                        val curYear = curCalendar.get(Calendar.YEAR)

                        val isOverdue = !bill.isPaid && !bill.isCompleted && (
                            (bill.year < curYear) ||
                            (bill.year == curYear && bill.month < curMonth) ||
                            (bill.year == curYear && bill.month == curMonth && bill.day < curDay)
                        )
                        val isToday = !bill.isPaid && !bill.isCompleted && bill.year == curYear && bill.month == curMonth && bill.day == curDay
                        val isNext7Days = !bill.isPaid && !bill.isCompleted && bill.year == curYear && bill.month == curMonth && bill.day in (curDay + 1)..(curDay + 7)

                        if (isOverdue) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.error)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "OVERDUE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onError
                                )
                            }
                        } else if (isToday) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF2E7D32)) // nice slate emerald green
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "TODAY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        } else if (isNext7Days) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "DUE SOON",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }

                        if (bill.isPriority) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "PRIORITY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        if (isCoupleMode) {
                            val ownerName = users.find { it.id == bill.ownerId }?.name ?: "User"
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (bill.ownerId == 1) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        else MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = ownerName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (bill.ownerId == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                    Text(
                        text = "Due: ${bill.day}/${bill.month}/${bill.year}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = rupiahFormatter.format(bill.amount),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (bill.isPaid) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                    )
                    if (bill.reservedAmount > 0 && !bill.isPaid) {
                        Text(
                            text = "Reserved: ${rupiahFormatter.format(bill.reservedAmount)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (!bill.isPaid && bill.amount > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Allocation Progress: ${bill.progressPercent}%",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Text(
                        text = "Remaining: ${rupiahFormatter.format(bill.remainingAmount)}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (bill.isPaid) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = "Paid", tint = Color.Green, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lunas (Paid on Time)", style = MaterialTheme.typography.bodySmall, color = Color.Green)
                }
            }
        }
    }
}
