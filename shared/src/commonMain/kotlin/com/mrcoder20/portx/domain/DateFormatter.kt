@file:Suppress("DEPRECATION")
package com.mrcoder20.portx.domain

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object DateFormatter {

    /**
     * Converts a Gregorian date (year, month, day) to Solar Hijri (Jalali) date.
     * Algorithm based on JDF / Birashk solar calendar arithmetic.
     */
    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
        val gDays = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDays = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        val gy2 = gy - 1600
        val gm2 = gm - 1
        val gd2 = gd - 1

        var gDayNo = 365 * gy2 + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400
        for (i in 0 until gm2) {
            gDayNo += gDays[i]
        }
        if (gm2 > 1 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) {
            gDayNo++ // leap year
        }
        gDayNo += gd2

        var jDayNo = gDayNo - 79
        val jNp = jDayNo / 12053
        jDayNo %= 12053

        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }

        var jm = 0
        for (i in 0 until 11) {
            if (jDayNo < jDays[i]) {
                jm = i
                break
            }
            jDayNo -= jDays[i]
            jm = i + 1
        }
        val jd = jDayNo + 1
        return Triple(jy, jm + 1, jd)
    }

    /**
     * Calculates the day of the week for any Gregorian date.
     * Returns 0 for Sunday, 1 for Monday, ..., 6 for Saturday.
     */
    fun dayOfWeek(gy: Int, gm: Int, gd: Int): Int {
        val t = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
        var y = gy
        if (gm < 3) y -= 1
        val dow = (y + y / 4 - y / 100 + y / 400 + t[gm - 1] + gd) % 7
        return if (dow < 0) (dow + 7) % 7 else dow
    }

    /**
     * Converts standard ASCII numeric digits (0-9) to Persian digits (۰-۹).
     */
    fun toPersianDigits(input: String): String {
        val faDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        return input.map { ch ->
            if (ch in '0'..'9') faDigits[ch - '0'] else ch
        }.joinToString("")
    }

    /**
     * Converts Persian (۰-۹) and Arabic (٠-٩) digits to standard ASCII numeric digits (0-9).
     */
    fun toAsciiDigits(input: String): String {
        return input.map { ch ->
            when (ch) {
                in '۰'..'۹' -> '0' + (ch - '۰')
                in '٠'..'٩' -> '0' + (ch - '٠')
                else -> ch
            }
        }.joinToString("")
    }

    /**
     * Formats an epoch millisecond timestamp into a localized (Date, Time) pair.
     * When lang is "fa", converts to Solar Hijri (Jalali) with Persian digits.
     */
    fun formatScanTimestamp(timestampMs: Long, lang: String): Pair<String, String> {
        return try {
            val dt = Instant.fromEpochMilliseconds(timestampMs)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            val gy = dt.date.year
            val gm = dt.date.monthNumber
            val gd = dt.date.dayOfMonth
            val hour = dt.time.hour.toString().padStart(2, '0')
            val min = dt.time.minute.toString().padStart(2, '0')

            when (lang) {
                "fa" -> {
                    val (jy, jm, jd) = gregorianToJalali(gy, gm, gd)
                    val dateStr = "$jy/${jm.toString().padStart(2, '0')}/${jd.toString().padStart(2, '0')}"
                    val timeStr = "$hour:$min"
                    toPersianDigits(dateStr) to toPersianDigits(timeStr)
                }
                "ru" -> {
                    val dateStr = "${gd.toString().padStart(2, '0')}.${gm.toString().padStart(2, '0')}.$gy"
                    val timeStr = "$hour:$min"
                    dateStr to timeStr
                }
                else -> {
                    val dateStr = "$gy-${gm.toString().padStart(2, '0')}-${gd.toString().padStart(2, '0')}"
                    val timeStr = "$hour:$min"
                    dateStr to timeStr
                }
            }
        } catch (_: Exception) {
            "N/A" to "N/A"
        }
    }

    /**
     * Parses a raw WHOIS date string (e.g., ISO-8601 or simple dates)
     * and formats it into a human-readable string with day-of-week, date, and time.
     * When lang is "fa", it converts to Solar Hijri (تاریخ و ساعت شمسی - روز هفته).
     */
    fun formatWhoisDate(rawDateStr: String?, lang: String): String {
        if (rawDateStr.isNullOrBlank()) return "—"
        val trimmed = rawDateStr.trim()

        val regex = "(\\d{4})[-/.](\\d{1,2})[-/.](\\d{1,2})(?:[T\\s](\\d{1,2}):(\\d{1,2}))?".toRegex()
        val match = regex.find(trimmed) ?: return trimmed

        val gy = match.groupValues[1].toIntOrNull() ?: return trimmed
        val gm = match.groupValues[2].toIntOrNull() ?: return trimmed
        val gd = match.groupValues[3].toIntOrNull() ?: return trimmed
        val hour = match.groupValues.getOrNull(4)?.takeIf { it.isNotEmpty() }?.toIntOrNull()
        val min = match.groupValues.getOrNull(5)?.takeIf { it.isNotEmpty() }?.toIntOrNull()

        if (gm !in 1..12 || gd !in 1..31) return trimmed

        val dow = dayOfWeek(gy, gm, gd)

        return when (lang) {
            "fa" -> {
                val (jy, jm, jd) = gregorianToJalali(gy, gm, gd)
                val dayNamesFa = arrayOf("یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه", "شنبه")
                val monthNamesFa = arrayOf(
                    "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
                    "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
                )
                val dayName = dayNamesFa.getOrElse(dow) { "" }
                val monthName = monthNamesFa.getOrElse(jm - 1) { "" }
                val dateText = buildString {
                    append("$dayName، $jd $monthName $jy")
                    if (hour != null && min != null) {
                        append(" - ساعت ${hour.toString().padStart(2, '0')}:${min.toString().padStart(2, '0')}")
                    }
                }
                toPersianDigits(dateText)
            }
            "ru" -> {
                val dayNamesRu = arrayOf("Воскресенье", "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота")
                val monthNamesRu = arrayOf(
                    "января", "февраля", "марта", "апреля", "мая", "июня",
                    "июля", "августа", "сентября", "октября", "ноября", "декабря"
                )
                val dayName = dayNamesRu.getOrElse(dow) { "" }
                val monthName = monthNamesRu.getOrElse(gm - 1) { "" }
                buildString {
                    append("$dayName, $gd $monthName $gy")
                    if (hour != null && min != null) {
                        append(" - ${hour.toString().padStart(2, '0')}:${min.toString().padStart(2, '0')}")
                    }
                }
            }
            else -> {
                val dayNamesEn = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
                val monthNamesEn = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                val dayName = dayNamesEn.getOrElse(dow) { "" }
                val monthName = monthNamesEn.getOrElse(gm - 1) { "" }
                buildString {
                    append("$dayName, $gd $monthName $gy")
                    if (hour != null && min != null) {
                        append(" - ${hour.toString().padStart(2, '0')}:${min.toString().padStart(2, '0')}")
                    }
                }
            }
        }
    }
}
