package com.mrcoder20.portx.data.local

import app.cash.sqldelight.ColumnAdapter

val listOfIntAdapter = object : ColumnAdapter<List<Int>, String> {
    override fun decode(databaseValue: String): List<Int> {
        val cleanValue = databaseValue.trim().removePrefix("[").removeSuffix("]")
        if (cleanValue.isEmpty()) return emptyList()
        return try {
            cleanValue.split(",").mapNotNull { it.trim().toIntOrNull() }.distinct().sorted()
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
            cleanValue.split("|").mapNotNull { entry ->
                if (!entry.contains(":")) return@mapNotNull null
                val port = entry.substringBefore(":").trim().toIntOrNull()
                if (port != null) {
                    val rawVal = entry.substringAfter(":")
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
