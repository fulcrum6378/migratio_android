package ir.mahdiparastesh.migratio.data

import androidx.room.*
import androidx.room.Database

@Database(
    entities = [Country::class, Criterion::class, MyCriterion::class],
    version = 2, exportSchema = false
)
@TypeConverters(Converters::class)
abstract class Database : RoomDatabase() {
    abstract fun dao(): Dao

    @androidx.room.Dao
    interface Dao {
        @Query("SELECT * FROM Country")
        fun getCountries(): List<Country>

        @Query("SELECT * FROM Criterion")
        fun getCriteria(): List<Criterion>

        @Query("SELECT * FROM MyCriterion")
        fun getMyCriteria(): List<MyCriterion>

        @Query("SELECT * FROM Country WHERE id LIKE :id LIMIT 1")
        fun getCountry(id: Long): Country

        @Query("SELECT * FROM Criterion WHERE id LIKE :id LIMIT 1")
        fun getCriterion(id: Long): Criterion

        @Query("SELECT * FROM MyCriterion WHERE id LIKE :id LIMIT 1")
        fun getMyCriterion(id: Long): MyCriterion

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun insertCountries(item: List<Country>)

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun insertCriteria(item: List<Criterion>)

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun insertMyCriteria(item: List<MyCriterion>)

        @Delete
        fun deleteCountries(item: List<Country>)

        @Delete
        fun deleteCriteria(item: List<Criterion>)

        @Delete
        fun deleteMyCriteria(item: List<MyCriterion>)
    }
}
