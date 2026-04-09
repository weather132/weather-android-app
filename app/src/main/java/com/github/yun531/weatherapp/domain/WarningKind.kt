package com.github.yun531.weatherapp.domain

enum class WarningKind(val label: String, val category: String) {
    HEAT("폭염", "기온"),
    COLDWAVE("한파", "기온"),
    RAIN("호우", "강수"),
    HEAVY_SNOW("대설", "강수"),
    WIND("강풍", "바람·해양"),
    HIGH_WAVE("풍랑", "바람·해양"),
    TYPHOON("태풍", "바람·해양"),
    DRY("건조", "기타"),
    FOG("안개", "기타"),
    TSUNAMI("해일", "기타"),
    EARTHQUAKE_TSUNAMI("지진해일", "기타");

    companion object {
        fun defaultSet(): Set<WarningKind> = entries.toSet()

        fun parseCsv(csv: String): Set<WarningKind> {
            if (csv.isBlank()) return emptySet()
            return csv.split(",")
                .mapNotNull { token -> runCatching { valueOf(token.trim()) }.getOrNull() }
                .toSet()
        }

        fun toCsv(set: Set<WarningKind>): String = set.joinToString(",") { it.name }

        fun groupedByCategory(): Map<String, List<WarningKind>> =
            entries.groupBy { it.category }
    }
}