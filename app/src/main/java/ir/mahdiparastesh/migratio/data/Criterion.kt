package ir.mahdiparastesh.migratio.data

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ir.mahdiparastesh.migratio.Fun
import java.text.Collator
import java.util.*

@Entity
data class Criterion(
    @PrimaryKey(autoGenerate = false) var id: Long,
    @ColumnInfo(name = TAG) var tag: String,
    @ColumnInfo(name = NAME) var name: String,
    @ColumnInfo(name = TYPE) var type: String,
    @ColumnInfo(name = GOOD) var good: String,
    @ColumnInfo(name = MEDI) var medi: String,
    @ColumnInfo(name = CENSOR) var censor: Int,
    @ColumnInfo(name = REFERENCE) var reference: String
) : Parcelable {
    private constructor(parcel: Parcel) : this(
        id = parcel.readLong(),
        tag = parcel.readString()!!,
        name = parcel.readString()!!,
        type = parcel.readString()!!,
        good = parcel.readString()!!,
        medi = parcel.readString()!!,
        censor = parcel.readInt(),
        reference = parcel.readString()!!
    )

    override fun writeToParcel(out: Parcel?, flags: Int) {
        out?.writeLong(id)
        out?.writeString(tag)
        out?.writeString(name)
        out?.writeString(type)
        out?.writeString(good)
        out?.writeString(medi)
        out?.writeInt(censor)
        out?.writeString(reference)
    }

    override fun describeContents() = 0

    fun parseName(): String {
        var r = Fun.jsonReader(name)
        var array = ArrayList<String>()
        r.beginArray()
        while (r.hasNext()) array.add(r.nextString())
        r.endArray()
        return array[when (Locale.getDefault().language) {
            "fa" -> 1
            else -> 0
        }]
    }

    @Suppress("unused")
    companion object {
        const val CRITERION = "criterion"
        const val ID = "id"
        const val TAG = "tag"
        const val NAME = "name"
        const val TYPE = "type"
        const val GOOD = "good"
        const val MEDI = "medi"
        const val CENSOR = "censor"
        const val REFERENCE = "reference"

        @JvmField
        val CREATOR = object : Parcelable.Creator<Criterion> {
            override fun createFromParcel(parcel: Parcel) = Criterion(parcel)
            override fun newArray(size: Int) = arrayOfNulls<Criterion>(size)
        }

        @Suppress("SpellCheckingInspection")
        val TAGS = arrayOf(
            "airpol", "humdev", "gayhap", "lngepi", "langen", "avgwth", "sumwin", "crimer",
            "worisk", "gpeace", "dnganm", "suicid", "sunshn", "frerel", "frebio", "fredrg",
            "fresex", "migacc", "racism", "intusr", "inflat", "unempl", "conscr"
        )


        class SortCri(val by: Int = 0) : Comparator<Criterion> {
            override fun compare(a: Criterion, b: Criterion) = when (by) {
                1 -> Collator.getInstance(Locale("fa")).compare(a.parseName(), b.parseName())
                else -> a.name.compareTo(b.name)
            }
        }
    }
}
