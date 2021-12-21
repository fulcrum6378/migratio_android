package ir.mahdiparastesh.migratio

import android.content.DialogInterface
import android.graphics.Typeface
import android.os.*
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.get
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import ir.mahdiparastesh.migratio.Fun.Companion.c
import ir.mahdiparastesh.migratio.Fun.Companion.dirLtr
import ir.mahdiparastesh.migratio.Fun.Companion.sp
import ir.mahdiparastesh.migratio.Fun.Companion.textFont
import ir.mahdiparastesh.migratio.Fun.Companion.titleFont
import ir.mahdiparastesh.migratio.Panel.Companion.exCensor
import ir.mahdiparastesh.migratio.adap.ConAdap
import ir.mahdiparastesh.migratio.adap.CriAdap
import ir.mahdiparastesh.migratio.data.*
import ir.mahdiparastesh.migratio.databinding.SelectBinding
import java.util.*
import kotlin.collections.ArrayList

@Suppress("UNCHECKED_CAST")
class Select : AppCompatActivity() {
    private lateinit var b: SelectBinding
    private lateinit var toolbar: Toolbar
    private lateinit var sNav1TV: TextView
    private lateinit var sNav2TV: TextView
    private lateinit var exporter: Exporter

    var conAdap: ConAdap? = null
    var criAdap: CriAdap? = null
    var countries: MutableList<Country>? = null
    var allCriteria: List<Criterion>? = null
    var criteria: MutableList<Criterion>? = null
    var switchedTo2nd = false
    var doSave = true
    var showingHelp = false

