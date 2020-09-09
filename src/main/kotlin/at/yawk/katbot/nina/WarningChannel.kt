package at.yawk.katbot.nina

import java.awt.geom.Point2D

data class WarningChannel(
        val channel: String,
        val longitude: Double,
        val latitude: Double
) {
    val point: Point2D.Double
        get() = Point2D.Double(longitude, latitude)
}