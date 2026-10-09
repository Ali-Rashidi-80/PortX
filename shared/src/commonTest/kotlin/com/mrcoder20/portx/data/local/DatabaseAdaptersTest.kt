package com.mrcoder20.portx.data.local

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DatabaseAdaptersTest {

    @Test
    fun testListOfIntAdapter_encoding() {
        val input = listOf(443, 80, 8080, 80, 22)
        val encoded = listOfIntAdapter.encode(input)
        assertEquals("22,80,443,8080", encoded, "Encoded string should be distinct, sorted, and comma-separated")
    }

    @Test
    fun testListOfIntAdapter_decodeStandard() {
        val input = "22,80,443,8080"
        val decoded = listOfIntAdapter.decode(input)
        assertEquals(listOf(22, 80, 443, 8080), decoded)
    }

    @Test
    fun testListOfIntAdapter_decodeJsonArrayFormat() {
        val input = "[22, 80, 443]"
        val decoded = listOfIntAdapter.decode(input)
        assertEquals(listOf(22, 80, 443), decoded)
    }

    @Test
    fun testListOfIntAdapter_decodeSemicolonsAndSpaces() {
        val input = "22; 80   443, 8080"
        val decoded = listOfIntAdapter.decode(input)
        assertEquals(listOf(22, 80, 443, 8080), decoded)
    }

    @Test
    fun testListOfIntAdapter_decodeEmptyAndCorrupt() {
        assertEquals(emptyList(), listOfIntAdapter.decode(""))
        assertEquals(emptyList(), listOfIntAdapter.decode("   "))
        assertEquals(emptyList(), listOfIntAdapter.decode("[]"))
        assertEquals(emptyList(), listOfIntAdapter.decode("abc, xyz, corrupt"))
    }

    @Test
    fun testMapIntStringAdapter_encodingWithPipeEscaping() {
        val map = mapOf(
            80 to "HTTP/1.1|Server: Apache",
            443 to "HTTPS Service",
            22 to "SSH-2.0-OpenSSH_8.9p1"
        )
        val encoded = mapIntStringAdapter.encode(map)
        assertTrue(encoded.contains("80:HTTP/1.1&#124;Server: Apache"), "Pipes in banners must be escaped with &#124;")
        assertTrue(encoded.startsWith("22:SSH-2.0-OpenSSH_8.9p1|80:"), "Should be sorted by port")
    }

    @Test
    fun testMapIntStringAdapter_decodePipeSeparatedWithEscapedPipes() {
        val dbValue = "22:OpenSSH|80:HTTP/1.1&#124;Apache|443:Nginx"
        val decoded = mapIntStringAdapter.decode(dbValue)
        assertEquals(3, decoded.size)
        assertEquals("OpenSSH", decoded[22])
        assertEquals("HTTP/1.1|Apache", decoded[80], "Escaped pipe must be restored to literal pipe")
        assertEquals("Nginx", decoded[443])
    }

    @Test
    fun testMapIntStringAdapter_decodeLegacyCommaSeparated() {
        val dbValue = "80:http, 443:https, 8080:http-alt"
        val decoded = mapIntStringAdapter.decode(dbValue)
        assertEquals(3, decoded.size)
        assertEquals("http", decoded[80])
        assertEquals("https", decoded[443])
        assertEquals("http-alt", decoded[8080])
    }

    @Test
    fun testMapIntStringAdapter_decodeEmptyAndCorrupt() {
        assertEquals(emptyMap(), mapIntStringAdapter.decode(""))
        assertEquals(emptyMap(), mapIntStringAdapter.decode("   "))
        assertEquals(emptyMap(), mapIntStringAdapter.decode("{}"))
        assertEquals(emptyMap(), mapIntStringAdapter.decode("invalid:not-a-number:entry"))
    }

    @Test
    fun testBooleanAdapter() {
        assertEquals(1L, booleanAdapter.encode(true))
        assertEquals(0L, booleanAdapter.encode(false))

        assertTrue(booleanAdapter.decode(1L))
        assertFalse(booleanAdapter.decode(0L))
        assertFalse(booleanAdapter.decode(2L))
        assertFalse(booleanAdapter.decode(-1L))
    }
}
