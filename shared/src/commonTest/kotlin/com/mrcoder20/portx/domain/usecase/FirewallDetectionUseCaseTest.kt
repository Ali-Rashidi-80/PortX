package com.mrcoder20.portx.domain.usecase

import com.mrcoder20.portx.domain.model.ScanResult
import kotlin.test.Test
import kotlin.test.assertTrue

class FirewallDetectionUseCaseTest {

    private val useCase = FirewallDetectionUseCase()

    private fun createResult(openPorts: List<Int>): ScanResult {
        return ScanResult(
            target = "192.168.1.1",
            openPorts = openPorts,
            timestamp = 1000L,
            securityScore = 80
        )
    }

    @Test
    fun testZeroOpenPorts() {
        val assessment = useCase(createResult(emptyList()))
        assertTrue(assessment.contains("High Probability of Firewall"), "Zero open ports indicates firewall or host down")
    }

    @Test
    fun testHardenedPerimeter() {
        val onePort = useCase(createResult(listOf(443)))
        assertTrue(onePort.contains("Hardened Perimeter"), "1 open port must be classified as hardened perimeter")

        val twoPorts = useCase(createResult(listOf(80, 443)))
        assertTrue(twoPorts.contains("Hardened Perimeter"), "2 open ports must be classified as hardened perimeter")
    }

    @Test
    fun testStandardNetworkProfile() {
        val threePorts = useCase(createResult(listOf(22, 80, 443)))
        assertTrue(threePorts.contains("Standard Network Profile"), "3 open ports must be standard network profile")

        val tenPorts = useCase(createResult((1..10).toList()))
        assertTrue(tenPorts.contains("Standard Network Profile"), "10 open ports must be standard network profile")
    }

    @Test
    fun testOpenPerimeter() {
        val elevenPorts = useCase(createResult((1..11).toList()))
        assertTrue(elevenPorts.contains("Open Perimeter"), "More than 10 open ports indicates open perimeter")
    }
}
