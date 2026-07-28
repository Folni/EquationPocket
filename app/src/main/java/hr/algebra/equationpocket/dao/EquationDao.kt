package hr.algebra.equationpocket.dao

import androidx.room.*
import hr.algebra.equationpocket.model.Equation
import hr.algebra.equationpocket.model.Pocket

@Dao
interface EquationDao {

    // --- POCKETS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPocket(pocket: Pocket): Long

    @Query("SELECT * FROM pockets")
    fun getAllPockets(): kotlinx.coroutines.flow.Flow<List<Pocket>>

    @Delete
    suspend fun deletePocket(pocket: Pocket)

    // --- EQUATIONS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquation(equation: Equation): Long

    @Query("SELECT * FROM equations WHERE pocketId = :pocketId")
    fun getEquationsForPocket(pocketId: Long): kotlinx.coroutines.flow.Flow<List<Equation>>

    @Query("SELECT * FROM equations")
    fun getAllEquations(): kotlinx.coroutines.flow.Flow<List<Equation>>

    @Update
    suspend fun updateEquation(equation: Equation)

    @Delete
    suspend fun deleteEquation(equation: Equation)
}
