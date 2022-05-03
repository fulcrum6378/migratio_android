package ir.mahdiparastesh.migratio

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.get
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import ir.mahdiparastesh.migratio.Panel.Companion.exCensor
import ir.mahdiparastesh.migratio.adap.ConAdap
import ir.mahdiparastesh.migratio.adap.CriAdap
import ir.mahdiparastesh.migratio.data.*
import ir.mahdiparastesh.migratio.databinding.ItemCriBinding
import ir.mahdiparastesh.migratio.databinding.SelectBinding
import ir.mahdiparastesh.migratio.more.BaseActivity
import java.util.*

@SuppressLint("NotifyDataSetChanged")
@Suppress("UNCHECKED_CAST")
class Select : BaseActivity() {
    private lateinit var b: SelectBinding
    private lateinit var sNav1TV: TextView
    private lateinit var sNav2TV: TextView
    private lateinit var exporter: Exporter
    var switchedTo2nd = false
    var doSave = true

    companion object {
        const val exMyCountries = "myCountries"
        const val exSwitchedTo2nd = "switchedTo2nd"
        var handler: Handler? = null
        val conCheck = ArrayList<Boolean>()
        var criOFOpened: ArrayList<Boolean>? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = SelectBinding.inflate(layoutInflater)
        setContentView(b.root)
        exporter = Exporter(this)

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
                            m.myCriteria = msg.obj as ArrayList<MyCriterion>
                            if (m.myCriteria != null) arrangeCriteria()
                            // TODO: ELSE
                        }
                    }

                    Works.SAVE_MY_COUNTRIES.ordinal -> sp.edit().apply {
                        if (m.gotCountries == null) return@apply
                        var ss = mutableSetOf<String>()
                        for (i in conCheck.indices)
                            if (conCheck[i] && m.gotCountries!!.size > i) ss.add(m.gotCountries!![i].tag)
                        putStringSet(exMyCountries, ss)
                        apply()
                    }

                    Works.INSERT_ALL.ordinal -> when (msg.arg1) {
                        Works.NONE.ordinal -> {
                        }
                        Works.EXIT_ON_SAVED.ordinal -> cut()
                        Works.NOTIFY_ON_SAVED.ordinal -> when (msg.arg2) {
                            Types.MY_CRITERION.ordinal -> b.rvCriteria.adapter?.notifyDataSetChanged()
                        }
                    }

                    Works.BREAK_CENSOR.ordinal -> sp.edit()
                        .putBoolean(exCensor, !sp.getBoolean(exCensor, true)).apply()

                    Works.CLEAR_AND_INSERT_ALL.ordinal -> when (msg.arg1) {
                        Types.MY_CRITERION.ordinal -> when (msg.arg2) {
                            Works.NONE.ordinal -> {
                                m.myCriteria =
                                    (msg.obj as List<MyCriterion>).toCollection(ArrayList())
                                b.rvCriteria.adapter?.notifyDataSetChanged()
                            }
                            Works.IMPORT.ordinal -> cut()
                        }
                    }
                }
            }
        }

        // Receive Data
        m.myCountries = sp.getStringSet(exMyCountries, null)
        m.myCriteria = null
        arrangeCountries()
        if (m.myCriteria != null) arrangeCriteria()
        else Work(
            c, handler, Works.GET_ALL, Types.MY_CRITERION, listOf(Types.MY_CRITERION.ordinal)
        ).start()

        // Navigation
        nav()
        if (switchedTo2nd) switchedTo2nd = Fun.switcher(this, b.sSwitcher, dirLtr, false)
        b.sNav1.setOnClickListener {
            if (switchedTo2nd) switchedTo2nd = Fun.switcher(this, b.sSwitcher, dirLtr); nav()
        }
        b.sNav2.setOnClickListener {
            if (!switchedTo2nd) switchedTo2nd = Fun.switcher(this, b.sSwitcher, dirLtr); nav()
        }
        sNav1TV.setTypeface(textFont, Typeface.BOLD)
        sNav2TV.setTypeface(textFont, Typeface.BOLD)

        if (m.showingHelp) help()
    }

    override fun onSaveInstanceState(state: Bundle) {
        state.putBoolean("switchedTo2nd", switchedTo2nd)
        state.putInt("rvConY", b.rvCountries.scrollY)
        state.putInt("rvCriY", b.rvCriteria.scrollY)
        if (criOFOpened != null)
            state.putBooleanArray("criOFOpened", criOFOpened!!.toBooleanArray())
        super.onSaveInstanceState(state)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        restoration(savedInstanceState)
    }

    fun restoration(state: Bundle?) {
        if (state == null) return
        if (state.containsKey("switchedTo2nd")) {
            switchedTo2nd = state.getBoolean("switchedTo2nd"); nav(); }
        if (state.containsKey("rvConY")) b.rvCountries.scrollTo(0, state.getInt("rvConY", 0))
        if (state.containsKey("rvCriY")) b.rvCriteria.scrollTo(0, state.getInt("rvCriY", 0))
        if (state.containsKey("criOFOpened"))
            criOFOpened = state.getBooleanArray("criOFOpened")?.toCollection(ArrayList())
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
                if (m.gotCriteria == null) return@OnClickListener
                defaultMyCriteria(m.gotCriteria!!, handler)
            })
        R.id.smSources -> Fun.alertDialogue3(
            this@Select, R.string.smSources,
            m.gotCriteria?.map { it.reference }?.toSet()?.joinToString("\n\n")
                ?: resources.getString(R.string.noInternet),
            copyable = false, linkify = true
        )
        R.id.smHelp -> help()
        else -> super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (!saveFocused() || !doSave) super.onBackPressed()
    }

    fun arrangeCountries() {
        if (m.gotCountries == null) return
        conCheck.clear()
        for (i in m.gotCountries!!.indices)
            conCheck.add(
                if (m.myCountries != null)
                    m.myCountries!!.contains(m.gotCountries!![i].tag) else false
            )
        b.rvCountries.adapter = ConAdap(this)
    }

    fun arrangeCriteria() {
        Collections.sort(m.gotCriteria!!, Criterion.Companion.SortCri())
        if (criOFOpened == null) {
            criOFOpened = ArrayList()
            for (i in m.gotCriteria!!) criOFOpened!!.add(false)
        }
        b.rvCriteria.adapter = CriAdap(this)
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
        if (m.gotCriteria == null) return isFocused
        for (f in 0 until b.rvCriteria.childCount) {
            val bc = ItemCriBinding.bind(b.rvCriteria[f])
            if (bc.ofo2ET.hasFocus()) {
                val cri = m.gotCriteria!![b.rvCriteria.getChildLayoutPosition(bc.root)]
                doSave = false
                (b.rvCriteria.adapter as CriAdap?)?.also {
                    it.saveMyC(
                        it.findMyC(cri.tag).apply { good = it.good(1, bc.ofo2ET, cri.medi) },
                        true
                    )
                }
                isFocused = true
            }
        }
        return isFocused
    }

    fun selectAll(bb: Boolean = true): Boolean {
        if (!switchedTo2nd) {
            for (con in conCheck.indices) conCheck[con] = bb
            handler?.obtainMessage(Works.SAVE_MY_COUNTRIES.ordinal, null)?.sendToTarget()
            b.rvCountries.adapter?.notifyDataSetChanged()
        } else if (m.myCriteria != null) {
            for (i in m.myCriteria!!.indices)
                if (shouldBeAdded(m.myCriteria!![i])) m.myCriteria!![i].isOn = bb
            Work(
                c, handler, Works.INSERT_ALL, Types.MY_CRITERION,
                listOf(m.myCriteria, Works.NOTIFY_ON_SAVED.ordinal, Types.MY_CRITERION.ordinal)
            ).start()
        }
        return true
    }

    fun shouldBeAdded(mycri: MyCriterion): Boolean =
        m.gotCriteria!!.toList().find { it.tag == mycri.tag } != null

    fun help(): Boolean {
        if (m.showingHelp) return false
        m.showingHelp = true
        Fun.alertDialogue1(
            this, R.string.pmHelp, R.string.pHelp, textFont,
            { _, _ -> m.showingHelp = false }, { m.showingHelp = false }, true
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
