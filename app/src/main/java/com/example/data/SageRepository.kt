package com.example.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class SageRepository(private val sageDao: SageDao) {

    val allUsers: Flow<List<User>> = sageDao.getAllUsersFlow()
    val allBills: Flow<List<Bill>> = sageDao.getAllBillsFlow()
    val livingFund: Flow<LivingFund?> = sageDao.getLivingFundFlow()
    val allIncomeLogs: Flow<List<IncomeLog>> = sageDao.getAllIncomeLogsFlow()
    val settings: Flow<SettingsEntity?> = sageDao.getSettingsFlow()

    suspend fun getSettingsInstance(): SettingsEntity? = sageDao.getSettings()
    suspend fun getLivingFundInstance(): LivingFund? = sageDao.getLivingFund()
    suspend fun getAllUsersInstance(): List<User> = sageDao.getAllUsers()
    suspend fun getAllBillsInstance(): List<Bill> = sageDao.getAllBills()

    suspend fun insertUser(user: User) = sageDao.insertUser(user)
    suspend fun updateUser(user: User) = sageDao.updateUser(user)

    suspend fun insertBill(bill: Bill) = sageDao.insertBill(bill)
    suspend fun insertBills(bills: List<Bill>) = sageDao.insertBills(bills)
    suspend fun updateBill(bill: Bill) = sageDao.updateBill(bill)
    suspend fun deleteBill(bill: Bill) = sageDao.deleteBill(bill)

    suspend fun updateLivingFund(fund: LivingFund) = sageDao.updateLivingFund(fund)
    suspend fun insertLivingFund(fund: LivingFund) = sageDao.insertLivingFund(fund)

    suspend fun insertIncomeLog(log: IncomeLog) = sageDao.insertIncomeLog(log)

    suspend fun updateSettings(settings: SettingsEntity) = sageDao.updateSettings(settings)

    /**
     * Initializes preloaded data ONLY if it doesn't already exist.
     * Guarantees that user data persists across any future APK upgrades without overriding.
     */
    suspend fun checkAndSeedDatabase() {
        val existingUsers = sageDao.getAllUsers()
        if (existingUsers.isNotEmpty()) {
            Log.d("SageRepository", "Database already seeded. Skipping initial loading.")
            verifyAndMigratePreloadedBills()
            return
        }

        Log.d("SageRepository", "First time launch. Creating default users, bills, settings, and living fund state...")

        // 1. Create default users
        val defaultUsers = listOf(
            User(
                id = 1,
                name = "Rama",
                incomeType = "Monthly Salary",
                incomeAmount = 4500000.0,
                currentBalance = 0.0,
                rentContribution = 1000000.0,
                isActive = true
            ),
            User(
                id = 2,
                name = "Nadiya",
                incomeType = "Weekly Income",
                incomeAmount = 0.0, // Dynamic
                currentBalance = 0.0,
                rentContribution = 1000000.0,
                isActive = true
            )
        )
        sageDao.insertUsers(defaultUsers)

        // 2. Create default settings
        val defaultSettings = SettingsEntity(
            id = 1,
            language = "en",
            isCoupleMode = true,
            kosName = "Kos",
            kosTarget = 2000000.0,
            kosRamaContribution = 0.0,
            kosNadiyaContribution = 0.0
        )
        sageDao.insertSettings(defaultSettings)

        // 3. Create default living fund
        val defaultLivingFund = LivingFund(
            id = 1,
            availableBalance = 0.0,
            laundryReserve = 0.0,
            foodAllocationPerDay = 50000.0,
            fuelAllocationPerWeek = 40000.0,
            laundryAllocationPerTwoWeeks = 50000.0,
            totalFreeSpending = 0.0,
            totalSavings = 0.0,
            lastBudgetApportionedDate = System.currentTimeMillis()
        )
        sageDao.insertLivingFund(defaultLivingFund)

        // 4. Create preloaded bills
        val preloadedBills = mutableListOf<Bill>()

        // RAMA BILLS
        // SPINJAM #1: 246000 (28 Jul, 28 Aug, 28 Sep 2026) -> 3 installments
        preloadedBills.add(Bill(ownerId = 1, name = "SPINJAM #1", amount = 246000.0, day = 28, month = 7, year = 2026, installmentIndex = 1, installmentTotal = 3))
        preloadedBills.add(Bill(ownerId = 1, name = "SPINJAM #1", amount = 246000.0, day = 28, month = 8, year = 2026, installmentIndex = 2, installmentTotal = 3))
        preloadedBills.add(Bill(ownerId = 1, name = "SPINJAM #1", amount = 246000.0, day = 28, month = 9, year = 2026, installmentIndex = 3, installmentTotal = 3))

        // SPINJAM #2: 187000 (07 Jul 2026)
        preloadedBills.add(Bill(ownerId = 1, name = "SPINJAM #2", amount = 187000.0, day = 7, month = 7, year = 2026))

        // SPINJAM #3: 187000 (17 Jul 2026)
        preloadedBills.add(Bill(ownerId = 1, name = "SPINJAM #3", amount = 187000.0, day = 17, month = 7, year = 2026))

        // SPINJAM #4: 82000 (18 Jul, 18 Aug, 18 Sep, 18 Oct 2026) -> 4 installments
        preloadedBills.add(Bill(ownerId = 1, name = "SPINJAM #4", amount = 82000.0, day = 18, month = 7, year = 2026, installmentIndex = 1, installmentTotal = 4))
        preloadedBills.add(Bill(ownerId = 1, name = "SPINJAM #4", amount = 82000.0, day = 18, month = 8, year = 2026, installmentIndex = 2, installmentTotal = 4))
        preloadedBills.add(Bill(ownerId = 1, name = "SPINJAM #4", amount = 82000.0, day = 18, month = 9, year = 2026, installmentIndex = 3, installmentTotal = 4))
        preloadedBills.add(Bill(ownerId = 1, name = "SPINJAM #4", amount = 82000.0, day = 18, month = 10, year = 2026, installmentIndex = 4, installmentTotal = 4))

        // SPINJAM #5: 187000 (03 Jul, 03 Aug 2026) -> 2 installments
        preloadedBills.add(Bill(ownerId = 1, name = "SPINJAM #5", amount = 187000.0, day = 3, month = 7, year = 2026, installmentIndex = 1, installmentTotal = 2))
        preloadedBills.add(Bill(ownerId = 1, name = "SPINJAM #5", amount = 187000.0, day = 3, month = 8, year = 2026, installmentIndex = 2, installmentTotal = 2))

        // SPINJAM #6: 187000 (18 Jul, 18 Aug 2026) -> 2 installments
        preloadedBills.add(Bill(ownerId = 1, name = "SPINJAM #6", amount = 187000.0, day = 18, month = 7, year = 2026, installmentIndex = 1, installmentTotal = 2))
        preloadedBills.add(Bill(ownerId = 1, name = "SPINJAM #6", amount = 187000.0, day = 18, month = 8, year = 2026, installmentIndex = 2, installmentTotal = 2))

        // SPINJAM #7: 1477000 (Monthly, Jul 2026 - Jun 2027) -> 12 installments
        for (idx in 1..12) {
            val m = 6 + idx // Jul is 7, Aug is 8, ..., Dec is 12, Jan is 13, etc.
            val actualMonth = if (m > 12) m - 12 else m
            val actualYear = if (m > 12) 2027 else 2026
            preloadedBills.add(
                Bill(
                    ownerId = 1,
                    name = "SPINJAM #7",
                    amount = 1477000.0,
                    day = 15, // Suggested standard mid-month due day
                    month = actualMonth,
                    year = actualYear,
                    installmentIndex = idx,
                    installmentTotal = 12
                )
            )
        }

        // ATOME: 310000 (26 Jul 2026)
        preloadedBills.add(Bill(ownerId = 1, name = "ATOME", amount = 310000.0, day = 26, month = 7, year = 2026))

        // CC Travel: 150000, Recurring Monthly, Due 25
        preloadedBills.add(Bill(ownerId = 1, name = "CC Travel", amount = 150000.0, day = 25, month = 7, year = 2026, isRecurring = true))

        // CC Tokped: 150000, Recurring Monthly, Due 1
        preloadedBills.add(Bill(ownerId = 1, name = "CC Tokped", amount = 150000.0, day = 1, month = 7, year = 2026, isRecurring = true))

        // GPaylater: 594000 (01 Jul 2026)
        preloadedBills.add(Bill(ownerId = 1, name = "GPaylater", amount = 594000.0, day = 1, month = 7, year = 2026))

        // SPaylater: 267000 (11 Jul 2026)
        preloadedBills.add(Bill(ownerId = 1, name = "SPaylater", amount = 267000.0, day = 11, month = 7, year = 2026))

        // SPaylater: 66000 (Monthly, Aug 2026 - May 2027) -> 10 installments
        for (idx in 1..10) {
            val m = 7 + idx // Aug is 8, Sep is 9, Spec says Aug 2026 to May 2027 (10 months)
            val actualMonth = if (m > 12) m - 12 else m
            val actualYear = if (m > 12) 2027 else 2026
            preloadedBills.add(
                Bill(
                    ownerId = 1,
                    name = "SPaylater",
                    amount = 66000.0,
                    day = 11,
                    month = actualMonth,
                    year = actualYear,
                    installmentIndex = idx,
                    installmentTotal = 10
                )
            )
        }

        // Motor: 835000, 30 installments, Starting 28 June 2026
        for (idx in 1..30) {
            val m = 5 + idx // idx=1 is June (6), idx=2 is July (7)
            val actualMonth = (m - 1) % 12 + 1
            val actualYear = 2026 + (m - 1) / 12
            preloadedBills.add(
                Bill(
                    ownerId = 1,
                    name = "Motor",
                    amount = 835000.0,
                    day = 28,
                    month = actualMonth,
                    year = actualYear,
                    installmentIndex = idx,
                    installmentTotal = 30,
                    isRecurring = false
                )
            )
        }

        // HP Rama: 1162000 (31 Jul 2026)
        preloadedBills.add(Bill(ownerId = 1, name = "HP Rama (Final PMT)", amount = 1162000.0, day = 31, month = 7, year = 2026))


        // NADIYA BILLS
        // Spinjam: 657500 (Monthly, Jul 2026 - Feb 2027) -> 8 installments
        for (idx in 1..8) {
            val m = 6 + idx
            val actualMonth = if (m > 12) m - 12 else m
            val actualYear = if (m > 12) 2027 else 2026
            preloadedBills.add(
                Bill(
                    ownerId = 2,
                    name = "Spinjam",
                    amount = 657500.0,
                    day = 1,
                    month = actualMonth,
                    year = actualYear,
                    installmentIndex = idx,
                    installmentTotal = 8
                )
            )
        }

        // Spinjam: 246000 (Nov 2026, Dec 2026) -> 2 installments
        preloadedBills.add(Bill(ownerId = 2, name = "Spinjam", amount = 246000.0, day = 28, month = 11, year = 2026, installmentIndex = 1, installmentTotal = 2))
        preloadedBills.add(Bill(ownerId = 2, name = "Spinjam", amount = 246000.0, day = 28, month = 12, year = 2026, installmentIndex = 2, installmentTotal = 2))

        // HP Nadiya: 1195000 (Monthly, Jul 2026 - Aug 2027) -> 14 installments
        for (idx in 1..14) {
            val m = 6 + idx
            val actualMonth = if (m > 12) m - 12 else m
            val actualYear = if (m > 12) 2027 else 2026
            preloadedBills.add(
                Bill(
                    ownerId = 2,
                    name = "HP Nadiya",
                    amount = 1195000.0,
                    day = 24,
                    month = actualMonth,
                    year = actualYear,
                    installmentIndex = idx,
                    installmentTotal = 14
                )
            )
        }

        // Shopee: 186000 (Jul 2026 - Oct 2026) -> 4 installments
        for (idx in 1..4) {
            val m = 6 + idx
            preloadedBills.add(
                Bill(
                    ownerId = 2,
                    name = "Shopee",
                    amount = 186000.0,
                    day = 18,
                    month = m,
                    year = 2026,
                    installmentIndex = idx,
                    installmentTotal = 4
                )
            )
        }

        // TikTok Paylater: 02 Jul 2026 = 340600, 02 Aug 2026 = 75936, 02 Sep 2026 = 75936
        preloadedBills.add(Bill(ownerId = 2, name = "TikTok Paylater", amount = 340600.0, day = 2, month = 7, year = 2026, installmentIndex = 1, installmentTotal = 3))
        preloadedBills.add(Bill(ownerId = 2, name = "TikTok Paylater", amount = 75936.0, day = 2, month = 8, year = 2026, installmentIndex = 2, installmentTotal = 3))
        preloadedBills.add(Bill(ownerId = 2, name = "TikTok Paylater", amount = 75936.0, day = 2, month = 9, year = 2026, installmentIndex = 3, installmentTotal = 3))

        // GPaylater: 01 Jul 2026 = 320000
        preloadedBills.add(Bill(ownerId = 2, name = "GPaylater", amount = 320000.0, day = 1, month = 7, year = 2026))

        // SPaylater: 11 Jul 2026 = 66000
        preloadedBills.add(Bill(ownerId = 2, name = "SPaylater", amount = 66000.0, day = 11, month = 7, year = 2026))

        sageDao.insertBills(preloadedBills)
        Log.d("SageRepository", "Database successfully seeded with ${preloadedBills.size} bills.")
        verifyAndMigratePreloadedBills()
    }

    /**
     * Verifies and generates missing recurring months and ensures Motor has 30 installments.
     */
    suspend fun verifyAndMigratePreloadedBills() {
        val allBills = sageDao.getAllBills()
        
        // 1. Check if "Motor" needs generation
        val motorBills = allBills.filter { it.name.contains("Motor", ignoreCase = true) }
        val hasMotorInstallments = motorBills.any { it.installmentTotal == 30 }
        
        if (!hasMotorInstallments && motorBills.isNotEmpty()) {
            Log.d("SageRepository", "Migrating Motor bill into 30 installments...")
            for (m in motorBills) {
                sageDao.deleteBill(m)
            }
            
            val newMotorBills = mutableListOf<Bill>()
            for (idx in 1..30) {
                val m = 5 + idx // June 2026 onwards
                val actualMonth = (m - 1) % 12 + 1
                val actualYear = 2026 + (m - 1) / 12
                newMotorBills.add(
                    Bill(
                        ownerId = 1,
                        name = "Motor",
                        amount = 835000.0,
                        day = 28,
                        month = actualMonth,
                        year = actualYear,
                        installmentIndex = idx,
                        installmentTotal = 30,
                        isRecurring = false
                    )
                )
            }
            sageDao.insertBills(newMotorBills)
        }

        // 2. Ensure ALL recurring bills (like Kos, CC Travel, CC Tokped) exist for at least 36 consecutive months
        val recurringB = sageDao.getAllBills().filter { it.isRecurring }
        val groupedByNameAndOwner = recurringB.groupBy { Pair(it.ownerId, it.name) }
        
        val freshGeneratedRecurring = mutableListOf<Bill>()
        for ((key, billsList) in groupedByNameAndOwner) {
            val (ownerId, name) = key
            val earliest = billsList.minByOrNull { it.year * 12 + it.month } ?: continue
            val startMonth = earliest.month
            val startYear = earliest.year
            val day = earliest.day
            val amount = earliest.amount
            val isPriority = earliest.isPriority
            val notes = earliest.notes
            
            for (idx in 1..36) {
                val m = startMonth + idx - 1
                val actualM = (m - 1) % 12 + 1
                val actualY = startYear + (m - 1) / 12
                val alreadyExists = billsList.any { it.month == actualM && it.year == actualY }
                if (!alreadyExists) {
                    freshGeneratedRecurring.add(
                        Bill(
                            ownerId = ownerId,
                            name = name,
                            amount = amount,
                            day = day,
                            month = actualM,
                            year = actualY,
                            isRecurring = true,
                            isPriority = isPriority,
                            notes = notes
                        )
                    )
                }
            }
        }
        if (freshGeneratedRecurring.isNotEmpty()) {
            sageDao.insertBills(freshGeneratedRecurring)
            Log.d("SageRepository", "Dynamically caught up recurring bills: generated ${freshGeneratedRecurring.size} rows.")
        }
    }

    /**
     * Resets all table data and immediately re-seeds default settings.
     */
    suspend fun clearAllData() {
        sageDao.clearUsers()
        sageDao.clearBills()
        sageDao.clearLivingFund()
        sageDao.clearIncomeLogs()
        sageDao.clearSettings()
    }
}
