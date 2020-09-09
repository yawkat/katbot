package at.yawk.katbot.nina

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import java.awt.geom.Path2D

class PolygonDeserializer : JsonDeserializer<Path2D.Double>() {
    companion object {
        fun parse(s: String): Path2D.Double {
            val parts = s.split(' ')
            val polygon = Path2D.Double()
            for ((i, part) in parts.withIndex()) {
                val (long, lat) = part.split(',').map { it.toDouble() }
                if (i == 0) {
                    polygon.moveTo(long, lat)
                } else {
                    polygon.lineTo(long, lat)
                }
            }
            polygon.closePath()
            return polygon
        }
    }

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext) = parse(p.valueAsString)
}