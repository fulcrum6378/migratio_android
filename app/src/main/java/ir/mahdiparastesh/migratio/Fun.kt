package ir.mahdiparastesh.migratio

import android.Manifest
import android.animation.*
import android.annotation.SuppressLint
import android.content.*
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkInfo
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.text.util.Linkify
import android.util.DisplayMetrics
import android.util.JsonReader
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import ir.mahdiparastesh.migratio.data.*
import ir.mahdiparastesh.migratio.more.Fonts
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.*

@Suppress("UNUSED_PARAMETER", "unused")
class Fun {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var c: Context
        lateinit var sp: SharedPreferences
        lateinit var logoFont: Typeface
        lateinit var titleFont: Typeface
        lateinit var textFont: Typeface
        lateinit var current: AppCompatActivity

        const val defDataDB = "data"
        const val td1Dur = 168
        const val doRefreshTime: Long = 86400000// A day
        val ssl = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) "s" else ""
        val cloudFol = "http${ssl}://migratio.mahdiparastesh.ir/xml/"
        var dm = DisplayMetrics()
        var censorBreak = 0
        var cm: ConnectivityManager? = null
        var cmCallbackSet = false
        var connected = false
        var dirLtr = true

        val cmCallback = @RequiresApi(Build.VERSION_CODES.M)
        object : ConnectivityManager.NetworkCallback() {
            private val activeNetworks: MutableList<Network> = mutableListOf()

            @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                if (activeNetworks.none { activeNetwork -> activeNetwork.networkHandle == network.networkHandle })
                    activeNetworks.add(network)
                connected = activeNetworks.isNotEmpty()
            }

            @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
            override fun onLost(network: Network) {
                super.onLost(network)
                activeNetworks.removeAll { activeNetwork -> activeNetwork.networkHandle == network.networkHandle }
                connected = activeNetworks.isNotEmpty()
            }
        }


        fun init(that: AppCompatActivity, body: ViewGroup) {
            c = that.applicationContext// if (!::c.isInitialized)
            if (!::sp.isInitialized) sp =
                that.getSharedPreferences("${that.packageName}_preferences", Context.MODE_PRIVATE)
            dm = that.resources.displayMetrics
            dirLtr = dir(c, body)
            current = that

            if (!::logoFont.isInitialized) logoFont = fonts(c, Fonts.LOGO)
            if (!::titleFont.isInitialized) titleFont = fonts(c, Fonts.TITLE)
            if (!::textFont.isInitialized) textFont = fonts(c, Fonts.TEXT)

            cm = that.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (!cmCallbackSet) cm?.let {
                    it.registerDefaultNetworkCallback(cmCallback)
                    cmCallbackSet = true
                }
            } else connected = isOnlineOld()
        }

        @Suppress("Deprecation")
        fun isOnlineOld(): Boolean {
            var nwi: NetworkInfo? = null
            if (cm != null) nwi = cm!!.activeNetworkInfo
            return nwi != null && nwi.isConnected
        }

        fun dp(px: Int = 0) = (dm.density * px.toFloat()).toInt()

        fun vis(v: View, b: Boolean = true) {
            v.visibility = if (b) View.VISIBLE else View.GONE
        }

        fun vish(v: View, b: Boolean = true) {
            v.visibility = if (b) View.VISIBLE else View.INVISIBLE
        }

        fun switcher(
            c: Context, vs: ViewSwitcher, dirLtr: Boolean, animate: Boolean = true,
            exSwitched: String = Select.exSwitchedTo2nd
        ): Boolean {
            val ir = if (dirLtr) R.anim.slide_in_right else R.anim.slide_in_left
            val il = if (dirLtr) R.anim.slide_in_left else R.anim.slide_in_right
            val ol = if (dirLtr) R.anim.slide_out_left else R.anim.slide_out_right
            val or = if (dirLtr) R.anim.slide_out_right else R.anim.slide_out_left
            vs.inAnimation = if (animate)
                AnimationUtils.loadAnimation(c, if (vs.displayedChild == 0) ir else il)
            else null
            vs.outAnimation = if (animate)
                AnimationUtils.loadAnimation(c, if (vs.displayedChild == 0) ol else or)
            else null
            vs.displayedChild = if (vs.displayedChild == 0) 1 else 0
            sp.edit().putBoolean(exSwitched, vs.displayedChild == 1).apply()
            return vs.displayedChild == 1
        }

        fun load1(iv: ImageView, placeHolder: View? = null, dur: Long = 444): ObjectAnimator =
            ObjectAnimator.ofFloat(iv, "rotation", 0f, 360f).apply {
                duration = dur
                repeatCount = ObjectAnimator.INFINITE
                interpolator = LinearInterpolator()
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator?) {
                        placeHolder?.let { vis(it, false) }
                        vis(iv)
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                        vis(iv, false)
                        placeHolder?.let { vis(it) }
                    }
                })
                start()
            }

        fun bolden(v: View, scale: Float, dur: Long = 870): AnimatorSet = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(v, "scaleX", 1f, scale),
                ObjectAnimator.ofFloat(v, "scaleY", 1f, scale)
            )
            childAnimations.forEach {
                (it as ObjectAnimator).apply {
                    duration = dur
                    repeatCount = ObjectAnimator.INFINITE
                    repeatMode = ValueAnimator.REVERSE//RESTART
                }
            }
            start()
        }

        fun countryNames(): Array<String> = when (Locale.getDefault().language) {
            "fa" -> Countries.FA
            else -> Countries.EN
        }

        fun fonts(c: Context, which: Fonts): Typeface =
            Typeface.createFromAsset(c.assets, c.resources.getString(which.id))

        fun now(): Long = Calendar.getInstance().timeInMillis

        fun alertDialogue1(
            that: AppCompatActivity, title: Int, message: Int, font: Typeface,
            onOk: DialogInterface.OnClickListener? = null,
            onCancel: DialogInterface.OnCancelListener? = null,
            censorBreaker: Boolean = false
        ): Boolean {
            AlertDialog.Builder(that, R.style.alertDialogue1).apply {
                setTitle(title)
                setMessage(message)
                setIcon(R.mipmap.launcher_round)
                setPositiveButton(R.string.ok, onOk)
                setOnCancelListener(onCancel)
            }.create().apply {
                show()
                fixADButton(that, getButton(AlertDialog.BUTTON_POSITIVE), font)
                fixADTitle(that, window, font)
                var tvMsg = fixADMsg(that, window, font)

                // Censor Breaker
                if (censorBreaker) tvMsg?.setOnClickListener {
                    censorBreak += 1
                    if (censorBreak >= 10) {
                        Select.handler?.obtainMessage(Works.BREAK_CENSOR.ordinal, null)
                            ?.sendToTarget()
                        cancel()
                    }
                    val time: Long = 5000
                    object : CountDownTimer(time, time) {
                        override fun onTick(p0: Long) {}
                        override fun onFinish() {
                            censorBreak = 0
                        }
                    }.start()
                }
            }
            return true
        }

        fun alertDialogue2(
            that: AppCompatActivity, title: Int, message: Int,
            onYes: DialogInterface.OnClickListener? = null,
            onNo: DialogInterface.OnClickListener? = null,
            onCancel: DialogInterface.OnCancelListener? = null,
            font: Typeface = textFont
        ): Boolean {
            AlertDialog.Builder(that, R.style.alertDialogue1).apply {
                setTitle(title)
                setMessage(message)
                setIcon(R.mipmap.launcher_round)
                setPositiveButton(R.string.yes, onYes)
                setNegativeButton(R.string.no, onNo)
                setOnCancelListener(onCancel)
            }.create().apply {
                show()
                fixADButton(that, getButton(AlertDialog.BUTTON_POSITIVE), font, dirLtr)
                fixADButton(that, getButton(AlertDialog.BUTTON_NEGATIVE), font, !dirLtr)
                fixADTitle(that, window, font)
                fixADMsg(that, window, font)
            }
            return true
        }

        fun alertDialogue3(
            that: AppCompatActivity, title: Int, message: String,
            copyable: Boolean, linkify: Boolean = false
        ): Boolean {
            AlertDialog.Builder(that, R.style.alertDialogue1).apply {
                setTitle(title)
                setMessage(message)
                setIcon(R.mipmap.launcher_round)
                setPositiveButton(R.string.ok, null)
            }.create().apply {
                show()
                var font = textFont
                fixADButton(that, getButton(AlertDialog.BUTTON_POSITIVE), font)
                fixADTitle(that, window, font)
                var tvMsg = fixADMsg(that, window, font, linkify)
                if (copyable) tvMsg?.setOnLongClickListener {
                    copyItsText(c, tvMsg)
                    true
                }
            }
            return true
        }

        fun fixADButton(
            that: AppCompatActivity, button: Button, font: Typeface, sMargin: Boolean = false
        ) {
            button.apply {
                setTextColor(ContextCompat.getColor(that, R.color.CA))
                setBackgroundColor(ContextCompat.getColor(that, R.color.CP))
                typeface = font
                textSize = that.resources.getDimension(R.dimen.alert1Button) / dm.density
                if (sMargin) (layoutParams as ViewGroup.MarginLayoutParams).apply {
                    marginStart = textSize.toInt()
                }
            }
        }

        fun fixADTitle(that: AppCompatActivity, window: Window?, font: Typeface): TextView? {
            var tvTitle = window?.findViewById<TextView>(R.id.alertTitle)
            tvTitle?.setTypeface(font, Typeface.BOLD)
            tvTitle?.textSize = that.resources.getDimension(R.dimen.alert1Title) / dm.density
            return tvTitle
        }

        fun fixADMsg(
            that: AppCompatActivity, window: Window?, font: Typeface, linkify: Boolean = false
        ): TextView? {
            var tvMsg = window?.findViewById<TextView>(android.R.id.message)
            tvMsg?.typeface = font
            tvMsg?.setLineSpacing(
                that.resources.getDimension(R.dimen.alert1MsgLine) / dm.density, 0f
            )
            tvMsg?.textSize = that.resources.getDimension(R.dimen.alert1Msg) / dm.density
            tvMsg?.setPadding(dp(28), dp(15), dp(28), dp(15))
            if (tvMsg != null && linkify) Linkify.addLinks(tvMsg, Linkify.ALL)
            return tvMsg
        }

        fun copyText(c: Context, s: String) {
            (c.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?)?.setPrimaryClip(
                ClipData.newPlainText("simple text", s)
            )
        }

        fun copyItsText(c: Context, tv: TextView, lang: Int = 0) {
            copyText(c, tv.text.toString())
            Toast.makeText(c, R.string.copied, Toast.LENGTH_SHORT).show()
        }

        fun dir(c: Context, body: ViewGroup): Boolean {
            val dirLtr = c.resources.getBoolean(R.bool.dirLtr)
            if (!dirLtr) body.layoutDirection = View.LAYOUT_DIRECTION_RTL
            return dirLtr
        }

        fun handleTB(that: AppCompatActivity, toolbar: Toolbar, titleFont: Typeface) {
            that.setSupportActionBar(toolbar)
            var tbTitle: TextView? = null
            for (g in 0 until toolbar.childCount) toolbar.getChildAt(g).apply {
                if (this is TextView &&
                    this.text.toString() == that.resources.getString(R.string.app_name)
                ) tbTitle = this
            }
            tbTitle?.apply {
                setTypeface(titleFont, Typeface.BOLD)
                textSize = that.resources.getDimension(R.dimen.tbTitle) / dm.density
            }
        }

        fun onLoad(view: View, func: Function): CountDownTimer =
            object : CountDownTimer(10000, 50) {
                override fun onFinish() {}
                override fun onTick(millisUntilFinished: Long) {
                    if (view.width <= 0) return
                    func.execute()
                    this.cancel()
                }
            }.start()

        fun defaultMyCriteria(gotCriteria: List<Criterion>, handler: Handler?) {
            sp.edit().putBoolean(Panel.exRepair, false).apply()
            val gotMyCriteria = ArrayList<MyCriterion>()
            for (i in gotCriteria) {
                var good = i.good
                if (good == "") good = i.medi
                gotMyCriteria.add(MyCriterion(0, i.tag, false, good, 100))
            }
            Work(
                c, handler, Works.CLEAR_AND_INSERT_ALL, Types.MY_CRITERION,
                listOf(gotMyCriteria, Types.MY_CRITERION.ordinal, Works.NONE.ordinal)
                // GIVING THE DESTINATION "Types.MY_CRITERION.ordinal" IS ESSENTIAL;
                // BECAUSE IF YOU DO OTHERWISE, THE RESULT WILL BE SENT TO DEST: 0.
            ).start()
        }

        fun repairMyCriteria(
            cris: List<Criterion>,
            oldMyCris: List<MyCriterion>,
            handler: Handler?
        ) {
            val newMyCris = ArrayList<MyCriterion>()
            for (i in cris) {
                var findIn = Computation.findMyCriByTag(i.tag, oldMyCris)
                if (findIn != null) newMyCris.add(
                    MyCriterion(0, i.tag, findIn.isOn, findIn.good, findIn.importance)
                ) else {
                    var good = i.good
                    if (good == "") good = i.medi
                    newMyCris.add(MyCriterion(0, i.tag, false, good, 100))
                }
            }
            Work(
                c, handler, Works.CLEAR_AND_INSERT_ALL, Types.MY_CRITERION,
                listOf(newMyCris, Types.MY_CRITERION.ordinal, Works.REPAIR.ordinal)
            ).start()
        }

        fun jsonReader(json: String): JsonReader = JsonReader(
            InputStreamReader(
                ByteArrayInputStream(json.toByteArray(Charset.forName("UTF-8"))), "UTF-8"
            )
        )

        fun findChildrenByClass(viewGroup: ViewGroup, clazz: Class<*>) =
            gatherChildrenByClass(viewGroup, clazz, ArrayList())

        fun gatherChildrenByClass(
            viewGroup: ViewGroup, clazz: Class<*>, childrenFound: ArrayList<View>
        ): ArrayList<View> {
            for (i in 0 until viewGroup.childCount) {
                val child = viewGroup.getChildAt(i)
                if (clazz.isAssignableFrom(child::class.java)) childrenFound.add(child)
                if (child is ViewGroup)
                    gatherChildrenByClass(child, clazz, childrenFound)
            }
            return childrenFound
        }

        fun z(n: Int): String {
            var s = n.toString()
            return if (s.length == 1) "0$s" else s
        }
    }


    interface Function {
        fun execute()
    }
}
