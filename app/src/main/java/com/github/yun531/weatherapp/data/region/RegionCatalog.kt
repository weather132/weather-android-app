package com.github.yun531.weatherapp.data.region

import android.content.Context
import com.github.yun531.weatherapp.R
import java.io.BufferedReader
import java.io.InputStreamReader

data class RegionOption(val name: String, val id: String)

class RegionCatalog private constructor(
    private val options: List<RegionOption>,
    private val idToName: Map<String, String>
) {
    fun allOptions(): List<RegionOption> = options
    fun nameOf(id: String): String = idToName[id] ?: id

    companion object {
        @Volatile private var instance: RegionCatalog? = null

        fun get(context: Context): RegionCatalog {
            return instance ?: synchronized(this) {
                instance ?: load(context).also { instance = it }
            }
        }

        private fun load(context: Context): RegionCatalog {
            val opts = mutableListOf<RegionOption>()
            val map = mutableMapOf<String, String>()

            context.resources.openRawResource(R.raw.regions).use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).useLines { lines ->
                    lines.map { it.trim() }
                        .filter { it.isNotBlank() }
                        .forEach { line ->
                            val parts = line.split(",")
                            if (parts.size >= 2) {
                                val name = parts[0].trim()
                                val id = parts[1].trim()
                                if (id.isNotBlank()) {
                                    opts += RegionOption(name, id)
                                    map[id] = name
                                }
                            }
                        }
                }
            }
            return RegionCatalog(opts, map)
        }
    }
}