    companion object {
        const val exMyCountries = "myCountries"
        const val exSwitchedTo2nd = "switchedTo2nd"
        var handler: Handler? = null
        val conCheck = ArrayList<Boolean>()
        var myCountries: MutableSet<String>? = null
        var myCriteria: ArrayList<MyCriterion>? = null
        var criOFOpened: ArrayList<Boolean>? = null
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = SelectBinding.inflate(layoutInflater)
        setContentView(b.root)
        Fun.init(this, b.root)
        exporter = Exporter(this)

        toolbar = findViewById(R.id.toolbar)
        sNav1TV = b.sNav1[0] as TextView
        sNav2TV = b.sNav2[0] as TextView

        switchedTo2nd = sp.getBoolean(exSwitchedTo2nd, false)
        //Panel.rvScrollY = 0

        // Handler
        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    Works.GET_ALL.ordinal -> when (msg.arg1) {
                        Types.MY_CRITERION.ordinal -> {
                            myCriteria = msg.obj as ArrayList<MyCriterion>
                            if (myCriteria != null) arrangeCriteria()
                            // TODO: ELSE
                        }
                    }

                    Works.SAVE_MY_COUNTRIES.ordinal -> sp.edit().apply {
                        if (countries == null) return@apply
                        var ss = mutableSetOf<String>()
                        for (i in conCheck.indices)
                            if (conCheck[i] && countries!!.size > i) ss.add(countries!![i].tag)
                        putStringSet(exMyCountries, ss)
                        apply()
                    }

                    Works.INSERT_ALL.ordinal -> when (msg.arg1) {
                        Works.NONE.ordinal -> {
                        }
                        Works.EXIT_ON_SAVED.ordinal -> cut()
                        Works.NOTIFY_ON_SAVED.ordinal -> when (msg.arg2) {
                            Types.MY_CRITERION.ordinal -> criAdap?.notifyDataSetChanged()
                        }
                    }

                    Works.BREAK_CENSOR.ordinal -> sp.edit()
                        .putBoolean(exCensor, !sp.getBoolean(exCensor, true)).apply()

                    Works.CLEAR_AND_INSERT_ALL.ordinal -> when (msg.arg1) {
                        Types.MY_CRITERION.ordinal -> when (msg.arg2) {
                            Works.NONE.ordinal -> {
                                myCriteria =
                                    (msg.obj as List<MyCriterion>).toCollection(ArrayList())
                                criAdap?.notifyDataSetChanged()
                            }
                            Works.IMPORT.ordinal -> cut()
                        }
                    }
                }
            }
        }

        // Receive Data
        myCountries = sp.getStringSet(exMyCountries, null)
        myCriteria = null
        if (intent.extras != null) {
            if (intent.extras!!.containsKey("countries") && intent.extras!!.containsKey("criteria")) {
                countries =
                    (intent.extras!!.getParcelableArray("countries") as Array<Parcelable>).toList() as MutableList<Country>
                if (countries != null)
                    Collections.sort(countries!!, Country.Companion.SortCon(1))
                arrangeCountries()
                criteria =
                    (intent.extras!!.getParcelableArray("criteria") as Array<Parcelable>).toList() as MutableList<Criterion>
                allCriteria = criteria?.map { it.copy() }
                if (criteria != null)
                    Collections.sort(criteria!!, Criterion.Companion.SortCri(1))
                val toBeCensored = ArrayList<Int>()
                if (criteria != null) for (cri in criteria!!.indices)
                    if (criteria!![cri].censor > 0 && sp.getBoolean(exCensor, true))
                        toBeCensored.add(cri)
                for (ce in toBeCensored.size - 1 downTo 0)
                    criteria!!.removeAt(toBeCensored[ce])
                if (myCriteria != null) arrangeCriteria()
                else Work(
                    c, handler, Works.GET_ALL, Types.MY_CRITERION,
                    listOf(Types.MY_CRITERION.ordinal)
                ).start()
            } else onBackPressed()
        } else onBackPressed()

        // Loading
        Fun.handleTB(this, toolbar, titleFont)

        // Navigation
        nav()
        if (switchedTo2nd) switchedTo2nd = Fun.switcher(c, b.sSwitcher, dirLtr, false)
        b.sNav1.setOnClickListener {
            if (switchedTo2nd) switchedTo2nd = Fun.switcher(c, b.sSwitcher, dirLtr); nav()
        }
        b.sNav2.setOnClickListener {
            if (!switchedTo2nd) switchedTo2nd = Fun.switcher(c, b.sSwitcher, dirLtr); nav()
        }
        sNav1TV.setTypeface(textFont, Typeface.BOLD)
        sNav2TV.setTypeface(textFont, Typeface.BOLD)

        restoration(savedInstanceState)
    }

    override fun onSaveInstanceState(state: Bundle) {
        state.putBoolean("switchedTo2nd", switchedTo2nd)
        state.putInt("rvConY", b.rvCountries.scrollY)
        state.putInt("rvCriY", b.rvCriteria.scrollY)
        if (criOFOpened != null)
            state.putBooleanArray("criOFOpened", criOFOpened!!.toBooleanArray())
        state.putBoolean("showingHelp", showingHelp)
        super.onSaveInstanceState(state)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        restoration(savedInstanceState)
    }

    override fun onBackPressed() {
        if (!saveFocused() || !doSave) super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.select, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.smSelectAll -> selectAll(true)
        R.id.smDeselectAll -> selectAll(false)
        R.id.smExport -> exporter.export()
        R.id.smImport -> exporter.import()
        R.id.smResetAll -> Fun.alertDialogue2(this, R.string.smResetAll, R.string.sureResetAll,
            DialogInterface.OnClickListener { _, _ ->
                if (allCriteria == null) return@OnClickListener
                Fun.defaultMyCriteria(allCriteria!!, handler)
            })
        R.id.smSources -> Fun.alertDialogue3(
            this@Select, R.string.smSources,
            criteria?.map { it.reference }?.toSet()?.joinToString("\n\n")
                ?: resources.getString(R.string.noInternet),
            copyable = false, linkify = true
        )
        R.id.smHelp -> help()
        else -> super.onOptionsItemSelected(item)
    }


    fun restoration(state: Bundle?) {
        if (state == null) return
        if (state.containsKey("switchedTo2nd")) {
            switchedTo2nd = state.getBoolean("switchedTo2nd"); nav(); }
        if (state.containsKey("rvConY")) b.rvCountries.scrollTo(0, state.getInt("rvConY", 0))
        if (state.containsKey("rvCriY")) b.rvCriteria.scrollTo(0, state.getInt("rvCriY", 0))
        if (state.containsKey("criOFOpened"))
            criOFOpened = state.getBooleanArray("criOFOpened")?.toCollection(ArrayList())
        if (state.getBoolean("showingHelp", false)) help()
    }

    fun arrangeCountries() {
        if (countries == null) return
        conCheck.clear()
        for (i in countries!!.indices)
            conCheck.add(if (myCountries != null) myCountries!!.contains(countries!![i].tag) else false)
        conAdap = ConAdap(c, countries!!)
        b.rvCountries.adapter = conAdap
    }

    fun arrangeCriteria() {
        Collections.sort(criteria!!, Criterion.Companion.SortCri())
        if (criOFOpened == null) {
            criOFOpened = ArrayList()
            for (i in criteria!!) criOFOpened!!.add(false)
        }
        criAdap = CriAdap(c, criteria!!, this)
        b.rvCriteria.adapter = criAdap
    }

    fun nav(dur: Long = resources.getInteger(R.integer.anim_lists_dur).toLong()) {
        var cs = ConstraintSet()// EACH OF ITS CHILDREN MUST HAVE AN ID IN BOTH LAYOUTS
        cs.clone(b.sNav)
        TransitionManager.beginDelayedTransition(b.sNav, AutoTransition().setDuration(dur))
        val att = if (switchedTo2nd) b.sNav2.id else b.sNav1.id
        cs.connect(b.sNavHL.id, ConstraintSet.START, att, ConstraintSet.START)
        cs.connect(b.sNavHL.id, ConstraintSet.END, att, ConstraintSet.END)
        cs.applyTo(b.sNav)
    }

    fun saveFocused(): Boolean {
        var isFocused = false
        if (criteria == null) return isFocused
        for (f in 0 until b.rvCriteria.childCount) {
            var i = b.rvCriteria[f] as ViewGroup
            var overflow = (i[CriAdap.clickablePos] as ViewGroup)[CriAdap.overflowPos] as ViewGroup
            var et = (overflow[CriAdap.ofo2Pos] as ViewGroup)[CriAdap.ofoETPos] as EditText
            if (et.hasFocus()) {
                val cri = criteria!![b.rvCriteria.getChildLayoutPosition(i)]
                doSave = false
                CriAdap.saveMyC(
                    c, CriAdap.findMyC(cri.tag).apply { good = CriAdap.good(1, et, cri.medi) }, true
                )
                isFocused = true
            }
        }
        return isFocused
    }

    fun selectAll(b: Boolean = true): Boolean {
        if (!switchedTo2nd) {
            for (con in conCheck.indices) conCheck[con] = b
            handler?.obtainMessage(Works.SAVE_MY_COUNTRIES.ordinal, null)?.sendToTarget()
            conAdap?.notifyDataSetChanged()
        } else if (myCriteria != null) {
            for (i in myCriteria!!.indices)
                if (shouldBeAdded(myCriteria!![i])) myCriteria!![i].isOn = b
            Work(
                c, handler, Works.INSERT_ALL, Types.MY_CRITERION,
                listOf(myCriteria, Works.NOTIFY_ON_SAVED.ordinal, Types.MY_CRITERION.ordinal)
            ).start()
        }
        return true
    }

    fun shouldBeAdded(mycri: MyCriterion): Boolean =
        Computation.findCriByTag(mycri.tag, criteria!!.toList()) != null

    fun help(): Boolean {
        if (showingHelp) return false
        showingHelp = true
        Fun.alertDialogue1(
            this, R.string.pmHelp, R.string.pHelp, textFont,
            { _, _ -> showingHelp = false }, { showingHelp = false }, true
        )
        return true
    }

    fun cut() {
        try {
            onBackPressed()
            finish()
        } catch (ignored: NullPointerException) {
        }
    }
}
