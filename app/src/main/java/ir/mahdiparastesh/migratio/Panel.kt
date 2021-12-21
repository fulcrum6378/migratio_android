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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import ir.mahdiparastesh.migratio.Fun.Companion.c
import ir.mahdiparastesh.migratio.Fun.Companion.connected
import ir.mahdiparastesh.migratio.Fun.Companion.defaultMyCriteria
import ir.mahdiparastesh.migratio.Fun.Companion.dm
import ir.mahdiparastesh.migratio.Fun.Companion.logoFont
import ir.mahdiparastesh.migratio.Fun.Companion.now
import ir.mahdiparastesh.migratio.Fun.Companion.sp
import ir.mahdiparastesh.migratio.Fun.Companion.textFont
import ir.mahdiparastesh.migratio.Fun.Companion.titleFont
import ir.mahdiparastesh.migratio.Fun.Companion.vis
import ir.mahdiparastesh.migratio.adap.MyConAdap
import ir.mahdiparastesh.migratio.data.*
import ir.mahdiparastesh.migratio.databinding.PanelBinding
import kotlin.math.round
import ir.mahdiparastesh.migratio.data.Criterion as Criterion1

// adb connect 192.168.1.20:

@Suppress("UNCHECKED_CAST")
class Panel : AppCompatActivity() {
    private lateinit var b: PanelBinding
    private lateinit var toolbar: Toolbar

    var gotCriteria: List<Criterion1>? = null
    var myCriteria: List<MyCriterion>? = null
    var tapToExit = false
    var loaded = false
    var canGoToSelect = true
    var myCountries: MutableSet<String>? = null
    var anReload: ObjectAnimator? = null
    var selectGuide: AnimatorSet? = null
    var showingHelp = false
    var showingAbout = false
    var computeSuspendedForRepair = false

    companion object {
        lateinit var rvMyCon: RecyclerView
        lateinit var handler: Handler

        const val exLastUpdated = "lastUpdated"
        const val exCensor = "censor"
        const val exRepair = "repair"
        var gotCountries: List<Country>? = null

        @SuppressLint("StaticFieldLeak")
        var myconAdapter: MyConAdap? = null
        var rvScrollY = 0
        var allComputations: List<Computation>? = null
        var computations: ArrayList<Computation>? = null

        fun search(text: String) {
            if (allComputations == null || computations == null || gotCountries == null || myconAdapter == null) return
            computations = ArrayList()
            for (p in allComputations!!)
                if (Fun.countryNames()[
                            Computation.findConById(p.id, gotCountries!!)!!.id.toInt()
                    ].contains(text, true)
                ) computations!!.add(p)
            arrange(0)
        }

        fun arrange(scrollY: Int) {
            myconAdapter = MyConAdap(c, computations!!, gotCountries!!)
            rvMyCon.adapter = myconAdapter
            rvMyCon.scrollBy(0, scrollY)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = PanelBinding.inflate(layoutInflater)
        setContentView(b.root)
        Fun.init(this, b.root)

        toolbar = findViewById(R.id.toolbar)
        rvMyCon = findViewById(R.id.rvMyCon)

        rvScrollY = 0
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
                            listOf(msg.obj as List<Criterion1>, Types.CRITERION.ordinal)
                        ).start()
                    }

                    Works.GET_ALL.ordinal -> when (msg.arg1) {
                        Works.CHECK.ordinal -> when (msg.arg2) {
                            Types.COUNTRY.ordinal -> {
                                gotCountries = msg.obj as List<Country>
                                if (gotCountries.isNullOrEmpty() || doRefreshData()) {
                                    if (connected) Parse(c, handler, Types.COUNTRY).start()
                                    else noInternet()
                                } else postLoading()
                            }
                            Types.CRITERION.ordinal -> {
                                gotCriteria = msg.obj as List<Criterion1>
                                if (gotCriteria.isNullOrEmpty() || doRefreshData()) {
                                    if (connected) Parse(c, handler, Types.CRITERION).start()
                                    else noInternet()
                                } else postLoading()// defaultMyCriteria() is MESSY here
                            }
                            Types.MY_CRITERION.ordinal -> {
                                myCriteria = msg.obj as List<MyCriterion>
                                if (!needsRepair(true)) compute(rvScrollY)
                            }
                        }
                    }

