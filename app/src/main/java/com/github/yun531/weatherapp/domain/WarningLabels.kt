package com.github.yun531.weatherapp.domain

object WarningLabels {

    fun kindLabel(kind: String?): String = when (kind) {
        "HEAT"               -> "폭염"
        "COLDWAVE"           -> "한파"
        "HEAVY_SNOW"         -> "대설"
        "RAIN"               -> "호우"
        "DRY"                -> "건조"
        "WIND"               -> "강풍"
        "FOG"                -> "안개"
        "HIGH_WAVE"          -> "풍랑"
        "TYPHOON"            -> "태풍"
        "TSUNAMI"            -> "해일"
        "EARTHQUAKE_TSUNAMI" -> "지진해일"
        else                 -> kind ?: "기상특보"
    }

    fun levelLabel(level: String?): String = when (level) {
        "WATCH"    -> "예비특보"
        "ADVISORY" -> "주의보"
        "WARNING"  -> "경보"
        else       -> level ?: ""
    }
}