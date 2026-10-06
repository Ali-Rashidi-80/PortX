package com.mrcoder20.portx.data.local

import app.cash.sqldelight.ColumnAdapter

val listOfIntAdapter = object : ColumnAdapter<List<Int>, String> {
    override fun decode(databaseValue: String): List<Int> {
        val cleanValue = databaseValue.trim().removePrefix("[").removeSuffix("]")
        if (cleanValue.isEmpty()) return emptyList()
        return try {
            cleanValue.split(Regex("""[,;\s]+""")).mapNotNull { it.trim().toIntOrNull() }.distinct().sorted()
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun encode(value: List<Int>): String = value.distinct().sorted().joinToString(separator = ",")
}

val mapIntStringAdapter = object : ColumnAdapter<Map<Int, String>, String> {
    override fun decode(databaseValue: String): Map<Int, String> {
        val cleanValue = databaseValue.trim().removePrefix("{").removeSuffix("}")
        if (cleanValue.isEmpty()) return emptyMap()
        return try {
            val entries = when {
                cleanValue.contains("|") -> cleanValue.split("|")
                // Only split on comma if followed by a subsequent port entry (e.g. legacy '80:http, 443:https' or '\"80\":\"http\", \"443\":...')
                Regex(""",\s*"?\d+"?\s*:""").containsMatchIn(cleanValue) -> cleanValue.split(Regex(""",\s*(?="?\d+"?\s*:)"""))
                else -> listOf(cleanValue)
            }
            entries.mapNotNull { entry ->
                if (!entry.contains(":")) return@mapNotNull null
                val rawPort = entry.substringBefore(":").trim().removeSurrounding("\"")
                val port = rawPort.toIntOrNull()
                if (port != null) {
                    val rawVal = entry.substringAfter(":").trim().removeSurrounding("\"")
                    val unescapedVal = rawVal.replace("&#124;", "|")
                    port to unescapedVal
                } else null
            }.toMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    override fun encode(value: Map<Int, String>): String =
        value.toSortedMap().entries.joinToString(separator = "|") { (port, text) ->
            val escapedText = text.replace("|", "&#124;")
            "$port:$escapedText"
        }
}

val booleanAdapter = object : ColumnAdapter<Boolean, Long> {
    override fun decode(databaseValue: Long): Boolean = databaseValue == 1L
    override fun encode(value: Boolean): Long = if (value) 1L else 0L
}
