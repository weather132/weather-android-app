package com.github.yun531.weatherapp.domain

enum class AlertKind {
    RAIN_ONSET,
    WARNING_ISSUED;

    companion object {
        fun defaultSet(): Set<AlertKind> = setOf(RAIN_ONSET, WARNING_ISSUED)

        fun noEventsLabel(kinds: Set<AlertKind>): String = when {
            kinds.containsAll(setOf(RAIN_ONSET, WARNING_ISSUED)) -> "강수/특보"
            kinds.contains(RAIN_ONSET)                           -> "강수"
            kinds.contains(WARNING_ISSUED)                       -> "기상특보"
            else                                                 -> "알림 조건"
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