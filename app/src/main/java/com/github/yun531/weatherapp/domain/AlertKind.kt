package com.github.yun531.weatherapp.domain

enum class AlertKind(val shortLabel: String) {
    RAIN_ONSET("강수"),
    WARNING_ISSUED("기상특보"),
    AIR_POLLUTION("미세먼지");

    companion object {
        fun defaultSet(): Set<AlertKind> = setOf(RAIN_ONSET, WARNING_ISSUED, AIR_POLLUTION)

        fun fromWire(raw: String?): AlertKind? =
            raw?.let { runCatching { valueOf(it) }.getOrNull() }

        fun noEventsLabel(kinds: Set<AlertKind>): String {
            val labels = entries.filter { it in kinds }.map { it.shortLabel }
            return if (labels.isEmpty()) "알림 조건" else labels.joinToString("/")
        }

        fun parseCsv(csv: String): Set<AlertKind> {
            if (csv.isBlank()) return emptySet()
            return csv.split(",")
                .mapNotNull { token -> runCatching { valueOf(token.trim()) }.getOrNull() }
                .toSet()
        }

        fun toCsv(set: Set<AlertKind>): String = set.joinToString(",") { it.name }
    }
}