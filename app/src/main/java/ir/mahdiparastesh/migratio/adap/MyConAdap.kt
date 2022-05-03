package ir.mahdiparastesh.migratio.adap

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import ir.mahdiparastesh.migratio.Fun
import ir.mahdiparastesh.migratio.Fun.Companion.vish
import ir.mahdiparastesh.migratio.Panel
import ir.mahdiparastesh.migratio.databinding.ItemMyConBinding
import ir.mahdiparastesh.migratio.more.AnyViewHolder
import kotlin.math.round

class MyConAdap(val c: Panel) : RecyclerView.Adapter<AnyViewHolder<ItemMyConBinding>>() {

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): AnyViewHolder<ItemMyConBinding> {
        val b = ItemMyConBinding.inflate(c.layoutInflater, parent, false)
        b.tvName.setTypeface(c.textFont, Typeface.BOLD)
        b.tvScore.setTypeface(c.textFont, Typeface.BOLD)
        return AnyViewHolder(b)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(h: AnyViewHolder<ItemMyConBinding>, i: Int) {
        h.b.tvName.text = "${i + 1}. ${
            Fun.countryNames()[c.m.gotCountries!!.find { it.id == c.computations!![i].id }!!.id.toInt()]
        }"

        h.b.tvScore.text = "${round(c.computations!![i].score).toInt()}%"
        //DecimalFormat("#").format(list[i].score)

        h.b.root.setOnClickListener {
            Toast.makeText(c, "${c.computations!![h.layoutPosition].score}%", Toast.LENGTH_SHORT)
                .show()
        }
        h.b.separator.vish(i != itemCount - 1)
    }

    override fun getItemCount() = c.computations?.size ?: 0
}
