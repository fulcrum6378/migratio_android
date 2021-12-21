package ir.mahdiparastesh.migratio.adap

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.get
import androidx.recyclerview.widget.RecyclerView
import ir.mahdiparastesh.migratio.Computation
import ir.mahdiparastesh.migratio.Fun
import ir.mahdiparastesh.migratio.Fun.Companion.textFont
import ir.mahdiparastesh.migratio.R
import ir.mahdiparastesh.migratio.data.Country
import kotlin.math.round

class MyConAdap(val c: Context, val list: ArrayList<Computation>, val cons: List<Country>) :
    RecyclerView.Adapter<MyConAdap.MyViewHolder>() {

    class MyViewHolder(val v: ConstraintLayout) : RecyclerView.ViewHolder(v)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        var v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_con, parent, false) as ConstraintLayout
        val tvName = v[tvNamePos] as TextView
        val tvScore = v[tvScorePos] as TextView

        // Fonts
        tvName.setTypeface(textFont, Typeface.BOLD)
        tvScore.setTypeface(textFont, Typeface.BOLD)

        return MyViewHolder(v)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(h: MyViewHolder, i: Int) {
        val tvName = h.v[tvNamePos] as TextView
        val tvScore = h.v[tvScorePos] as TextView
        val separator = h.v[separatorPos]

        // Texts
        tvName.text = "${i + 1}. ${
            Fun.countryNames()[Computation.findConById(
                list[i].id,
                cons
            )!!.id.toInt()]
        }"
        tvScore.text = "${round(list[i].score).toInt()}%"//DecimalFormat("#").format(list[i].score)

        // Clicks
        h.v.setOnClickListener {
            Toast.makeText(c, "${list[h.layoutPosition].score}%", Toast.LENGTH_SHORT).show()
        }

        // Other
        Fun.vish(separator, i != itemCount - 1)
    }

    override fun getItemCount() = list.size


    companion object {
        const val tvNamePos = 0
        const val tvScorePos = 1
        const val separatorPos = 2
    }
}
