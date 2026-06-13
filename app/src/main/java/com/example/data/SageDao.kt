package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SageDao {
    // Users
    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<User>>

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<User>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)

    @Update
    suspend fun updateUser(user: User)

    // Bills
    @Query("SELECT * FROM bills")
    fun getAllBillsFlow(): Flow<List<Bill>>

    @Query("SELECT * FROM bills")
    suspend fun getAllBills(): List<Bill>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: Bill)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBills(bills: List<Bill>)

    @Update
    suspend fun updateBill(bill: Bill)

    @Delete
    suspend fun deleteBill(bill: Bill)

    // Living Fund
    @Query("SELECT * FROM living_fund WHERE id = 1")
    fun getLivingFundFlow(): Flow<LivingFund?>

    @Query("SELECT * FROM living_fund WHERE id = 1")
    suspend fun getLivingFund(): LivingFund?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLivingFund(fund: LivingFund)

    @Update
    suspend fun updateLivingFund(fund: LivingFund)

    // Income Logs
    @Query("SELECT * FROM income_logs ORDER BY timestamp DESC")
    fun getAllIncomeLogsFlow(): Flow<List<IncomeLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomeLog(log: IncomeLog)

    // Settings
    @Query("SELECT * FROM settings WHERE id = 1")
    fun getSettingsFlow(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun getSettings(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: SettingsEntity)

    @Update
    suspend fun updateSettings(settings: SettingsEntity)

    // Global operations (e.g. for backup or manual reset)
    @Query("DELETE FROM users")
    suspend fun clearUsers()

    @Query("DELETE FROM bills")
    suspend fun clearBills()

    @Query("DELETE FROM living_fund")
    suspend fun clearLivingFund()

    @Query("DELETE FROM income_logs")
    suspend fun clearIncomeLogs()

    @Query("DELETE FROM settings")
    suspend fun clearSettings()
}
