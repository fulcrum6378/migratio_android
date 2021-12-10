package ir.mahdiparastesh.migratio

import ir.mahdiparastesh.migratio.data.Country
import ir.mahdiparastesh.migratio.data.Criterion
import ir.mahdiparastesh.migratio.data.MyCriterion
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

class Computation(val id: Long, val score: Double) {
    companion object {
        fun compute(
            cons: List<Country>, cris: List<Criterion>,
            allMyCons: List<String>, allMyCris: List<MyCriterion>
        ): List<Computation> {

            // Sort My Countries
            var myCons: MutableList<String> = allMyCons as MutableList<String>
            myCons.sort()

            // Filter which is On
            val myCris = ArrayList<MyCriterion>()
            for (amc in allMyCris) if (amc.isOn) myCris.add(amc)

            // Minima and Maxima
            val minAndMax = ArrayList<DoubleArray>()
            for (mycri in myCris) {
                val allVals = ArrayList<Double>()
                val crit = findCriByTag(mycri.tag, cris)!!
                for (con in cons) compileValue(con, crit, cons).apply {
                    allVals.add(
                        if (mycri.good == "+" || mycri.good == "-") this
                        else abs(this - safeGood(mycri, cris))
                    )
                }//for (mycon in myCons) findConByTag(mycon, cons)!!
                minAndMax.add(
                    doubleArrayOf(Collections.min(allVals), Collections.max(allVals)).apply {
                        if (mycri.good != "+") reverse()
                    }
                )
            }

            // Get mean scores in my criteria for each country
            val meanScores = ArrayList<Double>()
            for (mycon in myCons) {
                val myCon = findConByTag(mycon, cons)!!

                // Scores Without Importance Adjustment
                val scoresNoImport = ArrayList<Double>()
                for (mycri in myCris.indices) {
                    val notReadyVal = compileValue(
                        myCon, findCriByTag(myCris[mycri].tag, cris)!!, cons
                    )
                    val readyVal =
                        if (myCris[mycri].good != "-" && myCris[mycri].good != "+")
                            abs(notReadyVal - safeGood(myCris[mycri], cris))
                        else notReadyVal
                    scoresNoImport.add(
                        relativeScore(
                            minAndMax[mycri][0],
                            minAndMax[mycri][1],
                            readyVal
                        )
                    )
                }

                // Scores With Importance Adjustment
                val scores = ArrayList<Double>()
                var weightedMean = 0.0
                for (sni in scoresNoImport.indices) {
                    var weight = myCris[sni].importance.toDouble() / 100.0
                    weightedMean += weight
                    scores.add(scoresNoImport[sni] * weight)
                }

                // Mean Score of All Criteria
                var mean = 0.0
                for (s in scores) mean += s
                mean /= weightedMean

                meanScores.add(mean)
            }

            // Make relative scores out of mean scores
            val list = ArrayList<Computation>()
            for (mycon in myCons.indices) list.add(
                Computation(
                    findConByTag(myCons[mycon], cons)!!.id,
                    relativeScore(
                        Collections.min(meanScores),
                        Collections.max(meanScores),
                        meanScores[mycon]
                    )
                )
            )
            Collections.sort(list, SortComputs())
            return list.toList()
        }

        fun relativeScore(min: Double, max: Double, value: Double) =
            if (min != max) {
                val cA = abs(max - min)
                val cB = abs(value - min)
                val cC = 100.0 / cA
                val cD = cC * cB
                cD
            } else 100.0

        fun safeGood(mycri: MyCriterion, cris: List<Criterion>) =
            if (mycri.good != "") mycri.good.toDouble()
            else findCriByTag(mycri.tag, cris)!!.medi.toDouble()

        fun findConById(id: Long, list: List<Country>): Country? {
            var con: Country? = null
            for (i in list) if (i.id == id) con = i
            return con
        }

        fun findConByTag(tag: String, list: List<Country>): Country? {
            var con: Country? = null
            for (i in list) if (i.tag == tag) con = i
            return con
        }

        fun findCriByTag(tag: String, list: List<Criterion>): Criterion? {
            var cri: Criterion? = null
            for (i in list) if (i.tag == tag) cri = i
            return cri
        }

        fun findMyCriByTag(tag: String, list: List<MyCriterion>): MyCriterion? {
            var myCri: MyCriterion? = null
            for (i in list) if (i.tag == tag) myCri = i
            return myCri
        }

        fun compileValue(myCon: Country, myCri: Criterion, cons: List<Country>): Double {
            var value = myCon.attrs[myCri.tag]!!
            when {
                value == "-" -> {
                    val split = myCon.except[myCri.tag]!!.split("+")
                    val estimation = ArrayList<Double>()
                    for (ss in split)
                        estimation.add(compileValue(findConByTag(ss, cons)!!, myCri, cons))
                    var estimate = estimation[0]
                    if (estimation.size > 1) {
                        for (es in estimation) estimate += es
                        try {
                            estimate /= estimation.size
                        } catch (ignored: Exception) {
                        }
                    }
                    return estimate
                }
                value.substring(0, 1) == "~" -> return value.substring(1).toDouble()
                else -> return value.toDouble()
            }
        }


        class SortComputs : Comparator<Computation> {
            override fun compare(a: Computation, b: Computation) = b.score.compareTo(a.score)
        }
    }
}
