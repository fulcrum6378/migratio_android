package ir.mahdiparastesh.migratio.data

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MyCriterion(
    @PrimaryKey(autoGenerate = true) var id: Long,
    var tag: String,
    var isOn: Boolean,
    var good: String,
    var importance: Int
) : Parcelable {
    private constructor(parcel: Parcel) : this(
        id = parcel.readLong(),
        tag = parcel.readString()!!,
        isOn = parcel.readByte() == (1).toByte(),
        good = parcel.readString()!!,
        importance = parcel.readInt()
    )

    override fun writeToParcel(out: Parcel?, flags: Int) {
        out?.writeLong(id)
        out?.writeString(tag)
        out?.writeByte(if (isOn) 1 else 2)
        out?.writeString(good)
        out?.writeInt(importance)
    }

    override fun describeContents() = 0

    @Suppress("SpellCheckingInspection")
    companion object {
        @Suppress("unused")
        @JvmField
        val CREATOR = object : Parcelable.Creator<MyCriterion> {
            override fun createFromParcel(parcel: Parcel) = MyCriterion(parcel)
            override fun newArray(size: Int) = arrayOfNulls<MyCriterion>(size)
        }
    }
}
