package at.yawk.katbot.nina

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.jsoup.Jsoup
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.NodeTraversor
import org.jsoup.select.NodeVisitor
import java.awt.geom.Path2D
import java.lang.StringBuilder
import java.time.OffsetDateTime

data class Announcement(
        val identifier: String,
        val sender: String,
        val sent: OffsetDateTime,
        val status: String,
        val msgType: String,
        val scope: String,
        val code: List<String>,
        val references: String?,
        val info: List<Info>
) {
    data class Info(
            val language: String,
            val category: List<String>,
            val event: String,
            val urgency: String,
            val severity: String,
            val certainty: String,
            val eventCode: List<Value>,
            val headline: String,
            val description: String?,
            val instruction: String?,
            val web: String?,
            val contact: String?,
            val parameter: List<Value>,
            val area: List<Area>,
            val expires: OffsetDateTime? = null
    )

    data class Value(
            val valueName: String,
            val value: String
    )

    data class Area(
            val areaDesc: String,
            @JsonDeserialize(contentUsing = PolygonDeserializer::class) val polygon: List<Path2D.Double>,
            val geocode: List<Value>
    )

    val bestInfo = info.firstOrNull { it.language == "DE" } ?: info.first()
}

fun Announcement.toText(): String {
    return buildString {
        appendln(bestInfo.headline)
        appendln("=".repeat(bestInfo.headline.length))
        appendln()
        appendln(Jsoup.parseBodyFragment(bestInfo.description).body().toPlainText())
        appendln()
        if (bestInfo.web != null) {
            appendln(bestInfo.web)
            appendln()
        }
        appendln(identifier)
    }
}

private fun Node.toPlainText(): String {
    val toPlainText = ToPlainText()
    NodeTraversor.traverse(toPlainText, this)
    return toPlainText.text.toString()
}

private class ToPlainText : NodeVisitor {
    val text = StringBuilder()

    override fun head(node: Node, depth: Int) {
        if (node is TextNode) {
            text.append(node.text())
        } else if (node.nodeName() == "br" || node.nodeName() == "p") {
            text.append('\n')
        }
    }

    override fun tail(node: Node, depth: Int) {
        if (node.nodeName() == "p") {
            text.append('\n')
        }
    }
}