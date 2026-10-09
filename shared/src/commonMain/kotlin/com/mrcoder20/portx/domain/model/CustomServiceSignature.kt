package com.mrcoder20.portx.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a declarative service detection and fingerprinting signature.
 * Inspired by modern declarative security templates (like Nuclei & RFC byte probes),
 * allowing zero-code extensible matching for application banners, cloud daemons, and SCADA protocols.
 */
@Serializable
data class CustomServiceSignature(
    val id: String,
    val name: String,
    val protocol: String = "TCP",
    val defaultPorts: List<Int> = emptyList(),
    val probePayloadHex: String? = null,
    val matchSubstrings: List<String> = emptyList(),
    val matchRegexes: List<String> = emptyList(),
    val versionExtractionRegex: String? = null,
    val category: String = "GENERAL", // CLOUD, INDUSTRIAL, DATABASE, REMOTE_ACCESS, IOT, GENERAL
    val riskSeverity: String = "INFO" // INFO, LOW, MEDIUM, HIGH, CRITICAL
) {
    /**
     * Evaluates whether a raw response banner matches this signature.
     */
    fun matches(banner: String): Boolean {
        if (banner.isEmpty()) return false
        val lowBanner = banner.lowercase()

        // 1. Check substring matches
        for (pattern in matchSubstrings) {
            if (lowBanner.contains(pattern.lowercase())) {
                return true
            }
        }

        // 2. Check regex matches
        for (regexStr in matchRegexes) {
            try {
                if (Regex(regexStr, RegexOption.IGNORE_CASE).containsMatchIn(banner)) {
                    return true
                }
            } catch (_: Exception) {
                // Ignore malformed regex
            }
        }

        return false
    }

    /**
     * Extracts a refined version string from the banner if a regex is configured.
     */
    fun extractVersion(banner: String): String? {
        val regexStr = versionExtractionRegex ?: return null
        return try {
            val match = Regex(regexStr, RegexOption.IGNORE_CASE).find(banner)
            match?.groups?.get(1)?.value ?: match?.value
        } catch (_: Exception) {
            null
        }
    }
}
