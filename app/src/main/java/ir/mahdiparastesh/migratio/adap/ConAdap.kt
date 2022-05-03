package ir.mahdiparastesh.migratio.adap

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.graphics.drawable.TransitionDrawable
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ir.mahdiparastesh.migratio.Fun
import ir.mahdiparastesh.migratio.Fun.Companion.td1Dur
import ir.mahdiparastesh.migratio.Select
import ir.mahdiparastesh.migratio.Select.Companion.conCheck
import ir.mahdiparastesh.migratio.data.Continents
import ir.mahdiparastesh.migratio.data.Works
import ir.mahdiparastesh.migratio.databinding.ItemConBinding
import ir.mahdiparastesh.migratio.more.AnyViewHolder
import ir.mahdiparastesh.migratio.more.BaseActivity

class ConAdap(val c: BaseActivity) : RecyclerView.Adapter<AnyViewHolder<ItemConBinding>>() {

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): AnyViewHolder<ItemConBinding> {
        val b = ItemConBinding.inflate(c.layoutInflater, parent, false)
        b.tvName.setTypeface(c.textFont, Typeface.BOLD)
        b.tvCont.setTypeface(c.textFont, Typeface.NORMAL)
        return AnyViewHolder(b)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(h: AnyViewHolder<ItemConBinding>, i: Int) {
        h.b.tvName.text = "${i + 1}. ${Fun.countryNames()[c.m.gotCountries!![i].id.toInt()]}"
        h.b.tvCont.text = c.resources.getString(
            Continents.values()[c.m.gotCountries!![i].continent].label
        )

        (h.b.check.background as TransitionDrawable).apply {
            resetTransition()
            if (conCheck[i]) startTransition(td1Dur)
        }
        h.b.clickable.setOnClickListener {
            if (conCheck.size <= h.layoutPosition) return@setOnClickListener
            (h.b.check.background as TransitionDrawable).apply {
                conCheck[h.layoutPosition] = !conCheck[h.layoutPosition]
                if (conCheck[h.layoutPosition]) startTransition(td1Dur)
                else reverseTransition(td1Dur)
            }
            Select.handler?.obtainMessage(Works.SAVE_MY_COUNTRIES.ordinal, null)?.sendToTarget()
        }
    }

    override fun getItemCount() = c.m.gotCountries?.size ?: 0
}
