package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.BankTransaction
import com.example.model.DocCollectStatus
import com.example.model.LoanDisbursementInput
import com.example.model.LoanScheme
import com.example.model.UserPreferences
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "local_users",
    indices = [Index(value = ["usernameLower"], unique = true)]
)
data class LocalUserEntity(
    @PrimaryKey val userId: String,
    val username: String,
    val usernameLower: String,
    val displayName: String,
    val passwordHash: String,
    val saltBase64: String,
    val createdAtMillis: Long
)

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["userId"])]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val userId: String,
    val type: String,
    val instrument: String,
    val amountCents: Long,
    val currency: String,
    val merchant: String,
    val category: String,
    val bankName: String,
    val accountLast4: String,
    val source: String,
    val messageHash: String,
    val note: String,
    val transactionTimeMillis: Long,
    val loanAmountCents: Long = 0L,
    val roiPercent: Double = 0.0,
    val tenureMonths: Int = 0,
    val emiCents: Long = 0L,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    fun toDomain(): BankTransaction = BankTransaction(
        id = id,
        userId = userId,
        type = type,
        instrument = instrument,
        amountCents = amountCents,
        currency = currency,
        merchant = merchant,
        category = category,
        bankName = bankName,
        accountLast4 = accountLast4,
        source = source,
        messageHash = messageHash,
        note = note,
        transactionTimeMillis = transactionTimeMillis,
        loanAmountCents = loanAmountCents,
        roiPercent = roiPercent,
        tenureMonths = tenureMonths,
        emiCents = emiCents,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val userId: String,
    val defaultCurrency: String,
    val monthlyBudgetCents: Long
) {
    fun toDomain(): UserPreferences = UserPreferences(
        userId = userId,
        defaultCurrency = defaultCurrency,
        monthlyBudgetCents = monthlyBudgetCents
    )
}

@Entity(
    tableName = "loan_sheets",
    indices = [Index(value = ["userId"])]
)
data class LoanSheetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val userId: String,
    val customerName: String,
    val scheme: String,
    val loanAmount: Double,
    val loanInsuranceSk: Double,
    val pf: Double,
    val rto: Double,
    val stp: Double,
    val vc: Double,
    val forcloseFc: Double,
    val rtoCash: Double,
    val reducingRateIt: Double,
    val tenureMonths: Int,
    val disbursementDateMillis: Long,
    val rcCollect: String,
    val insurancePuc: String,
    val lightBill: String,
    val cheques: String,
    val updatedAtMillis: Long
) {
    fun toDomain(): LoanDisbursementInput = LoanDisbursementInput(
        id = id,
        customerName = customerName,
        scheme = LoanScheme.fromCode(scheme),
        loanAmount = loanAmount,
        loanInsuranceSk = loanInsuranceSk,
        pf = pf,
        rto = rto,
        stp = stp,
        vc = vc,
        forcloseFc = forcloseFc,
        rtoCash = rtoCash,
        reducingRateIt = reducingRateIt,
        tenureMonths = tenureMonths,
        disbursementDateMillis = disbursementDateMillis,
        rcCollect = DocCollectStatus.fromRaw(rcCollect),
        insurancePuc = DocCollectStatus.fromRaw(insurancePuc),
        lightBill = DocCollectStatus.fromRaw(lightBill),
        cheques = cheques
    )
}

@Dao
interface LocalUserDao {
    @Query("SELECT * FROM local_users WHERE usernameLower = :usernameLower LIMIT 1")
    suspend fun findByUsername(usernameLower: String): LocalUserEntity?

    @Query("SELECT * FROM local_users WHERE userId = :userId LIMIT 1")
    suspend fun findById(userId: String): LocalUserEntity?

    @Query("SELECT * FROM local_users ORDER BY createdAtMillis DESC")
    fun observeAllUsers(): Flow<List<LocalUserEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: LocalUserEntity)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY transactionTimeMillis DESC, id DESC")
    fun observeTransactions(userId: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<TransactionEntity>)

    @Query(
        """
        UPDATE transactions SET
            type = :type,
            instrument = :instrument,
            amountCents = :amountCents,
            currency = :currency,
            merchant = :merchant,
            category = :category,
            bankName = :bankName,
            accountLast4 = :accountLast4,
            note = :note,
            transactionTimeMillis = :transactionTimeMillis,
            loanAmountCents = :loanAmountCents,
            roiPercent = :roiPercent,
            tenureMonths = :tenureMonths,
            emiCents = :emiCents,
            updatedAtMillis = :updatedAtMillis
        WHERE id = :id AND userId = :userId
        """
    )
    suspend fun updateOwned(
        id: Long,
        userId: String,
        type: String,
        instrument: String,
        amountCents: Long,
        currency: String,
        merchant: String,
        category: String,
        bankName: String,
        accountLast4: String,
        note: String,
        transactionTimeMillis: Long,
        loanAmountCents: Long,
        roiPercent: Double,
        tenureMonths: Int,
        emiCents: Long,
        updatedAtMillis: Long
    )

    @Query("DELETE FROM transactions WHERE id = :id AND userId = :userId")
    suspend fun deleteOwned(id: Long, userId: String)
}

@Dao
interface UserPreferencesDao {
    @Query("SELECT * FROM user_preferences WHERE userId = :userId LIMIT 1")
    fun observePreferences(userId: String): Flow<UserPreferencesEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserPreferencesEntity)
}

@Dao
interface LoanSheetDao {
    @Query("SELECT * FROM loan_sheets WHERE userId = :userId ORDER BY updatedAtMillis DESC, id DESC")
    fun observeLoanSheets(userId: String): Flow<List<LoanSheetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LoanSheetEntity): Long

    @Query("DELETE FROM loan_sheets WHERE id = :id AND userId = :userId")
    suspend fun deleteOwned(id: Long, userId: String)
}

@Database(
    entities = [
        LocalUserEntity::class,
        TransactionEntity::class,
        UserPreferencesEntity::class,
        LoanSheetEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun localUserDao(): LocalUserDao
    abstract fun transactionDao(): TransactionDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun loanSheetDao(): LoanSheetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ledger_pulse_offline.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
