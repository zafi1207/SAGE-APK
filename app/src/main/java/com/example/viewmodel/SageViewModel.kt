package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class SageViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = SageRepository(database.sageDao())

    // Language setting ("en" or "id")
    val settings = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val currentLanguage: StateFlow<String> = settings
        .map { it?.language ?: "en" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    val themeMode: StateFlow<String> = settings
        .map { it?.themeMode ?: "Light" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Light")

    val isCoupleMode: StateFlow<Boolean> = settings
        .map { it?.isCoupleMode ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isNotificationsEnabled: StateFlow<Boolean> = settings
        .map { it?.isNotificationsEnabled ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _requestedTab = MutableStateFlow(0)
    val requestedTab: StateFlow<Int> = _requestedTab.asStateFlow()

    fun setRequestedTab(tab: Int) {
        _requestedTab.value = tab
    }

    // Forecast mode offset (In Days)
    private val _forecastOffsetDays = MutableStateFlow(0)
    val forecastOffsetDays: StateFlow<Int> = _forecastOffsetDays.asStateFlow()

    // Users
    val users = repository.allUsers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Bills
    val bills = repository.allBills.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Living Fund State
    val livingFund = repository.livingFund.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Income logs
    val incomeLogs = repository.allIncomeLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // active dashboard companion tab (1 for Rama, 2 for Nadiya)
    private val _activeUserTab = MutableStateFlow(1)
    val activeUserTab: StateFlow<Int> = _activeUserTab.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.checkAndSeedDatabase()
        }
    }

    fun setActiveUserTab(userId: Int) {
        _activeUserTab.value = userId
    }

    // Language switcher
    fun setLanguage(lang: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val s = repository.getSettingsInstance() ?: SettingsEntity()
            repository.updateSettings(s.copy(language = lang))
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val s = repository.getSettingsInstance() ?: SettingsEntity()
            repository.updateSettings(s.copy(themeMode = mode))
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val s = repository.getSettingsInstance() ?: SettingsEntity()
            repository.updateSettings(s.copy(isNotificationsEnabled = enabled))
            if (!enabled) {
                com.example.notifications.NotificationHelper.cancelAllNotifications(getApplication())
            } else {
                com.example.notifications.NotificationHelper.checkAndShowNotifications(getApplication())
            }
        }
    }

    // Toggle mode
    fun setCoupleModeEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val s = repository.getSettingsInstance() ?: SettingsEntity()
            repository.updateSettings(s.copy(isCoupleMode = enabled))
            
            // If couple is disabled, make Nadiya inactive
            val nadiya = repository.getAllUsersInstance().find { it.id == 2 }
            if (nadiya != null) {
                repository.updateUser(nadiya.copy(isActive = enabled))
            }
        }
    }

    fun updateKosTarget(target: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val s = repository.getSettingsInstance() ?: SettingsEntity()
            repository.updateSettings(s.copy(kosTarget = target))
        }
    }

    // User Operations
    fun updateUser(user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateUser(user)
        }
    }

    fun addUser(name: String, incomeType: String, incomeAmount: Double, rentContribution: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val nextId = 2 // Simple static system (Rama is 1, Nadiya is 2)
            val nUser = User(
                id = nextId,
                name = name,
                incomeType = incomeType,
                incomeAmount = incomeAmount,
                currentBalance = 0.0,
                rentContribution = rentContribution,
                isActive = true
            )
            repository.insertUser(nUser)
            val s = repository.getSettingsInstance() ?: SettingsEntity()
            repository.updateSettings(s.copy(isCoupleMode = true))
        }
    }

    // Bill Operations
    fun addBill(
        ownerId: Int,
        name: String,
        amount: Double,
        day: Int,
        month: Int,
        year: Int,
        isRecurring: Boolean,
        notes: String,
        installmentIndex: Int? = null,
        installmentTotal: Int? = null,
        isPriority: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (installmentTotal != null && installmentTotal > 1) {
                val startIdx = installmentIndex ?: 1
                val list = mutableListOf<Bill>()
                for (idx in startIdx..installmentTotal) {
                    val m = month + (idx - startIdx)
                    val actualM = (m - 1) % 12 + 1
                    val actualY = year + (m - 1) / 12
                    list.add(
                        Bill(
                            ownerId = ownerId,
                            name = name,
                            amount = amount,
                            day = day,
                            month = actualM,
                            year = actualY,
                            installmentIndex = idx,
                            installmentTotal = installmentTotal,
                            isRecurring = false,
                            notes = notes,
                            isPriority = isPriority
                        )
                    )
                }
                repository.insertBills(list)
            } else if (isRecurring) {
                val list = mutableListOf<Bill>()
                for (idx in 1..36) {
                    val m = month + idx - 1
                    val actualM = (m - 1) % 12 + 1
                    val actualY = year + (m - 1) / 12
                    list.add(
                        Bill(
                            ownerId = ownerId,
                            name = name,
                            amount = amount,
                            day = day,
                            month = actualM,
                            year = actualY,
                            isRecurring = true,
                            notes = notes,
                            isPriority = isPriority
                        )
                    )
                }
                repository.insertBills(list)
            } else {
                val b = Bill(
                    ownerId = ownerId,
                    name = name,
                    amount = amount,
                    day = day,
                    month = month,
                    year = year,
                    isRecurring = isRecurring,
                    notes = notes,
                    installmentIndex = installmentIndex,
                    installmentTotal = installmentTotal,
                    isPriority = isPriority
                )
                repository.insertBill(b)
            }
            com.example.notifications.NotificationHelper.checkAndShowNotifications(getApplication())
        }
    }

    fun editBill(bill: Bill) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateBill(bill)
            com.example.notifications.NotificationHelper.checkAndShowNotifications(getApplication())
        }
    }

    fun deleteBill(bill: Bill) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBill(bill)
            com.example.notifications.NotificationHelper.checkAndShowNotifications(getApplication())
        }
    }

    fun markBillPaid(bill: Bill, payOption: Boolean) { // payOption: true for set paid/archived, false for reset
        viewModelScope.launch(Dispatchers.IO) {
            if (payOption) {
                // If it is regular recurring monthly, duplicate it automatically for the next month!
                if (bill.isRecurring) {
                    var nextM = bill.month + 1
                    var nextY = bill.year
                    if (nextM > 12) {
                        nextM = 1
                        nextY += 1
                    }
                    val shouldSpawn = if (bill.recurringEndMonth != null && bill.recurringEndYear != null) {
                        (nextY < bill.recurringEndYear) || (nextY == bill.recurringEndYear && nextM <= bill.recurringEndMonth)
                    } else true

                    if (shouldSpawn) {
                        repository.insertBill(
                            bill.copy(
                                id = 0, // Fresh insert
                                month = nextM,
                                year = nextY,
                                reservedAmount = 0.0,
                                isPaid = false,
                                isCompleted = false
                            )
                        )
                    }
                }
                
                // Create income log if a reserved payment actually occurred? No, payment is just marked paid.
                // Subtract user balance by remainingAmount required to pay it (since allocation might have covered some or all)
                val user = repository.getAllUsersInstance().find { it.id == bill.ownerId }
                if (user != null) {
                    val actualPaymentRequired = bill.remainingAmount
                    val newBal = (user.currentBalance - actualPaymentRequired).coerceAtLeast(0.0)
                    repository.updateUser(user.copy(currentBalance = newBal))
                }

                repository.updateBill(bill.copy(isPaid = true, reservedAmount = bill.amount, isCompleted = true))
            } else {
                repository.updateBill(bill.copy(isPaid = false, reservedAmount = 0.0, isCompleted = false))
            }
            com.example.notifications.NotificationHelper.checkAndShowNotifications(getApplication())
        }
    }

    fun markBillCompleted(bill: Bill) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateBill(bill.copy(isCompleted = true))
            com.example.notifications.NotificationHelper.checkAndShowNotifications(getApplication())
        }
    }

    // Allocation System API
    fun manualAllocation(
        userId: Int,
        totalIncome: Double,
        livingFundAlloc: Double,
        laundryAlloc: Double,
        kosAlloc: Double,
        savingsAlloc: Double,
        freeSpendingAlloc: Double,
        billAllocations: Map<Int, Double>, // BillID -> Reserved Amount added
        incomeType: String = if (userId == 1) "Monthly Salary" else "Weekly Income"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Log Income
            val incomeLog = IncomeLog(
                userId = userId,
                amount = totalIncome,
                incomeType = incomeType
            )
            repository.insertIncomeLog(incomeLog)

            // 2. Adjust User Balance (Total income is distributed now or saved details)
            val user = repository.getAllUsersInstance().find { it.id == userId }
            if (user != null) {
                // Free spending goes directly into user balance, or wallet tracks it
                val newBal = user.currentBalance + totalIncome - (livingFundAlloc + laundryAlloc + kosAlloc + savingsAlloc + billAllocations.values.sum())
                repository.updateUser(user.copy(currentBalance = (newBal).coerceAtLeast(0.0)))
            }

            // 3. Update Living Fund allocations
            val lf = repository.getLivingFundInstance() ?: LivingFund()
            val updatedLf = lf.copy(
                availableBalance = lf.availableBalance + livingFundAlloc,
                laundryReserve = lf.laundryReserve + laundryAlloc,
                totalFreeSpending = lf.totalFreeSpending + freeSpendingAlloc,
                totalSavings = lf.totalSavings + savingsAlloc
            )
            repository.insertLivingFund(updatedLf)

            // 4. Update Kos Rent split contributions
            val s = repository.getSettingsInstance() ?: SettingsEntity()
            val nextS = if (userId == 1) {
                s.copy(kosRamaContribution = (s.kosRamaContribution + kosAlloc).coerceIn(0.0, s.kosTarget))
            } else {
                s.copy(kosNadiyaContribution = (s.kosNadiyaContribution + kosAlloc).coerceIn(0.0, s.kosTarget))
            }
            repository.updateSettings(nextS)

            // 5. Apply Bill Reserved contributions
            for ((billId, allocAmt) in billAllocations) {
                val dbBill = repository.getAllBillsInstance().find { it.id == billId }
                if (dbBill != null) {
                    val maxPossibleAlloc = dbBill.amount - dbBill.reservedAmount
                    val cleanAlloc = allocAmt.coerceIn(0.0, maxPossibleAlloc)
                    repository.updateBill(dbBill.copy(reservedAmount = dbBill.reservedAmount + cleanAlloc))
                }
            }
        }
    }

    // Spend from Living Fund Wallet manually
    fun spendFromLivingFund(amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val lf = repository.getLivingFundInstance() ?: LivingFund()
            repository.insertLivingFund(lf.copy(availableBalance = (lf.availableBalance - amount).coerceAtLeast(0.0)))
        }
    }

    // Spend from Free Spending manually
    fun spendFreeSpending(amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val lf = repository.getLivingFundInstance() ?: LivingFund()
            repository.insertLivingFund(lf.copy(totalFreeSpending = (lf.totalFreeSpending - amount).coerceAtLeast(0.0)))
        }
    }

    // Allocate directly to Kos rent manually from balance
    fun allocateKosRent(userId: Int, amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val s = repository.getSettingsInstance() ?: SettingsEntity()
            val user = repository.getAllUsersInstance().find { it.id == userId }
            if (user != null && user.currentBalance >= amount) {
                repository.updateUser(user.copy(currentBalance = user.currentBalance - amount))
                val nextS = if (userId == 1) {
                    s.copy(kosRamaContribution = (s.kosRamaContribution + amount).coerceAtLeast(0.0))
                } else {
                    s.copy(kosNadiyaContribution = (s.kosNadiyaContribution + amount).coerceAtLeast(0.0))
                }
                repository.updateSettings(nextS)
            }
        }
    }

    // Pay rent from saved contributions
    fun payRent() {
        viewModelScope.launch(Dispatchers.IO) {
            val s = repository.getSettingsInstance() ?: SettingsEntity()
            // Reset contributions upon pay
            repository.updateSettings(s.copy(kosRamaContribution = 0.0, kosNadiyaContribution = 0.0))
        }
    }

    // Forecast Shift Methods (Never touch real DB!)
    fun shiftForecastMode(days: Int) {
        _forecastOffsetDays.value = _forecastOffsetDays.value + days
    }

    fun resetForecastMode() {
        _forecastOffsetDays.value = 0
    }

    // Live Date calculation relative to shift
    fun getSimulatedCalendar(): Calendar {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, _forecastOffsetDays.value)
        return cal
    }

    // Helper function to calculate Rama's exact payday day in a given year and month
    private fun getRamaPaydayDay(year: Int, monthIndex0: Int): Int {
        val tempCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthIndex0)
            set(Calendar.DAY_OF_MONTH, 28)
        }
        val dayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK)
        return when (dayOfWeek) {
            Calendar.SATURDAY -> 27
            Calendar.SUNDAY -> 26
            else -> 28
        }
    }

    // Helper functions for Dynamic calculations on UI
    fun getSimulatedDaysUntilPayday(userId: Int, cal: Calendar): Int {
        if (userId == 1) {
            // Rama: payday is every 28 of month, but if 28 is weekend, moves faster to 27, and if 27 is weekend also, faster to 26.
            val todayDay = cal.get(Calendar.DAY_OF_MONTH)
            val thisMonthPayday = getRamaPaydayDay(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
            
            return if (todayDay <= thisMonthPayday) {
                thisMonthPayday - todayDay
            } else {
                // Find next month's payday
                val nextMonthCal = cal.clone() as Calendar
                nextMonthCal.add(Calendar.MONTH, 1)
                val nextMonthPayday = getRamaPaydayDay(nextMonthCal.get(Calendar.YEAR), nextMonthCal.get(Calendar.MONTH))
                
                val lastDayOfThisMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val daysRemainingThisMonth = lastDayOfThisMonth - todayDay
                daysRemainingThisMonth + nextMonthPayday
            }
        } else {
            // Nadiya: weekly payday is every Sunday (Sunday = 1)
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val diff = Calendar.SUNDAY - dayOfWeek
            return if (diff >= 0) diff else diff + 7
        }
    }

    // Data Backup Systems (JSON based)
    fun exportBackupJson(): String {
        return try {
            val json = JSONObject()
            // We use standard list calls synchronously for export, which is fine since backup is tapped manually.
            var usersList: List<User> = emptyList()
            var billsList: List<Bill> = emptyList()
            var lf: LivingFund? = null
            var logs: List<IncomeLog> = emptyList()
            var set: SettingsEntity? = null

            usersList = users.value
            billsList = bills.value
            lf = livingFund.value
            logs = incomeLogs.value
            set = settings.value

            // Export Users
            val userArr = JSONArray()
            for (u in usersList) {
                val uJson = JSONObject()
                uJson.put("id", u.id)
                uJson.put("name", u.name)
                uJson.put("incomeType", u.incomeType)
                uJson.put("incomeAmount", u.incomeAmount)
                uJson.put("currentBalance", u.currentBalance)
                uJson.put("rentContribution", u.rentContribution)
                uJson.put("isActive", u.isActive)
                userArr.put(uJson)
            }
            json.put("users", userArr)

            // Export Bills
            val billArr = JSONArray()
            for (b in billsList) {
                val bJson = JSONObject()
                bJson.put("ownerId", b.ownerId)
                bJson.put("name", b.name)
                bJson.put("amount", b.amount)
                bJson.put("reservedAmount", b.reservedAmount)
                bJson.put("day", b.day)
                bJson.put("month", b.month)
                bJson.put("year", b.year)
                bJson.put("installmentIndex", b.installmentIndex ?: -1)
                bJson.put("installmentTotal", b.installmentTotal ?: -1)
                bJson.put("isPaid", b.isPaid)
                bJson.put("isCompleted", b.isCompleted)
                bJson.put("isRecurring", b.isRecurring)
                bJson.put("notes", b.notes)
                billArr.put(bJson)
            }
            json.put("bills", billArr)

            // Export Living Fund
            if (lf != null) {
                val lfJson = JSONObject()
                lfJson.put("availableBalance", lf.availableBalance)
                lfJson.put("laundryReserve", lf.laundryReserve)
                lfJson.put("laundryAllocationPerTwoWeeks", lf.laundryAllocationPerTwoWeeks)
                lfJson.put("foodAllocationPerDay", lf.foodAllocationPerDay)
                lfJson.put("fuelAllocationPerWeek", lf.fuelAllocationPerWeek)
                lfJson.put("totalFreeSpending", lf.totalFreeSpending)
                lfJson.put("totalSavings", lf.totalSavings)
                json.put("livingFund", lfJson)
            }

            // Export Settings
            if (set != null) {
                val setJson = JSONObject()
                setJson.put("language", set.language)
                setJson.put("isCoupleMode", set.isCoupleMode)
                setJson.put("kosName", set.kosName)
                setJson.put("kosTarget", set.kosTarget)
                setJson.put("kosRamaContribution", set.kosRamaContribution)
                setJson.put("kosNadiyaContribution", set.kosNadiyaContribution)
                json.put("settings", setJson)
            }

            json.toString(2)
        } catch (e: Exception) {
            Log.e("Backup", "Export failed", e)
            ""
        }
    }

    fun importBackupJson(backupContent: String): Boolean {
        return try {
            val json = JSONObject(backupContent)
            
            viewModelScope.launch(Dispatchers.IO) {
                repository.clearAllData()

                // Import Settings
                if (json.has("settings")) {
                    val sJ = json.getJSONObject("settings")
                    repository.updateSettings(
                        SettingsEntity(
                            id = 1,
                            language = sJ.optString("language", "en"),
                            isCoupleMode = sJ.optBoolean("isCoupleMode", true),
                            kosName = sJ.optString("kosName", "Kos"),
                            kosTarget = sJ.optDouble("kosTarget", 2000000.0),
                            kosRamaContribution = sJ.optDouble("kosRamaContribution", 0.0),
                            kosNadiyaContribution = sJ.optDouble("kosNadiyaContribution", 0.0)
                        )
                    )
                }

                // Import Users
                if (json.has("users")) {
                    val uA = json.getJSONArray("users")
                    for (i in 0 until uA.length()) {
                        val uJ = uA.getJSONObject(i)
                        repository.insertUser(
                            User(
                                id = uJ.getInt("id"),
                                name = uJ.getString("name"),
                                incomeType = uJ.getString("incomeType"),
                                incomeAmount = uJ.getDouble("incomeAmount"),
                                currentBalance = uJ.getDouble("currentBalance"),
                                rentContribution = uJ.getDouble("rentContribution"),
                                isActive = uJ.optBoolean("isActive", true)
                            )
                        )
                    }
                }

                // Import Living Fund
                if (json.has("livingFund")) {
                    val lfJ = json.getJSONObject("livingFund")
                    repository.insertLivingFund(
                        LivingFund(
                            id = 1,
                            availableBalance = lfJ.optDouble("availableBalance", 0.0),
                            laundryReserve = lfJ.optDouble("laundryReserve", 0.0),
                            laundryAllocationPerTwoWeeks = lfJ.optDouble("laundryAllocationPerTwoWeeks", 50000.0),
                            foodAllocationPerDay = lfJ.optDouble("foodAllocationPerDay", 50000.0),
                            fuelAllocationPerWeek = lfJ.optDouble("fuelAllocationPerWeek", 40000.0),
                            totalFreeSpending = lfJ.optDouble("totalFreeSpending", 0.0),
                            totalSavings = lfJ.optDouble("totalSavings", 0.0)
                        )
                    )
                }

                // Import Bills
                if (json.has("bills")) {
                    val bA = json.getJSONArray("bills")
                    for (i in 0 until bA.length()) {
                        val bJ = bA.getJSONObject(i)
                        repository.insertBill(
                            Bill(
                                ownerId = bJ.getInt("ownerId"),
                                name = bJ.getString("name"),
                                amount = bJ.getDouble("amount"),
                                reservedAmount = bJ.optDouble("reservedAmount", 0.0),
                                day = bJ.getInt("day"),
                                month = bJ.getInt("month"),
                                year = bJ.getInt("year"),
                                installmentIndex = if (bJ.optInt("installmentIndex", -1) != -1) bJ.getInt("installmentIndex") else null,
                                installmentTotal = if (bJ.optInt("installmentTotal", -1) != -1) bJ.getInt("installmentTotal") else null,
                                isPaid = bJ.optBoolean("isPaid", false),
                                isCompleted = bJ.optBoolean("isCompleted", false),
                                isRecurring = bJ.optBoolean("isRecurring", false),
                                notes = bJ.optString("notes", "")
                            )
                        )
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e("Backup", "Import failed", e)
            false
        }
    }

    // Heavy Reset typed function
    fun resetAllDataConfirmed() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllData()
            // Immediately standard seed but clean
            repository.checkAndSeedDatabase()
        }
    }
}
