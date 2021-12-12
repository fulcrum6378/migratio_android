package ir.mahdiparastesh.migratio.data

import android.content.Context
import android.os.Handler
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import ir.mahdiparastesh.migratio.Fun
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.URLDecoder
import java.net.URLEncoder

class Parse(val c: Context, val handler: Handler, val type: Types) : Thread() {
    companion object {
        val xmlCountries = Fun.cloudFol + "countries.xml"
        val xmlCriteria = Fun.cloudFol + "criteria.xml"
    }

    override fun run() {
        var address = ""
        var timeout = DefaultRetryPolicy.DEFAULT_TIMEOUT_MS
        var iType: Int
        when (type) {
            Types.COUNTRY -> {
                address = xmlCountries
                timeout = 25000
                iType = Types.COUNTRY.ordinal
            }
            Types.CRITERION -> {
                address = xmlCriteria
                timeout = 15000
                iType = Types.CRITERION.ordinal
            }
            else -> iType = -1
        }
        if (iType == -1) return

        Volley.newRequestQueue(c).add(
            StringRequest(Request.Method.GET, address, { oldRes ->
                val res = URLDecoder.decode(
                    URLEncoder.encode(oldRes, Charsets.ISO_8859_1.name()), Charsets.UTF_8.name()
                )
                handler.obtainMessage(
                    Works.DOWNLOAD.ordinal, iType, 0,
                    if (res.substring(0, 5) == "<?xml") when (type) {
                        Types.COUNTRY -> countries(res)
                        Types.CRITERION -> criteria(res)
                        else -> null
                    } else null
                ).sendToTarget()
            }, { //if (it is TimeoutError)
                handler.obtainMessage(Works.DOWNLOAD.ordinal, null).sendToTarget()
            }).setTag("download$type").setRetryPolicy(
                DefaultRetryPolicy(timeout, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
            )
        )
    }

    private val tagCountries = "cs"
    private val tagCountry = "cn"
    private val tagCountryName = "nm"
    private val tagCountryContinent = "ct"
    private val tagCountryItem = "it"
    private val tagCountryItemName = "nm"
    private val tagCountryItemValue = "vl"
    private val tagCountryItemException = "ex"

    @Throws(XmlPullParserException::class, IOException::class)
    private fun countries(res: String): List<Country> {
        val input = ByteArrayInputStream(res.toByteArray(Charsets.UTF_8))
        var countries: MutableList<Country>
        XmlPullParserFactory.newInstance().newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(input, Charsets.UTF_8.name())
            nextTag()

            require(XmlPullParser.START_TAG, null, tagCountries)
            countries = mutableListOf()
            while (next() != XmlPullParser.END_TAG) {
                if (eventType != XmlPullParser.START_TAG) continue
                if (name == tagCountry) {
                    require(XmlPullParser.START_TAG, null, tagCountry)
                    var cTag = getAttributeValue(null, tagCountryName)
                    var cId: Long? = null
                    try {
                        cId = ir.mahdiparastesh.migratio.Countries.TAGS.indexOf(cTag).toLong()
                    } catch (ignored: Exception) {
                    }
                    var cCont: Int? = null
                    try {
                        cCont = getAttributeValue(null, tagCountryContinent).toInt()
                    } catch (ignored: Exception) {
                    }
                    var cAttrs: HashMap<String, String>? = null
                    var cExcept: HashMap<String, String>? = null
                    while (next() != XmlPullParser.END_TAG) {
                        if (eventType != XmlPullParser.START_TAG) continue
                        if (cAttrs == null) cAttrs = HashMap()
                        if (cExcept == null) cExcept = HashMap()
                        if (name == tagCountryItem) {
                            var arr = readIt(this)
                            cAttrs[arr[0]!!] = arr[1]!!
                            if (arr[2] != null) cExcept[arr[0]!!] = arr[2]!!
                        } else skip(this)
                    }
                    if (cId != null && cCont != null && cAttrs != null && cExcept != null)
                        countries.add(Country(cId, cTag, cCont, cAttrs, cExcept))
                } else skip(this)
            }
        }
        return countries
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readIt(p: XmlPullParser): Array<String?> {
        p.require(XmlPullParser.START_TAG, null, tagCountryItem)
        var name = p.getAttributeValue(null, tagCountryItemName)
        var value = p.getAttributeValue(null, tagCountryItemValue)
        var except = p.getAttributeValue(null, tagCountryItemException) ?: null
        p.nextTag()
        p.require(XmlPullParser.END_TAG, null, tagCountryItem)
        return arrayOf(name, value, except)
    }

    private val tagCriteria = "ca"
    private val tagCriterion = "cr"
    private val tagCriterionName = "id"
    private val tagCriterionFullName = "nm"
    private val tagCriterionType = "ty"
    private val tagCriterionGood = "gd"
    private val tagCriterionMedium = "md"
    private val tagCriterionCensor = "ce"
    private val tagCriterionReference = "rf"

    @Throws(XmlPullParserException::class, IOException::class)
    private fun criteria(res: String): List<Criterion>? {
        val input = ByteArrayInputStream(res.toByteArray(Charsets.UTF_8))
        var criteria: MutableList<Criterion>?
        XmlPullParserFactory.newInstance().newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(input, Charsets.UTF_8.name())
            nextTag()

            require(XmlPullParser.START_TAG, null, tagCriteria)
            criteria = mutableListOf()
            while (next() != XmlPullParser.END_TAG) {
                if (eventType != XmlPullParser.START_TAG) continue
                if (name == tagCriterion) {
                    require(XmlPullParser.START_TAG, null, tagCriterion)
                    val censor = getAttributeValue(null, tagCriterionCensor)?.toInt() ?: 0
                    criteria!!.add(
                        Criterion(
                            criteria!!.size.toLong(),
                            getAttributeValue(null, tagCriterionName),
                            getAttributeValue(null, tagCriterionFullName),
                            getAttributeValue(null, tagCriterionType),
                            getAttributeValue(null, tagCriterionGood),
                            getAttributeValue(null, tagCriterionMedium),
                            censor, getAttributeValue(null, tagCriterionReference)
                        )
                    )
                    nextTag()
                    require(XmlPullParser.END_TAG, null, tagCriterion)
                } else skip(this)
            }
        }
        return criteria
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(p: XmlPullParser) {
        if (p.eventType != XmlPullParser.START_TAG) throw IllegalStateException()
        var depth = 1
        while (depth != 0) when (p.next()) {
            XmlPullParser.END_TAG -> depth--
            XmlPullParser.START_TAG -> depth++
        }
    }
}
