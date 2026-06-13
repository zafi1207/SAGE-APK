package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: Int, // 1 for Rama, 2 for Nadiya
    val name: String,
    val incomeType: String, // "Monthly Salary", "Weekly Income", "Ojol", "Other Income"
    val incomeAmount: Double,
    val currentBalance: Double,
    val rentContribution: Double,
    val isActive: Boolean = true // False if Nadiya is not registered in Personal Mode
)

@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ownerId: Int, // 1 for Rama, 2 for Nadiya
    val name: String,
    val amount: Double,
    val reservedAmount: Double = 0.0, // Amount reserved via allocation (Allocation is NOT payment)
    val day: Int, // Due day (e.g. 28)
    val month: Int, // Due month (e.g. 7 for July)
    val year: Int, // Due year (e.g. 2026)
    val installmentIndex: Int? = null, // e.g. 1
    val installmentTotal: Int? = null, // e.g. 3
    val isPaid: Boolean = false, // Current month payment done
    val isCompleted: Boolean = false, // Completely archived or all installments paid
    val isRecurring: Boolean = false, // True for indefinite monthly bills (recurring)
    val recurringEndMonth: Int? = null,
    val recurringEndYear: Int? = null,
    val notes: String = "",
    val isPriority: Boolean = false
) {
    val remainingAmount: Double
        get() = (amount - reservedAmount).coerceAtLeast(0.0)

    val progressPercent: Int
        get() = if (amount > 0) ((reservedAmount / amount) * 100).coerceIn(0.0, 100.0).toInt() else 0
}

@Entity(tableName = "living_fund")
data class LivingFund(
    @PrimaryKey val id: Int = 1,
    val availableBalance: Double = 0.0, // Running pool for food + fuel
    val laundryReserve: Double = 0.0, // Laundry pool (Order 1 in priorities)
    val foodAllocationPerDay: Double = 50000.0,
    val fuelAllocationPerWeek: Double = 40000.0,
    val laundryAllocationPerTwoWeeks: Double = 50000.0,
    val totalFreeSpending: Double = 0.0,
    val totalSavings: Double = 0.0,
    val lastBudgetApportionedDate: Long = System.currentTimeMillis() // To track daily automatic topup
)

@Entity(tableName = "income_logs")
data class IncomeLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val amount: Double,
    val incomeType: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val language: String = "en", // "en" or "id"
    val isCoupleMode: Boolean = true, // Couple Mode = True, Personal Mode = False
    val kosName: String = "Kos",
    val kosTarget: Double = 2000000.0,
    val kosRamaContribution: Double = 0.0, // Running reserved rent
    val kosNadiyaContribution: Double = 0.0, // Running reserved rent
    val themeMode: String = "Light" // "Light", "Dark", "System"
)
