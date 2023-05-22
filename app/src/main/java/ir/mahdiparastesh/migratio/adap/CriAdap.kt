package ir.mahdiparastesh.migratio.adap

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.TransitionDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import ir.mahdiparastesh.migratio.Fun
import ir.mahdiparastesh.migratio.Fun.Companion.td1Dur
import ir.mahdiparastesh.migratio.Fun.Companion.vis
import ir.mahdiparastesh.migratio.R
import ir.mahdiparastesh.migratio.Select.Companion.criOFOpened
import ir.mahdiparastesh.migratio.Select.Companion.handler
import ir.mahdiparastesh.migratio.data.MyCriterion
import ir.mahdiparastesh.migratio.data.Types
import ir.mahdiparastesh.migratio.data.Work
import ir.mahdiparastesh.migratio.data.Works
import ir.mahdiparastesh.migratio.databinding.ItemCriBinding
import ir.mahdiparastesh.migratio.more.AnyViewHolder
import ir.mahdiparastesh.migratio.more.BaseActivity

class CriAdap(val c: BaseActivity) : RecyclerView.Adapter<AnyViewHolder<ItemCriBinding>>() {
    var scrolling = false
    val overflowHeight = c.dp(250)

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): AnyViewHolder<ItemCriBinding> {
        val b = ItemCriBinding.inflate(c.layoutInflater, parent, false)
        b.tvName.setTypeface(c.textFont, Typeface.BOLD)
        listOf(b.ofo1TV, b.ofo2TV, b.ofo3TV)
            .forEach { it.setTypeface(c.textFont, Typeface.BOLD) }
        b.ofo2ET.setTypeface(c.textFont, Typeface.BOLD)
        b.ofo4SkInfo.setTypeface(c.textFont, Typeface.NORMAL)
        return AnyViewHolder(b)
    }

    override fun onBindViewHolder(h: AnyViewHolder<ItemCriBinding>, i: Int) {
        val swiss = listOf(h.b.ofo1Sw, h.b.ofo2Sw, h.b.ofo3Sw, h.b.ofo2ET)

        // Texts
        h.b.tvName.text = c.m.gotCriteria!![i].parseName()
        h.b.ofo2ET.hint = c.m.gotCriteria!![i].medi

        // Settings
        val myc = findMyC(c.m.gotCriteria!![i].tag)
        h.b.switcher.isChecked = myc.isOn
        resetCheck(h.b.ofo1Sw); resetCheck(h.b.ofo2Sw); resetCheck(h.b.ofo3Sw)
        h.b.ofo2ET.alpha = etAlpha
        h.b.ofo2ET.isEnabled = false
        when (myc.good) {
            "+" -> defCheck(h.b.ofo1Sw)
            "-" -> defCheck(h.b.ofo3Sw)
            else -> {
                defCheck(h.b.ofo2Sw)
                h.b.ofo2ET.setText(myc.good)
                h.b.ofo2ET.alpha = 1f
                h.b.ofo2ET.isEnabled = true
            }
        }
        h.b.overflow.vis(false)
        (h.b.overflow.layoutParams as ConstraintLayout.LayoutParams).apply {
            height = 0; h.b.overflow.layoutParams = this
        }
        if (criOFOpened != null && criOFOpened!!.size > i) if (criOFOpened!![i]) {
            h.b.overflow.vis()
            (h.b.overflow.layoutParams as ConstraintLayout.LayoutParams).apply {
                height = overflowHeight; h.b.overflow.layoutParams = this
            }
        }
        if (h.b.switcher.isChecked) h.b.overflow.alpha = if (myc.isOn) 1f else ofAlpha
        h.b.ofo2ET.isEnabled = myc.isOn
        h.b.ofo4Sk.isEnabled = myc.isOn
        h.b.ofo4Sk.progress = myc.importance
        importanceInfo(c, h.b.ofo4SkInfo, myc.importance)

        // Clicks
        h.b.switcher.setOnCheckedChangeListener { _, b ->
            saveMyC(findMyC(c.m.gotCriteria!![h.layoutPosition].tag).apply { isOn = b })
            h.b.overflow.alpha = if (b) 1f else ofAlpha
            h.b.ofo2ET.isEnabled = b
            h.b.ofo4Sk.isEnabled = b
        }
        h.b.tvName.setOnClickListener {
            if (scrolling) return@setOnClickListener
            scrolling = true
            criOFOpened!![h.layoutPosition] = !criOFOpened!![h.layoutPosition]
            val goDown = criOFOpened!![h.layoutPosition]

            ValueAnimator.ofInt(
                if (goDown) 0 else overflowHeight, if (goDown) overflowHeight else 0
            ).apply {
                duration = 148
                addUpdateListener {
                    (h.b.overflow.layoutParams as ConstraintLayout.LayoutParams).apply {
                        height = it.animatedValue as Int
                        h.b.overflow.layoutParams = this
                    }
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        if (goDown) h.b.overflow.vis()
                        val maxAlpha =
                            if (findMyC(c.m.gotCriteria!![h.layoutPosition].tag).isOn) 1f else ofAlpha
                        ObjectAnimator.ofFloat(h.b.overflow, "alpha", if (goDown) maxAlpha else 0f)
                            .apply { duration = 18; start(); }
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        scrolling = false
                        if (!goDown) h.b.overflow.vis(false)
                    }
                })
                start()
            }
        }
        h.b.tvName.setOnLongClickListener {
            PopupMenu(c, it).apply {
                setOnMenuItemClickListener { it1 ->
                    return@setOnMenuItemClickListener when (it1.itemId) {
                        R.id.clcSource -> {
                            Fun.alertDialogue3(
                                c,
                                R.string.clcSource,
                                c.m.gotCriteria!![h.layoutPosition].reference,
                                copyable = true,
                                linkify = true
                            )
                            true
                        }

                        else -> false
                    }
                }
                inflate(R.menu.cri_long_click)
                show()
            }
            true
        }
        h.b.ofo1.setOnClickListener {
            if (!h.b.switcher.isChecked) return@setOnClickListener
            val cri = c.m.gotCriteria!![h.layoutPosition]
            saveMyC(findMyC(cri.tag).apply { good = radio(swiss, 0, cri.medi) })
        }
        h.b.ofo2.setOnClickListener {
            if (!h.b.switcher.isChecked) return@setOnClickListener
            val cri = c.m.gotCriteria!![h.layoutPosition]
            saveMyC(findMyC(cri.tag).apply { good = radio(swiss, 1, cri.medi) })
        }
        h.b.ofo3.setOnClickListener {
            if (!h.b.switcher.isChecked) return@setOnClickListener
            val cri = c.m.gotCriteria!![h.layoutPosition]
            saveMyC(findMyC(cri.tag).apply { good = radio(swiss, 2, cri.medi) })
        }
        h.b.ofo2ET.setOnFocusChangeListener { view, b ->
            if (view == null || b) return@setOnFocusChangeListener
            val cri = c.m.gotCriteria!![h.layoutPosition]
            saveMyC(findMyC(cri.tag).apply { good = good(1, h.b.ofo2ET, cri.medi) })
        }
        h.b.ofo4Sk.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {
                if (p0 == null) return
                saveMyC(
                    findMyC(c.m.gotCriteria!![h.layoutPosition].tag).apply {
                        importance = p0.progress
                    })
            }

            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                importanceInfo(c, h.b.ofo4SkInfo, p1)
            }
        })
    }

    override fun getItemCount() = c.m.gotCriteria?.size ?: 0

    fun radio(swiss: List<View>, i: Int, med: String): String {
        (swiss[0].background as TransitionDrawable).apply {
            resetTransition(); if (i == 0) startTransition(td1Dur)
        }
        (swiss[1].background as TransitionDrawable).apply {
            resetTransition(); if (i == 1) startTransition(td1Dur)
        }
        (swiss[2].background as TransitionDrawable).apply {
            resetTransition(); if (i == 2) startTransition(td1Dur)
        }
        val ofoET = swiss[3] as EditText
        ofoET.alpha = if (i == 1) 1f else etAlpha
        ofoET.isEnabled = i == 1
        return good(i, ofoET, med)
    }

    fun good(i: Int, et: EditText, med: String): String {
        var value = et.text.toString()
        if (value == "") {
            et.setText(med); value = med; }
        return when (i) {
            0 -> "+"
            1 -> value
            2 -> "-"
            else -> ""
        }
    }

    fun findMyC(tag: String): MyCriterion {
        var pos = -1
        for (i in c.m.myCriteria!!.indices) if (c.m.myCriteria!![i].tag == tag) pos = i
        return c.m.myCriteria!![pos]
    }

    fun findMyCPos(myc: MyCriterion): Int {
        var pos = -1
        for (i in c.m.myCriteria!!.indices) if (c.m.myCriteria!![i].tag == myc.tag) pos = i
        return pos
    }

    fun saveMyC(myc: MyCriterion, exitOnSaved: Boolean = false) {
        if (c.m.myCriteria != null && c.m.myCriteria!!.size > findMyCPos(myc))
            c.m.myCriteria!![findMyCPos(myc)] = myc
        val purp = if (exitOnSaved) Works.EXIT_ON_SAVED else Works.NONE
        Work(
            c.c, handler, Works.INSERT_ALL, Types.MY_CRITERION,
            listOf(listOf(myc), purp.ordinal)
        ).start()
    }

    fun resetCheck(switch: View) {
        (switch.background as TransitionDrawable).resetTransition()
    }

    fun defCheck(switch: View) {
        (switch.background as TransitionDrawable).startTransition(td1Dur)
    }

    @SuppressLint("SetTextI18n")
    fun importanceInfo(c: Context, tv: TextView, importance: Int) {
        tv.text = "${c.resources.getString(R.string.criImportance)} $importance%"
    }

    companion object {
        const val ofAlpha = 0.68f
        const val etAlpha = 0.48f
    }
}
