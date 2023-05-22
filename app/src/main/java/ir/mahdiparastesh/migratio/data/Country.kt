package ir.mahdiparastesh.migratio.data

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import ir.mahdiparastesh.migratio.Fun.Companion.countryNames
import java.text.Collator
import java.util.*

@Suppress("UNCHECKED_CAST", "DEPRECATION")
@Entity
data class Country(
    @PrimaryKey var id: Long,
    var tag: String,
    var continent: Int,
    var attrs: HashMap<String, String>,
    var except: HashMap<String, String>
) : Parcelable {
    private constructor(parcel: Parcel) : this(
        id = parcel.readLong(),
        tag = parcel.readString()!!,
        continent = parcel.readInt(),
        attrs = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            parcel.readSerializable<HashMap<*, *>>(null, HashMap::class.java)
        else parcel.readSerializable()) as HashMap<String, String>,
        except = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            parcel.readSerializable<HashMap<*, *>>(null, HashMap::class.java)
        else parcel.readSerializable()) as HashMap<String, String>
    )

    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeLong(id)
        out.writeString(tag)
        out.writeInt(continent)
        out.writeSerializable(attrs)
        out.writeSerializable(except)
    }

    override fun describeContents() = 0

    companion object {
        @Suppress("unused")
        @JvmField
        val CREATOR = object : Parcelable.Creator<Country> {
            override fun createFromParcel(parcel: Parcel) = Country(parcel)
            override fun newArray(size: Int) = arrayOfNulls<Country>(size)
        }

        class SortCon(val by: Int = 0) : Comparator<Country> {
            override fun compare(a: Country, b: Country) = when (by) {
                1 -> Collator.getInstance(Locale("fa")).compare(
                    countryNames()[ir.mahdiparastesh.migratio.Countries.TAGS.indexOf(a.tag)],
                    countryNames()[ir.mahdiparastesh.migratio.Countries.TAGS.indexOf(b.tag)]
                )

                else -> a.id.compareTo(b.id)
            }
        }
    }
}
