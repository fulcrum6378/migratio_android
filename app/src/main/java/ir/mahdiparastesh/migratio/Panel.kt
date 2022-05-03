package ir.mahdiparastesh.migratio

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.*
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import ir.mahdiparastesh.migratio.Fun.Companion.connected
import ir.mahdiparastesh.migratio.Fun.Companion.now
import ir.mahdiparastesh.migratio.Fun.Companion.vis
import ir.mahdiparastesh.migratio.adap.MyConAdap
import ir.mahdiparastesh.migratio.data.*
import ir.mahdiparastesh.migratio.databinding.PanelBinding
import ir.mahdiparastesh.migratio.more.BaseActivity
import java.util.*
import kotlin.math.round

@Suppress("UNCHECKED_CAST")
class Panel : BaseActivity() {
    private lateinit var b: PanelBinding
    var allComputations: List<Computation>? = null
    var computations: ArrayList<Computation>? = null
    var canGoToSelect = true
    var anReload: ObjectAnimator? = null
    var selectGuide: AnimatorSet? = null
    var computeSuspendedForRepair = false
    var rvScrollY = 0

    companion object {
        lateinit var handler: Handler
        const val exLastUpdated = "lastUpdated"
        const val exCensor = "censor"
        const val exRepair = "repair"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = PanelBinding.inflate(layoutInflater)
        setContentView(b.root)

        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    Works.DOWNLOAD.ordinal -> if (msg.obj != null) when (msg.arg1) {
                        Types.COUNTRY.ordinal -> Work(
                            c, handler, Works.CLEAR_AND_INSERT_ALL, Types.COUNTRY,
                            listOf(msg.obj as List<Country>, Types.COUNTRY.ordinal)
                        ).start()
                        Types.CRITERION.ordinal -> Work(
                            c, handler, Works.CLEAR_AND_INSERT_ALL, Types.CRITERION,
                            listOf(msg.obj as List<Criterion>, Types.CRITERION.ordinal)
                        ).start()
                    }

                    Works.GET_ALL.ordinal -> when (msg.arg1) {
                        Works.CHECK.ordinal -> when (msg.arg2) {
                            Types.COUNTRY.ordinal -> {
                                m.gotCountries = msg.obj as List<Country>
                                if (m.gotCountries.isNullOrEmpty() || doRefreshData()) {
                                    if (connected) Parse(c, handler, Types.COUNTRY).start()
                                    else noInternet()
                                } else postLoading()
                            }
                            Types.CRITERION.ordinal -> {
                                m.gotCriteria = msg.obj as List<Criterion>
                                if (m.gotCriteria.isNullOrEmpty() || doRefreshData()) {
                                    if (connected) Parse(c, handler, Types.CRITERION).start()
                                    else noInternet()
                                } else postLoading() // defaultMyCriteria() is MESSY here
                            }
                            Types.MY_CRITERION.ordinal -> {
                                m.myCriteria = ArrayList(msg.obj as List<MyCriterion>)
                                if (!needsRepair(true)) compute(rvScrollY)
                            }
                        }
                    }

                    Works.CLEAR_AND_INSERT_ALL.ordinal -> when (msg.arg1) {
                        Types.COUNTRY.ordinal -> {
                            m.gotCountries = msg.obj as List<Country>
                            if (dataLoaded()) loaded()
                            postLoading(true)
                        }
                        Types.CRITERION.ordinal -> {
                            m.gotCriteria = msg.obj as List<Criterion>
                            if (m.myCriteria.isNullOrEmpty())
                                defaultMyCriteria(m.gotCriteria!!, handler)
                            else sp.edit().putBoolean(exRepair, true).apply()
                            postLoading(true)
                        }
                        Types.MY_CRITERION.ordinal -> when (msg.arg2) {
                            Works.REPAIR.ordinal -> {
                                sp.edit().putBoolean(exRepair, false).apply()
                                if (computeSuspendedForRepair) {
                                    computeSuspendedForRepair = false
                                    compute()
                                }
                            }
                        }
                    }
                }
            }
        }

        // Loading
        b.load.setOnClickListener { }
        b.logoText.typeface = logoFont
        b.logoReload.setOnClickListener {
            if (b.loading.visibility == View.VISIBLE) return@setOnClickListener
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
                connected = Fun.isOnlineOld()
            if (connected) {
                anReload = Fun.load1(b.loading, b.logoReload)
                Parse(c, handler, Types.COUNTRY).start()
                Parse(c, handler, Types.CRITERION).start()
            } else noInternet()
        }
        if (m.loaded) b.root.removeView(b.load)
        else if (m.gotCountries == null || m.gotCriteria == null) {
            Work(
                c, handler, Works.GET_ALL, Types.COUNTRY,
                listOf(Works.CHECK.ordinal, Types.COUNTRY.ordinal)
            ).start()
            Work(
                c, handler, Works.GET_ALL, Types.CRITERION,
                listOf(Works.CHECK.ordinal, Types.CRITERION.ordinal)
            ).start()
        }

        // Go to Select
        b.goToSelect.setOnClickListener {
            if (!canGoToSelect || m.gotCountries == null || m.gotCriteria == null)
                return@setOnClickListener
            startActivity(Intent(c, Select::class.java))
            canGoToSelect = false
            object : CountDownTimer(1000, 1000) {
                override fun onTick(p0: Long) {}
                override fun onFinish() {
                    canGoToSelect = true
                }
            }.start()
        }
        selectGuide = Fun.bolden(b.goToSelect, 1.22f)

        // Help
        b.tvRVMCE.setOnClickListener { help() }
        if (m.showingHelp) help()
        if (m.showingAbout) about()
    }

    override fun onResume() {
        super.onResume()
        m.myCountries = sp.getStringSet(Select.exMyCountries, null)
        Work(
            c, handler, Works.GET_ALL, Types.MY_CRITERION,
            listOf(Works.CHECK.ordinal, Types.MY_CRITERION.ordinal)
        ).start()
        canGoToSelect = true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.panel, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val ret = super.onPrepareOptionsMenu(menu)
        (toolbar.menu.findItem(R.id.pmSearch)?.actionView as SearchView?)?.apply {
            findViewById<SearchView.SearchAutoComplete>(androidx.appcompat.R.id.search_src_text)
                ?.setTextColor(ContextCompat.getColor(context, R.color.migratioSearchView1))

            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String) = true

                @SuppressLint("NotifyDataSetChanged")
                override fun onQueryTextChange(newText: String): Boolean {
                    if (allComputations == null || computations == null || m.gotCountries == null ||
                        b.rvMyCon.adapter == null
                    ) return true
                    computations = ArrayList()
                    for (p in allComputations!!)
                        if (Fun.countryNames()[
                                    m.gotCountries!!.find { it.id == p.id }!!.id.toInt()
                            ].contains(newText, true)
                        ) computations!!.add(p)
                    arrange(0)
                    return true
                }
            })
        }
        return ret
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.pmSearch -> true
        R.id.pmRefresh -> {
            refresh(); true; }
        R.id.pmShareResults -> {
            if (allComputations != null && m.gotCountries != null)
                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, resources.getString(R.string.shareResSubject))
                    putExtra(Intent.EXTRA_TEXT, share())
                }, resources.getString(R.string.shareResChooser)))
            true
        }
        R.id.pmHelp -> help()
        R.id.pmAbout -> about()
        else -> super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        rvScrollY = b.rvMyCon.computeVerticalScrollOffset()
    }

    var tapToExit = false
    override fun onBackPressed() {
        if (!tapToExit) {
            Toast.makeText(c, R.string.tapToExit, Toast.LENGTH_LONG).show()
            tapToExit = true
            object : CountDownTimer(3000, 3000) {
                override fun onTick(p0: Long) {}
                override fun onFinish() {
                    tapToExit = false
                }
            }.start(); return
        }
        sp.edit().apply {
            remove(Select.exSwitchedTo2nd)
            apply()
        }
        moveTaskToBack(true)
        Process.killProcess(Process.myPid())
        kotlin.system.exitProcess(0)
    }

    fun refresh() {
        if (connected) {
            Parse(c, handler, Types.COUNTRY).start()
            Parse(c, handler, Types.CRITERION).start()
        }
    }

    fun dataLoaded() = !m.gotCountries.isNullOrEmpty() && !m.gotCriteria.isNullOrEmpty()

    fun loaded() {
        if (m.loaded) return
        m.loaded = true
        ObjectAnimator.ofFloat(
            b.load, "translationX", -dm.widthPixels * 1.5f
        ).apply {
            startDelay = 1110
            duration = 870
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    b.root.removeView(b.load)
                }
            })
            start()
        }
    }

    fun noInternet() {
        Toast.makeText(c, R.string.noInternet, Toast.LENGTH_SHORT).show()
        b.logoText.vis(false)
        b.logoReload.vis()
    }

    fun postLoading(didUpdate: Boolean = false) {
        if (m.gotCountries != null) {
            Collections.sort(m.gotCountries!!, Country.Companion.SortCon(1))
        }
        if (m.gotCriteria != null) {
            Collections.sort(m.gotCriteria!!, Criterion.Companion.SortCri(1))
            if (sp.getBoolean(exCensor, true))
                m.gotCriteria = m.gotCriteria!!.filter { it.censor == 0 }
        }
        if (didUpdate) sp.edit().apply {
            putLong(exLastUpdated, now())
            apply()
        }
        if (dataLoaded()) loaded()
        b.loading.vis(false)
        if (!needsRepair(true)) compute()
    }

    fun needsRepair(computeAfter: Boolean = false): Boolean {
        if (m.myCriteria == null || m.gotCriteria == null) return true
        var rut = sp.getBoolean(exRepair, false)
        if (rut) {
            computeSuspendedForRepair = computeAfter
            Fun.repairMyCriteria(this, m.gotCriteria!!, m.myCriteria!!, handler)
        }
        return rut
    }

    fun doRefreshData() = (now() - sp.getLong(exLastUpdated, 0)) > Fun.doRefreshTime

    fun compute(scrollY: Int = 0) {
        if (m.myCountries.isNullOrEmpty() || m.gotCountries.isNullOrEmpty() ||
            m.gotCriteria.isNullOrEmpty() || m.myCriteria.isNullOrEmpty()
        ) {
            resetMyCon(); return; }
        var atLeastOneCri = false
        for (mycri in m.myCriteria!!) if (mycri.isOn) atLeastOneCri = true
        if (!atLeastOneCri) {
            resetMyCon(); return; }

        selectGuide?.cancel()
        selectGuide = null
        b.goToSelect.scaleX = 1f
        b.goToSelect.scaleY = 1f

        b.rvMyConEmpty.vis(false)
        allComputations =
            Computation.compute(
                m.gotCountries!!,
                m.gotCriteria!!,
                m.myCountries!!.toList(),
                m.myCriteria!!
            )
        if (allComputations == null) return
        computations = allComputations!!.toCollection(ArrayList())
        arrange(scrollY)
    }

    fun arrange(scrollY: Int) {
        b.rvMyCon.adapter = MyConAdap(this)
        b.rvMyCon.scrollBy(0, scrollY)
    }

    fun resetMyCon() {
        b.rvMyConEmpty.vis()
        b.rvMyCon.adapter = null
    }

    fun share(): String {
        if (allComputations == null || m.gotCountries == null) return ""
        val sb = StringBuilder()
        for (p in allComputations!!.indices)
            sb.append(
                "${p + 1}. ${
                    Fun.countryNames()[m.gotCountries!!
                        .find { it.id == allComputations!![p].id }!!.id.toInt()]
                } (${round(allComputations!![p].score).toInt()}%)\n"
            )
        return sb.toString()
    }

    fun help(): Boolean {
        if (m.showingHelp) return false
        m.showingHelp = true
        Fun.alertDialogue1(this, R.string.pmHelp, R.string.pHelp,
            textFont, { _, _ -> m.showingHelp = false }, { m.showingHelp = false }
        )
        return true
    }

    fun about(): Boolean {
        if (m.showingAbout) return false
        m.showingAbout = true
        Fun.alertDialogue1(this, R.string.pmAbout, R.string.about,
            textFont, { _, _ -> m.showingAbout = false }, { m.showingAbout = false }
        )
        return true
    }
}
