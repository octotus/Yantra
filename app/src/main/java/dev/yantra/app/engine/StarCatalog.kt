package dev.yantra.app.engine

import android.content.Context
import org.json.JSONArray
import kotlin.math.*

data class StarRecord(val id: Int, val coordinate: EquatorialCoordinate, val magnitude: Double)

data class StellarPattern(
    val kind: String,
    val index: Int,
    val stars: List<Int>,
    val edges: List<Pair<Int, Int>>,
)

data class ChartFrame(val center: EquatorialCoordinate, val extent: Double)

class StarCatalog(val stars: List<StarRecord>, val patterns: List<StellarPattern>) {
    val byId = stars.associateBy { it.id }

    fun pattern(kind: String, index: Int): StellarPattern = patterns.first { it.kind == kind && it.index == index }

    /** The frame is anchored to J2000, independent of time, observer and ayanamsa.
     * This reference longitude only frames the view; actual boundaries come from Swiss Ephemeris. */
    fun frame(pattern: StellarPattern): ChartFrame {
        val width = if (pattern.kind == "rashi") 30.0 else 360.0 / 27.0
        fun reference(longitude: Double): EquatorialCoordinate {
            val lon = toRadians(longitude + 23.853055)
            val eps = toRadians(23.4392911)
            return EquatorialCoordinate(normalizeDegrees(Math.toDegrees(atan2(sin(lon) * cos(eps), cos(lon)))), Math.toDegrees(asin(sin(lon) * sin(eps))))
        }
        val selected = pattern.stars.map { byId.getValue(it).coordinate }
        val middle = reference((pattern.index + 0.5) * width)
        val center = coordinateCenter(listOf(coordinateCenter(selected), middle))
        val projection = StereographicProjection(center)
        val context = listOf(reference((pattern.index - 0.55) * width), reference((pattern.index + 1.55) * width))
        val points = (selected + context).mapNotNull(projection::project)
        val extent = points.maxOf { max(abs(it.x), abs(it.y)) }.coerceAtLeast(0.22) * 1.22
        return ChartFrame(center, extent)
    }

    companion object {
        @Volatile private var cached: StarCatalog? = null

        fun load(context: Context): StarCatalog = cached ?: synchronized(this) {
            cached ?: read(context.applicationContext).also { cached = it }
        }

        private fun read(context: Context): StarCatalog {
            val stars = context.assets.open("celestial/stars.tsv").bufferedReader().useLines { lines ->
                lines.filter { it.isNotBlank() && !it.startsWith('#') }.map { line ->
                    val values = line.split('\t')
                    StarRecord(values[0].toInt(), EquatorialCoordinate(values[1].toDouble(), values[2].toDouble()), values[3].toDouble())
                }.toList()
            }
            val json = JSONArray(context.assets.open("celestial/patterns.json").bufferedReader().use { it.readText() })
            val patterns = (0 until json.length()).map { i ->
                val item = json.getJSONObject(i)
                val ids = item.getJSONArray("stars")
                val edges = item.getJSONArray("edges")
                StellarPattern(item.getString("kind"), item.getInt("index"),
                    (0 until ids.length()).map(ids::getInt),
                    (0 until edges.length()).map { edges.getJSONArray(it).let { edge -> edge.getInt(0) to edge.getInt(1) } })
            }
            val ids = stars.map { it.id }.toSet()
            require(patterns.count { it.kind == "rashi" } == 12 && patterns.count { it.kind == "nakshatra" } == 27)
            require(patterns.all { pattern -> pattern.stars.all { it in ids } })
            return StarCatalog(stars, patterns)
        }
    }
}
