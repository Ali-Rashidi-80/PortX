package com.mrcoder20.portx.data.network

import com.mrcoder20.portx.domain.SecurityHarden
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RemoteApiTest {

    private val client = SecurityHarden.createSecureClient()
    private val remoteApi = RemoteApiImpl(client)

    @AfterTest
    fun tearDown() {
        client.close()
    }

    @Test
    fun testGetThreatIntel() = runTest {
        val info = remoteApi.getThreatIntel("8.8.8.8")
        assertEquals("8.8.8.8", info.ip)
        assertTrue(info.threatLevel.isNotEmpty())
        assertTrue(info.lastReported.isNotEmpty())
    }

    @Test
    fun testCheckCve() = runTest {
        val cves = remoteApi.checkCve(443)
        assertTrue(cves.isNotEmpty(), "CVE list should return findings for scanned port")
        val first = cves.first()
        assertTrue(first.id.startsWith("CVE-"))
        assertTrue(first.description.contains("443"))
        assertEquals("High", first.severity)
    }

    @Test
    fun testCheckAbuseIP() = runTest {
        val score = remoteApi.checkAbuseIP("192.168.1.1")
        assertTrue(score >= 0, "AbuseIP score must be non-negative")
    }
}
