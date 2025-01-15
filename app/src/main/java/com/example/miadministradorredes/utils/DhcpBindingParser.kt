package com.example.miadministradorredes.utils

class DhcpBindingParser {
    fun parseDhcpBinding(input: String): List<List<String>> {
        val result = mutableListOf<List<String>>()

        val lines = input.lines()

        for (line in lines.drop(2)) {
            val columns = line.trim().split(Regex("\\s+"))

            // Asegurarse de que tenga al menos 4 columnas (IP, Client-ID, Type, Lease)
            if (columns.size >= 4) {
                val ipAddress = columns[0]
                val clientId = columns[1]
                val type = columns[2]
                val leaseExpiration = columns[3]

                result.add(listOf(ipAddress, clientId, type, leaseExpiration))
            }
        }
        return result
    }

    fun extractRate(input: String): Float {
        val regex = Regex("""(\d+)\s+bits/sec""")
        val matchResult = regex.find(input)
        return matchResult?.groups?.get(1)?.value?.toFloatOrNull() ?: 0.0f
    }
}