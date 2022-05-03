package ir.mahdiparastesh.migratio.data

import android.content.Context
import android.os.Handler
import androidx.room.Room
import ir.mahdiparastesh.migratio.Fun

@Suppress("UNCHECKED_CAST")
class Work(
    val c: Context,
    val handler: Handler?,
    val action: Works,
    val type: Types,
    val values: List<Any?>? = null
) : Thread() {

    override fun run() {
        var db = Room.databaseBuilder(c, Database::class.java, Fun.defDataDB)
            .fallbackToDestructiveMigration()
            .build()
        var dao = db.dao()
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (action) {
            Works.GET_ALL -> handler?.obtainMessage(
                action.ordinal,
                if (values != null && values.isNotEmpty()) values[0] as Int else 0,
                if (values != null && values.size > 1) values[1] as Int else 0,
                when (type) {
                    Types.COUNTRY -> dao.getCountries()
                    Types.CRITERION -> dao.getCriteria()
                    Types.MY_CRITERION -> dao.getMyCriteria()
                }
            )?.sendToTarget()

            Works.GET_ONE -> if (values != null && values.isNotEmpty()) handler?.obtainMessage(
                action.ordinal,
                if (values.size > 1) values[1] as Int else 0,
                if (values.size > 2) values[2] as Int else 0,
                when (type) {
                    Types.COUNTRY -> dao.getCountry(values[0] as Long)
                    Types.CRITERION -> dao.getCriterion(values[0] as Long)
                    Types.MY_CRITERION -> dao.getMyCriterion(values[0] as Long)
                }
            )?.sendToTarget()

            Works.INSERT_ALL -> if (values != null && values.isNotEmpty()) {
                when (type) {
                    Types.COUNTRY -> dao.insertCountries(values[0] as List<Country>)
                    Types.CRITERION -> dao.insertCriteria(values[0] as List<Criterion>)
                    Types.MY_CRITERION -> dao.insertMyCriteria(values[0] as List<MyCriterion>)
                }
                handler?.obtainMessage(
                    action.ordinal, if (values.size > 1) values[1] as Int else 0,
                    if (values.size > 2) values[2] as Int else 0, when (type) {
                        Types.COUNTRY -> dao.getCountries()
                        Types.CRITERION -> dao.getCriteria()
                        Types.MY_CRITERION -> dao.getMyCriteria()
                    }
                )?.sendToTarget()
            }

            Works.DELETE_ALL -> if (values != null && values.isNotEmpty()) {
                when (type) {
                    Types.COUNTRY -> dao.deleteCountries(values[0] as List<Country>)
                    Types.CRITERION -> dao.deleteCriteria(values[0] as List<Criterion>)
                    Types.MY_CRITERION -> dao.deleteMyCriteria(values[0] as List<MyCriterion>)
                }
                handler?.obtainMessage(
                    action.ordinal, if (values.size > 1) values[1] as Int else 0,
                    if (values.size > 2) values[2] as Int else 0, null
                )?.sendToTarget()
            }

            Works.CLEAR_AND_INSERT_ALL -> if (values != null && values.isNotEmpty()) {
                when (type) {
                    Types.COUNTRY -> {
                        dao.deleteCountries(dao.getCountries())
                        dao.insertCountries(values[0] as List<Country>)
                    }
                    Types.CRITERION -> {
                        dao.deleteCriteria(dao.getCriteria())
                        dao.insertCriteria(values[0] as List<Criterion>)
                    }
                    Types.MY_CRITERION -> {
                        dao.deleteMyCriteria(dao.getMyCriteria())
                        dao.insertMyCriteria(values[0] as List<MyCriterion>)
                    }
                }
                handler?.obtainMessage(
                    action.ordinal, if (values.size > 1) values[1] as Int else 0,
                    if (values.size > 2) values[2] as Int else 0, when (type) {
                        Types.COUNTRY -> dao.getCountries()
                        Types.CRITERION -> dao.getCriteria()
                        Types.MY_CRITERION -> dao.getMyCriteria()
                    }
                )?.sendToTarget()
            }
        }
        db.close()
    }
}
