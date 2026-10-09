package com.mrcoder20.portx.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class SecurityHardenTest {

    @Test
    fun testTransformInvolution() {
        val original = "https://api.github.com/repos/mr-coder20/PortX"
        val obfuscated = SecurityHarden.transform(original)

        assertNotEquals(original, obfuscated, "Obfuscated string must not match plaintext")
        val restored = SecurityHarden.transform(obfuscated)
        assertEquals(original, restored, "Double transformation (XOR involution) must perfectly restore original string")
    }

    @Test
    fun testTransformEmptyAndSpecialChars() {
        assertEquals("", SecurityHarden.transform(""))
        
        val complex = "Token_123!@#\$%^&*()_+{}|:\"<>?~`"
        val obfuscated = SecurityHarden.transform(complex)
        assertEquals(complex, SecurityHarden.transform(obfuscated))
    }

    @Test
    fun testCreateSecureClientInstantiation() {
        val client = SecurityHarden.createSecureClient()
        assertNotNull(client, "Secure HTTP client factory must return a non-null configured engine")
        client.close()
    }
}