                    Works.CLEAR_AND_INSERT_ALL.ordinal -> when (msg.arg1) {
                        Types.COUNTRY.ordinal -> {
                            gotCountries = msg.obj as List<Country>
                            if (dataLoaded()) loaded()
                            postLoading(true)
                        }
                        Types.CRITERION.ordinal -> {
                            gotCriteria = msg.obj as List<Criterion1>
                            if (myCriteria.isNullOrEmpty())
                                defaultMyCriteria(gotCriteria!!, handler)
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
        restoration(savedInstanceState)


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
        if (loaded) b.root.removeView(b.load)
        else if (gotCountries == null || gotCriteria == null) {
            Work(
                c, handler, Works.GET_ALL, Types.COUNTRY,
                listOf(Works.CHECK.ordinal, Types.COUNTRY.ordinal)
            ).start()
            Work(
                c, handler, Works.GET_ALL, Types.CRITERION,
                listOf(Works.CHECK.ordinal, Types.CRITERION.ordinal)
            ).start()
        }
        Fun.handleTB(this, toolbar, titleFont)

        // Go to Select
        b.goToSelect.setOnClickListener {
            if (!canGoToSelect || gotCountries == null || gotCriteria == null)
                return@setOnClickListener
            startActivity(
                Intent(c, Select::class.java)
                    .putExtra("countries", gotCountries!!.toTypedArray())
                    .putExtra("criteria", gotCriteria!!.toTypedArray())
            )
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
    }

    override fun onResume() {
        super.onResume()

        // List (DOESN'T NEED INTERNET!)
        myCountries = sp.getStringSet(Select.exMyCountries, null)
        /*object : CountDownTimer(5000, 50) {
            override fun onFinish() {}
            override fun onTick(millisUntilFinished: Long) {
                if (myCountries.isNullOrEmpty()) return
                cancel()
            }
        }.start()*/
        Work(
            c, handler, Works.GET_ALL, Types.MY_CRITERION,
            listOf(Works.CHECK.ordinal, Types.MY_CRITERION.ordinal)
        ).start()

        // Other
        canGoToSelect = true
    }

    override fun onPause() {
        super.onPause()
        rvScrollY = rvMyCon.computeVerticalScrollOffset()
    }

    override fun onSaveInstanceState(state: Bundle) {
        state.putBoolean("loaded", loaded)
        state.putBoolean("showingHelp", showingHelp)
        state.putBoolean("showingAbout", showingAbout)
        if (gotCountries != null)
            state.putParcelableArray("gotCountries", gotCountries!!.toTypedArray())
        if (gotCriteria != null)
            state.putParcelableArray("gotCriteria", gotCriteria!!.toTypedArray())
        state.putInt("rvScrollY", rvMyCon.scrollY)
        super.onSaveInstanceState(state)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        restoration(savedInstanceState)
    }

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
        kotlin.system.exitProcess(1)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.panel, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.pmSearch -> true
        R.id.pmRefresh -> {
            refresh(); true
        }
        R.id.pmShareResults -> {
            if (allComputations != null && gotCountries != null)
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


    fun restoration(state: Bundle?) {
        if (state == null) return
        loaded = state.getBoolean("loaded", false)
        if (state.getBoolean("showingHelp", false)) help()
        if (state.getBoolean("showingAbout", false)) about()
        if (gotCountries == null)
            gotCountries = state.getParcelableArray("gotCountries")?.toList() as List<Country>
        if (gotCriteria == null)
            gotCriteria = state.getParcelableArray("gotCriteria")?.toList() as List<Criterion1>
    }

    fun refresh() {
        if (connected) {
            Parse(c, handler, Types.COUNTRY).start()
            Parse(c, handler, Types.CRITERION).start()
        }
    }

    fun dataLoaded() = !gotCountries.isNullOrEmpty() && !gotCriteria.isNullOrEmpty()

    fun loaded() {
        if (loaded) return
        loaded = true
        ObjectAnimator.ofFloat(b.load, "translationX", -dm.widthPixels * 1.5f).apply {
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
        vis(b.logoText, false)
        vis(b.logoReload)
    }

    fun postLoading(didUpdate: Boolean = false) {
        if (didUpdate) sp.edit().apply {
            putLong(exLastUpdated, now())
            apply()
        }
        if (dataLoaded()) loaded()
        vis(b.loading, false)
        if (!needsRepair(true)) compute()
    }

    fun needsRepair(computeAfter: Boolean = false): Boolean {
        if (myCriteria == null || gotCriteria == null) return true
        var rut = sp.getBoolean(exRepair, false)
        if (rut) {
            computeSuspendedForRepair = computeAfter
            Fun.repairMyCriteria(gotCriteria!!, myCriteria!!, handler)
        }
        return rut
    }

    fun doRefreshData() = (now() - sp.getLong(exLastUpdated, 0)) > Fun.doRefreshTime

    fun compute(scrollY: Int = 0) {
        if (myCountries.isNullOrEmpty() || gotCountries.isNullOrEmpty() ||
            gotCriteria.isNullOrEmpty() || myCriteria.isNullOrEmpty()
        ) {
            resetMyCon(); return; }
        var atLeastOneCri = false
        for (mycri in myCriteria!!) if (mycri.isOn) atLeastOneCri = true
        if (!atLeastOneCri) {
            resetMyCon(); return; }

        selectGuide?.cancel()
        selectGuide = null
        b.goToSelect.scaleX = 1f
        b.goToSelect.scaleY = 1f

        vis(b.rvMyConEmpty, false)
        allComputations =
            Computation.compute(gotCountries!!, gotCriteria!!, myCountries!!.toList(), myCriteria!!)
        if (allComputations == null) return
        computations = allComputations!!.toCollection(ArrayList())
        arrange(scrollY)
    }

    fun resetMyCon() {
        vis(b.rvMyConEmpty)
        myconAdapter = null
        rvMyCon.adapter = null
    }

    fun share(): String {
        if (allComputations == null || gotCountries == null) return ""
        val sb = StringBuilder()
        for (p in allComputations!!.indices)
            sb.append(
                "${p + 1}. ${
                    Fun.countryNames()[Computation.findConById(
                        allComputations!![p].id, gotCountries!!
                    )!!.id.toInt()]
                } (${round(allComputations!![p].score).toInt()}%)\n"
            )
        return sb.toString()
    }

    fun help(): Boolean {
        if (showingHelp) return false
        showingHelp = true
        Fun.alertDialogue1(this, R.string.pmHelp, R.string.pHelp, textFont,
            { _, _ -> showingHelp = false }, { showingHelp = false }
        )
        return true
    }

    fun about(): Boolean {
        if (showingAbout) return false
        showingAbout = true
        Fun.alertDialogue1(this, R.string.pmAbout, R.string.about, textFont,
            { _, _ -> showingAbout = false }, { showingAbout = false }
        )
        return true
    }
}
