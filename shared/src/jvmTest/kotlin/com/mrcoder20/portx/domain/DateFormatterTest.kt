package com.mrcoder20.portx.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DateFormatterTest {

    @Test
    fun testGregorianToJalali1997() {
        val (jy, jm, jd) = DateFormatter.gregorianToJalali(1997, 9, 15)
        assertEquals(1376, jy)
        assertEquals(6, jm)
        assertEquals(24, jd)
    }

    @Test
    fun testGregorianToJalali2028() {
        val (jy, jm, jd) = DateFormatter.gregorianToJalali(2028, 9, 13)
        assertEquals(1407, jy)
        assertEquals(6, jm)
        assertEquals(23, jd)
    }

    @Test
    fun testDayOfWeek() {
        // 1997-09-15 was Monday (1)
        assertEquals(1, DateFormatter.dayOfWeek(1997, 9, 15))
        // 2028-09-13 is Wednesday (3)
        assertEquals(3, DateFormatter.dayOfWeek(2028, 9, 13))
    }

    @Test
    fun testFormatWhoisDateFa() {
        val formattedCreated = DateFormatter.formatWhoisDate("1997-09-15T07:00:00+0000", "fa")
        assertEquals("دوشنبه، ۲۴ شهریور ۱۳۷۶ - ساعت ۰۷:۰۰", formattedCreated)

        val formattedExpiry = DateFormatter.formatWhoisDate("2028-09-13T07:00:00+0000", "fa")
        assertEquals("چهارشنبه، ۲۳ شهریور ۱۴۰۷ - ساعت ۰۷:۰۰", formattedExpiry)
    }

    @Test
    fun testFormatWhoisDateEn() {
        val formatted = DateFormatter.formatWhoisDate("1997-09-15T07:00:00+0000", "en")
        assertEquals("Monday, 15 Sep 1997 - 07:00", formatted)
    }

    @Test
    fun testFormatWhoisDateFallback() {
        assertEquals("—", DateFormatter.formatWhoisDate(null, "fa"))
        assertEquals("—", DateFormatter.formatWhoisDate("", "fa"))
        assertEquals("UnknownDateString", DateFormatter.formatWhoisDate("UnknownDateString", "fa"))
    }

    @Test
    fun testFormatScanTimestamp() {
        // Test an epoch millis timestamp
        val ts = 874306800000L // 1997-09-15
        val (dateEn, timeEn) = DateFormatter.formatScanTimestamp(ts, "en")
        assertTrue(dateEn.contains("1997"))
        assertTrue(timeEn.contains(":"))

        val (dateFa, timeFa) = DateFormatter.formatScanTimestamp(ts, "fa")
        assertTrue(dateFa.contains("۱۳۷۶"))
        assertTrue(timeFa.isNotEmpty())
    }
}
