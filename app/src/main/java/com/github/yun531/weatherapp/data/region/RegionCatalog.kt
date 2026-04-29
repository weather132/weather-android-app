package com.github.yun531.weatherapp.data.region

import android.content.Context
import com.github.yun531.weatherapp.R
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 단일 지역 항목.
 *
 * @property province 시/도 정식 명칭 (예: "강원특별자치도")
 * @property name 시/군/구 이름 (예: "고성")
 * @property id cityRegionCode (예: "11D20402"). 백엔드 전송 키.
 */
data class RegionOption(
    val province: String,
    val name: String,
    val id: String
)

/**
 * regions.txt 카탈로그.
 *
 * 파일 포맷: `시도,시군구,cityRegionCode` (헤더 없음, UTF-8)
 *
 * 정렬 정책은 파일 작성 순서를 따른다 (시/도는 행정 관습 순, 시/군/구는 가나다순).
 */
class RegionCatalog private constructor(
    private val options: List<RegionOption>,
    private val idToOption: Map<String, RegionOption>
) {
    fun allOptions(): List<RegionOption> = options

    /** ForecastScreen 등에서 cityRegionCode로 표시명을 조회할 때 사용. */
    fun nameOf(id: String): String = idToOption[id]?.name ?: id

    fun regionOf(id: String): RegionOption? = idToOption[id]

    /**
     * 시/도별로 그룹핑된 지역 목록.
     * `groupBy`는 LinkedHashMap을 반환하므로 파일 작성 순서가 유지.
     */
    fun groupedByProvince(): Map<String, List<RegionOption>> =
        options.groupBy { it.province }

    /**
     * 시/군/구 이름의 접두 매칭. 결과는 시/군/구 가나다순.
     * 시/도 컬럼은 매칭 대상 아님.
     */
    fun searchByPrefix(query: String): List<RegionOption> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        return options
            .filter { it.name.startsWith(q) }
            .sortedBy { it.name }
    }

    companion object {
        @Volatile private var instance: RegionCatalog? = null

        fun get(context: Context): RegionCatalog {
            return instance ?: synchronized(this) {
                instance ?: load(context).also { instance = it }
            }
        }

        private fun load(context: Context): RegionCatalog {
            val opts = mutableListOf<RegionOption>()
            val map = mutableMapOf<String, RegionOption>()

            context.resources.openRawResource(R.raw.regions).use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).useLines { lines ->
                    lines.map { it.trim() }
                        .filter { it.isNotBlank() }
                        .forEach { line ->
                            val parts = line.split(",")
                            if (parts.size >= 3) {
                                val province = parts[0].trim()
                                val name = parts[1].trim()
                                val id = parts[2].trim()
                                if (province.isNotBlank() && name.isNotBlank() && id.isNotBlank()) {
                                    val opt = RegionOption(province, name, id)
                                    opts += opt
                                    map[id] = opt
                                }
                            }
                        }
                }
            }
            return RegionCatalog(opts, map)
        }
    }
}