package ir.mahdiparastesh.migratio.data

import android.os.Parcel
import android.os.Parcelable
import android.util.JsonReader
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.text.Collator
import java.util.*

@Entity
data class Criterion(
    @PrimaryKey var id: Long,
    var tag: String,
    var name: String,
    var type: String,
    var good: String,
    var medi: String,
    var censor: Int,
    var reference: String
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

    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeLong(id)
        out.writeString(tag)
        out.writeString(name)
        out.writeString(type)
        out.writeString(good)
        out.writeString(medi)
        out.writeInt(censor)
        out.writeString(reference)
    }

    override fun describeContents() = 0

    fun parseName(): String {
        var r = JsonReader(
            InputStreamReader(
                ByteArrayInputStream(name.toByteArray(Charset.forName("UTF-8"))), "UTF-8"
            )
        )
        var array = ArrayList<String>()
        r.beginArray()
        while (r.hasNext()) array.add(r.nextString())
        r.endArray()
        return array[when (Locale.getDefault().language) {
            "fa" -> 1
            else -> 0
        }]
    }

    companion object {
        @Suppress("unused")
        @JvmField
        val CREATOR = object : Parcelable.Creator<Criterion> {
            override fun createFromParcel(parcel: Parcel) = Criterion(parcel)
            override fun newArray(size: Int) = arrayOfNulls<Criterion>(size)
        }

        class SortCri(val by: Int = 0) : Comparator<Criterion> {
            override fun compare(a: Criterion, b: Criterion) = when (by) {
                1 -> Collator.getInstance(Locale("fa")).compare(a.parseName(), b.parseName())
                else -> a.name.compareTo(b.name)
            }
        }
    }
}
