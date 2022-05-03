@file:Suppress("DEPRECATION")

package ir.mahdiparastesh.migratio

import android.Manifest
import android.animation.*
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkInfo
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.text.util.Linkify
import android.util.JsonReader
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import ir.mahdiparastesh.migratio.data.*
import ir.mahdiparastesh.migratio.more.BaseActivity
import ir.mahdiparastesh.migratio.more.Fonts
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.*

class Fun {
    companion object {
        const val defDataDB = "data"
        const val td1Dur = 168
        const val doRefreshTime: Long = 86400000// A day
        val ssl = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) "s" else ""
        val cloudFol = "http${ssl}://migratio.mahdiparastesh.ir/xml/"
        var censorBreak = 0
        var cm: ConnectivityManager? = null
        var cmCallbackSet = false
        var connected = false

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


        fun isOnlineOld(): Boolean {
            var nwi: NetworkInfo? = null
            if (cm != null) nwi = cm!!.activeNetworkInfo
            return nwi != null && nwi.isConnected
        }

        fun View.vis(b: Boolean = true) {
            visibility = if (b) View.VISIBLE else View.GONE
        }

        fun View.vish(b: Boolean = true) {
            visibility = if (b) View.VISIBLE else View.INVISIBLE
        }

        fun switcher(
            c: BaseActivity, vs: ViewSwitcher, dirLtr: Boolean, animate: Boolean = true,
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
            c.sp.edit().putBoolean(exSwitched, vs.displayedChild == 1).apply()
            return vs.displayedChild == 1
        }

        fun load1(iv: ImageView, placeHolder: View? = null, dur: Long = 444): ObjectAnimator =
            ObjectAnimator.ofFloat(iv, "rotation", 0f, 360f).apply {
                duration = dur
                repeatCount = ObjectAnimator.INFINITE
                interpolator = LinearInterpolator()
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator?) {
                        placeHolder?.vis(false)
                        iv.vis()
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                        iv.vis(false)
                        placeHolder?.vis()
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
            c: BaseActivity, title: Int, message: Int, font: Typeface,
            onOk: DialogInterface.OnClickListener? = null,
            onCancel: DialogInterface.OnCancelListener? = null,
            censorBreaker: Boolean = false
        ): Boolean {
            AlertDialog.Builder(c, R.style.AlertDialogue).apply {
                setTitle(title)
                setMessage(message)
                setIcon(R.mipmap.launcher_round)
                setPositiveButton(R.string.ok, onOk)
                setOnCancelListener(onCancel)
            }.create().apply {
                show()
                fixADButton(c, getButton(AlertDialog.BUTTON_POSITIVE), font)
                fixADTitle(c, font)
                var tvMsg = fixADMsg(c, font)

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
            c: BaseActivity, title: Int, message: Int,
            onYes: DialogInterface.OnClickListener? = null,
            onNo: DialogInterface.OnClickListener? = null,
            onCancel: DialogInterface.OnCancelListener? = null,
            font: Typeface = c.textFont
        ): Boolean {
            AlertDialog.Builder(c, R.style.AlertDialogue).apply {
                setTitle(title)
                setMessage(message)
                setIcon(R.mipmap.launcher_round)
                setPositiveButton(R.string.yes, onYes)
                setNegativeButton(R.string.no, onNo)
                setOnCancelListener(onCancel)
            }.create().apply {
                show()
                fixADButton(c, getButton(AlertDialog.BUTTON_POSITIVE), font, c.dirLtr)
                fixADButton(c, getButton(AlertDialog.BUTTON_NEGATIVE), font, !c.dirLtr)
                fixADTitle(c, font)
                fixADMsg(c, font)
            }
            return true
        }

        fun alertDialogue3(
            c: BaseActivity, title: Int, message: String,
            copyable: Boolean, linkify: Boolean = false
        ): Boolean {
            AlertDialog.Builder(c, R.style.AlertDialogue).apply {
                setTitle(title)
                setMessage(message)
                setIcon(R.mipmap.launcher_round)
                setPositiveButton(R.string.ok, null)
            }.create().apply {
                show()
                var font = c.textFont
                fixADButton(c, getButton(AlertDialog.BUTTON_POSITIVE), font)
                fixADTitle(c, font)
                var tvMsg = fixADMsg(c, font, linkify)
                if (copyable) tvMsg?.setOnLongClickListener {
                    copyItsText(c, tvMsg)
                    true
                }
            }
            return true
        }

        fun fixADButton(c: BaseActivity, button: Button, font: Typeface, sMargin: Boolean = false) {
            button.apply {
                setTextColor(ContextCompat.getColor(c, R.color.CA))
                setBackgroundColor(ContextCompat.getColor(c, R.color.CP))
                typeface = font
                textSize = c.resources.getDimension(R.dimen.alert1Button) / c.dm.density
                if (sMargin) (layoutParams as ViewGroup.MarginLayoutParams).apply {
                    marginStart = textSize.toInt()
                }
            }
        }

        fun fixADTitle(c: BaseActivity, font: Typeface): TextView? {
            var tvTitle = c.window?.findViewById<TextView>(R.id.alertTitle)
            tvTitle?.setTypeface(font, Typeface.BOLD)
            tvTitle?.textSize = c.resources.getDimension(R.dimen.alert1Title) / c.dm.density
            return tvTitle
        }

        fun fixADMsg(c: BaseActivity, font: Typeface, linkify: Boolean = false): TextView? {
            var tvMsg = c.window?.findViewById<TextView>(android.R.id.message)
            tvMsg?.typeface = font
            tvMsg?.setLineSpacing(
                c.resources.getDimension(R.dimen.alert1MsgLine) / c.dm.density, 0f
            )
            tvMsg?.textSize = c.resources.getDimension(R.dimen.alert1Msg) / c.dm.density
            tvMsg?.setPadding(c.dp(28), c.dp(15), c.dp(28), c.dp(15))
            if (tvMsg != null && linkify) Linkify.addLinks(tvMsg, Linkify.ALL)
            return tvMsg
        }

        fun copyText(c: Context, s: String) {
            (c.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?)?.setPrimaryClip(
                ClipData.newPlainText("simple text", s)
            )
        }

        fun copyItsText(c: Context, tv: TextView) {
            copyText(c, tv.text.toString())
            Toast.makeText(c, R.string.copied, Toast.LENGTH_SHORT).show()
        }

        /*fun onLoad(view: View, func: Function): CountDownTimer =
            object : CountDownTimer(10000, 50) {
                override fun onFinish() {}
                override fun onTick(millisUntilFinished: Long) {
                    if (view.width <= 0) return
                    func.execute()
                    this.cancel()
                }
            }.start()*/

        fun repairMyCriteria(
            c: BaseActivity, cris: List<Criterion>, oldMyCris: List<MyCriterion>, handler: Handler?
        ) {
            val newMyCris = ArrayList<MyCriterion>()
            for (i in cris) {
                var findIn = oldMyCris.find { it.tag == i.tag }
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

        fun z(n: Int): String {
            var s = n.toString()
            return if (s.length == 1) "0$s" else s
        }
    }
}
