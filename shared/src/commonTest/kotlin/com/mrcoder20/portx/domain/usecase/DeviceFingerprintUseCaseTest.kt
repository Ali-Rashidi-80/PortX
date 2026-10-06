package com.mrcoder20.portx.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DeviceFingerprintUseCaseTest {

    private val useCase = DeviceFingerprintUseCase()

    @Test
    fun testIndustrialScadaSignatures() {
        // Modbus/TCP
        val modbus = useCase(setOf(502), emptyMap())
        assertEquals("Industrial Controller", modbus.deviceName)
        assertEquals("Modbus/TCP PLC", modbus.osFingerprint)

        // Siemens S7
        val s7 = useCase(setOf(102), emptyMap())
        assertEquals("Siemens Simatic S7", s7.deviceName)
        assertEquals("Siemens Industrial PLC", s7.osFingerprint)

        // BACnet
        val bacnet = useCase(setOf(47808), emptyMap())
        assertEquals("Building Controller", bacnet.deviceName)
        assertEquals("BACnet/IP Automation Unit", bacnet.osFingerprint)

        // OPC UA
        val opcua = useCase(setOf(4840), emptyMap())
        assertEquals("Industrial Gateway", opcua.deviceName)
        assertEquals("OPC UA Server", opcua.osFingerprint)
    }

    @Test
    fun testAndroidAdbSignature() {
        val result = useCase(setOf(5555), emptyMap())
        assertEquals("Android Device", result.deviceName)
        assertEquals("Android (ADB Enabled)", result.osFingerprint)
    }

    @Test
    fun testPrinterSignatures() {
        // Raw print port 9100
        val p9100 = useCase(setOf(9100), mapOf(9100 to "Hewlett-Packard JetDirect"))
        assertEquals("Network Printer", p9100.deviceName)
        assertEquals("HP LaserJet / JetDirect", p9100.osFingerprint)

        // Canon banner
        val canon = useCase(setOf(80), mapOf(80 to "Canon imageRUNNER ADVANCE Web Interface"))
        assertEquals("Network Printer", canon.deviceName)
        assertEquals("Canon Network Print Device", canon.osFingerprint)

        // Brother banner
        val brother = useCase(setOf(80), mapOf(80 to "Brother NC-7400w"))
        assertEquals("Network Printer", brother.deviceName)
        assertEquals("Brother Network Print Device", brother.osFingerprint)
    }

    @Test
    fun testRouterAndVpnSignatures() {
        // WireGuard
        val wireguard = useCase(setOf(51820), emptyMap())
        assertEquals("Network Gateway", wireguard.deviceName)
        assertEquals("WireGuard VPN Gateway", wireguard.osFingerprint)

        // OpenVPN
        val openvpn = useCase(setOf(1194), emptyMap())
        assertEquals("Network Gateway", openvpn.deviceName)
        assertEquals("OpenVPN Server", openvpn.osFingerprint)

        // MikroTik RouterOS
        val mikrotik = useCase(setOf(80), mapOf(80 to "RouterOS v7.14"))
        assertEquals("Network Gateway", mikrotik.deviceName)
        assertEquals("MikroTik RouterOS", mikrotik.osFingerprint)

        // pfSense
        val pfsense = useCase(setOf(443), mapOf(443 to "pfSense WebGUI"))
        assertEquals("Network Gateway", pfsense.deviceName)
        assertEquals("pfSense FreeBSD", pfsense.osFingerprint)
    }

    @Test
    fun testNasStorageSignatures() {
        val synology = useCase(setOf(5000), mapOf(5000 to "Synology DiskStation DSM 7.2"))
        assertEquals("Network Storage (NAS)", synology.deviceName)
        assertEquals("Synology DSM", synology.osFingerprint)

        val qnap = useCase(setOf(8080), mapOf(8080 to "QNAP QTS Turbo NAS"))
        assertEquals("Network Storage (NAS)", qnap.deviceName)
        assertEquals("QNAP QTS", qnap.osFingerprint)
    }

    @Test
    fun testCloudAndContainerSignatures() {
        // Kubernetes
        val k8s = useCase(setOf(6443), emptyMap())
        assertEquals("Kubernetes Node", k8s.deviceName)
        assertEquals("Kubernetes Cluster Node", k8s.osFingerprint)

        // Docker
        val docker = useCase(setOf(2375), emptyMap())
        assertEquals("Container Host", docker.deviceName)
        assertEquals("Docker Engine Daemon", docker.osFingerprint)

        // ZooKeeper
        val zk = useCase(setOf(2181), emptyMap())
        assertEquals("Coordination Service", zk.deviceName)
        assertEquals("Apache ZooKeeper Cluster Node", zk.osFingerprint)

        // Kafka
        val kafka = useCase(setOf(9092), emptyMap())
        assertEquals("Message Streaming", kafka.deviceName)
        assertEquals("Apache Kafka Broker", kafka.osFingerprint)
    }

    @Test
    fun testDatabaseSignatures() {
        val pg = useCase(setOf(5432), emptyMap())
        assertEquals("Database Server", pg.deviceName)
        assertEquals("PostgreSQL Database Server", pg.osFingerprint)

        val redis = useCase(setOf(6379), emptyMap())
        assertEquals("Database Server", redis.deviceName)
        assertEquals("Redis Cache/Datastore", redis.osFingerprint)

        val mongo = useCase(setOf(27017), emptyMap())
        assertEquals("Database Server", mongo.deviceName)
        assertEquals("MongoDB NoSQL Server", mongo.osFingerprint)
    }

    @Test
    fun testFallbackUnknown() {
        val unknown = useCase(setOf(9999), emptyMap())
        assertNull(unknown.deviceName)
        assertNull(unknown.osFingerprint)
    }
}
