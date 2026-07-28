package hr.algebra.equationpocket.dao

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import hr.algebra.equationpocket.model.Equation
import hr.algebra.equationpocket.model.Pocket

@Database(entities = [Pocket::class, Equation::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun equationDao(): EquationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "equation_pocket_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
