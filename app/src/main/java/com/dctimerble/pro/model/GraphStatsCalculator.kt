package com.dctimerble.pro.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Pure graph aggregation used by the statistics screen. No Android View dependencies. */
object GraphStatsCalculator {
    const val DAY = 0
    const val WEEK = 1
    const val MONTH = 2
    const val YEAR = 3
    const val RANGE = 4

    data class Summary(
        val bins: IntArray,
        val solves: Int,
        val average: Int?,
        val best: Int?,
        val worst: Int?,
        val start: Calendar,
        val end: Calendar
    )

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply { isLenient = false }
    init {
        dateFormat.isLenient = false
    }

    fun aggregate(result: Result, startInput: Calendar, endInput: Calendar, rangeType: Int): Summary {
        val start = day(startInput)
        val end = day(endInput).let { candidate ->
            if (candidate.before(start)) (start.clone() as Calendar) else candidate
        }
        val exclusiveEnd = (end.clone() as Calendar).apply { add(Calendar.DATE, 1) }
        val dateList = result.getDates()
        val bins = binCount(start, end, rangeType)
        val values = IntArray(bins)
        var solves = 0
        var sum = 0L
        var best = Int.MAX_VALUE
        var worst = 0

        val startText = dateFormat.format(start.time)
        val endText = dateFormat.format(exclusiveEnd.time)
        for (i in dateList.indices) {
            val raw = dateList[i]
            val dateOnly = raw?.take(10).orEmpty()
            if (dateOnly.isEmpty() || dateOnly < startText || dateOnly >= endText) continue
            val time = try { dateToCalendar(raw) } catch (_: Exception) { continue }
            val index = indexOf(start, time, rangeType)
            if (index in values.indices) values[index]++
            if (!result.isDnf(i)) {
                val solveTime = result.getTime(i)
                solves++
                sum += solveTime.toLong()
                if (solveTime < best) best = solveTime
                if (solveTime > worst) worst = solveTime
            }
        }
        return Summary(values, solves, if (solves == 0) null else ((sum + solves / 2) / solves).toInt(), if (solves == 0) null else best, if (solves == 0) null else worst, start, end)
    }

    private fun binCount(start: Calendar, end: Calendar, type: Int): Int = when (type) {
        DAY -> 24
        WEEK -> 7
        MONTH -> daysBetween(start, end) + 1
        YEAR -> 12
        else -> daysBetween(start, end) + 1
    }

    private fun indexOf(start: Calendar, date: Calendar, type: Int): Int = when (type) {
        DAY -> date.get(Calendar.HOUR_OF_DAY)
        WEEK, MONTH, RANGE -> daysBetween(start, date)
        YEAR -> (date.get(Calendar.YEAR) - start.get(Calendar.YEAR)) * 12 + date.get(Calendar.MONTH) - start.get(Calendar.MONTH)
        else -> 0
    }

    private fun dateToCalendar(value: String): Calendar = Calendar.getInstance().apply {
        val parser = if (value.length >= 19) timestampFormat else dateFormat
        time = parser.parse(value) ?: throw IllegalArgumentException(value)
    }

    private fun day(source: Calendar): Calendar = (source.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun daysBetween(start: Calendar, end: Calendar): Int = ((end.timeInMillis - start.timeInMillis) / 86_400_000L).toInt()
}
