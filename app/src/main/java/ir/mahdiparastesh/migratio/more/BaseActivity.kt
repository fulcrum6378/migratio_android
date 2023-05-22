package ir.mahdiparastesh.migratio.more

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import ir.mahdiparastesh.migratio.Fun
import ir.mahdiparastesh.migratio.Model
import ir.mahdiparastesh.migratio.Panel
import ir.mahdiparastesh.migratio.R
import ir.mahdiparastesh.migratio.data.Criterion
import ir.mahdiparastesh.migratio.data.MyCriterion
import ir.mahdiparastesh.migratio.data.Types
import ir.mahdiparastesh.migratio.data.Work
import ir.mahdiparastesh.migratio.data.Works

abstract class BaseActivity : AppCompatActivity() {
    val c: Context get() = applicationContext
    lateinit var m: Model
    val sp: SharedPreferences by lazy { getSharedPreferences("preferences", Context.MODE_PRIVATE) }
    val logoFont: Typeface by lazy { Fun.fonts(c, Fonts.LOGO) }
    val titleFont: Typeface by lazy { Fun.fonts(c, Fonts.TITLE) }
    val textFont: Typeface by lazy { Fun.fonts(c, Fonts.TEXT) }
    lateinit var toolbar: Toolbar
    val dm: DisplayMetrics by lazy { resources.displayMetrics }
    val dirLtr by lazy { c.resources.getBoolean(R.bool.dirLtr) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        m = ViewModelProvider(this, Model.Factory())["Model", Model::class.java]

        Fun.cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (!Fun.cmCallbackSet) Fun.cm?.let {
                it.registerDefaultNetworkCallback(Fun.cmCallback)
                Fun.cmCallbackSet = true
            }
        } else Fun.connected = Fun.isOnlineOld()
    }

    override fun setContentView(root: View?) {
        super.setContentView(root)
        root?.layoutDirection =
            if (!dirLtr) ViewGroup.LAYOUT_DIRECTION_RTL else ViewGroup.LAYOUT_DIRECTION_LTR
        toolbar = findViewById(R.id.toolbar)
        handleTB()
    }

    fun handleTB() {
        setSupportActionBar(toolbar)
        var tbTitle: TextView? = null
        for (g in 0 until toolbar.childCount) toolbar.getChildAt(g).apply {
            if (this is TextView &&
                this.text.toString() == c.resources.getString(R.string.app_name)
            ) tbTitle = this
        }
        tbTitle?.apply {
            setTypeface(titleFont, Typeface.BOLD)
            textSize = c.resources.getDimension(R.dimen.tbTitle) / dm.density
        }
    }

    fun dp(px: Int = 0) = (dm.density * px.toFloat()).toInt()

    fun color(@ColorRes res: Int) = ContextCompat.getColor(this, res)

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
}
