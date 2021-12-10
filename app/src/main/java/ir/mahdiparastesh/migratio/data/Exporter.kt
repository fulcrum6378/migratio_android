package ir.mahdiparastesh.migratio.data

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import ir.mahdiparastesh.migratio.Fun
import ir.mahdiparastesh.migratio.Fun.Companion.c
import ir.mahdiparastesh.migratio.Fun.Companion.z
import ir.mahdiparastesh.migratio.R
import ir.mahdiparastesh.migratio.Select
import java.io.*
import java.util.*

class Exporter(that: AppCompatActivity) {
    var pack: Exported? = null

    private var exportLauncher: ActivityResultLauncher<Intent> =
        that.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            val bExp = try {
                c.contentResolver.openFileDescriptor(it.data!!.data!!, "w")?.use { des ->
                    FileOutputStream(des.fileDescriptor).use { fos ->
                        fos.write(Gson().toJson(pack).toByteArray())
                        fos.close()
                    }
                }; true
            } catch (ignored: Exception) {
                false
            }
            Toast.makeText(
                c, if (bExp) R.string.exportDone else R.string.exportUndone, Toast.LENGTH_LONG
            ).show()
        }
    private var importLauncher: ActivityResultLauncher<Intent> =
        that.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            var data: String? = null
            try {
                c.contentResolver.openFileDescriptor(it.data!!.data!!, "r")?.use { des ->
                    val sb = StringBuffer()
                    FileInputStream(des.fileDescriptor).apply {
                        var i: Int
                        while (read().also { r -> i = r } != -1) sb.append(i.toChar())
                        close()
                    }
                    data = sb.toString()
                }
                data!!
            } catch (e: Exception) {
                Toast.makeText(c, R.string.importOpenError, Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }
            var imported: Exported
            try {
                imported = Gson().fromJson(data, Exported::class.java)
            } catch (e: Exception) {
                Toast.makeText(c, R.string.importReadError, Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }
            Fun.sp.edit().apply {
                putStringSet(Select.exMyCountries, imported.MYCON)
                apply()
            }
            Work(
                c, Select.handler, Works.CLEAR_AND_INSERT_ALL, Types.MY_CRITERION,
                listOf(imported.MYCRI, Types.MY_CRITERION.ordinal, Works.IMPORT.ordinal)
            ).start()
        }

    companion object {
        val mime = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) "application/octet-stream"
        else "application/json"
    }

    fun export(): Boolean {
        if (Select.myCountries == null || Select.myCriteria == null) return true
        pack = Exported(Select.myCountries!!, Select.myCriteria!!)
        val d = Calendar.getInstance()
        val date =
            "${d[Calendar.YEAR]}-${z(d[Calendar.MONTH] + 1)}-${z(d[Calendar.DAY_OF_MONTH])}"
        val time =
            "${z(d[Calendar.HOUR_OF_DAY])}-${z(d[Calendar.MINUTE])}-${z(d[Calendar.SECOND])}"
        exportLauncher.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mime
            putExtra(Intent.EXTRA_TITLE, "Migratio Settings $date - $time.json")
        })
        return true
    }

    fun import(): Boolean {
        importLauncher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mime
        })
        return true
    }


    data class Exported(var MYCON: MutableSet<String>, var MYCRI: ArrayList<MyCriterion>)
}
