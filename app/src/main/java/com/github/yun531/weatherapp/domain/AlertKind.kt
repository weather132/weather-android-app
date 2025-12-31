package com.github.yun531.weatherapp.domain

enum class AlertKind {
    RAIN_ONSET,
    WARNING_ISSUED;

    companion object {
        fun defaultSet(): Set<AlertKind> = setOf(RAIN_ONSET, WARNING_ISSUED)

        fun parseCsv(csv: String): Set<AlertKind> {
            if (csv.isBlank()) return emptySet()
            return csv.split(",")
                .mapNotNull { token -> runCatching { valueOf(token.trim()) }.getOrNull() }
                .toSet()
        }

        fun toCsv(set: Set<AlertKind>): String = set.joinToString(",") { it.name }
    }
}