package at.yawk.katbot

import org.jsoup.Jsoup
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent
import java.net.URI
import java.net.URLEncoder
import java.util.regex.Pattern
import javax.inject.Inject


/**
 * @author yawkat
 */
class UrbanDictionary @Inject constructor(val ircProvider: IrcProvider, val urlShortener: UrlShortener) {
    companion object {
        private val PATTERN = "~(?:ud|urban|ub|urbandictionary) (.+)".toPattern(Pattern.CASE_INSENSITIVE)
        private const val MESSAGE_LENGTH_LIMIT = 100
    }

    fun start() {
        ircProvider.registerEventListener(this)
    }

    @Subscribe
    fun onPublicMessage(event: ChannelMessageEvent) {
        val match = PATTERN.matcher(event.message)
        if (match.matches()) {
            val term = match.group(1)
            val url = "http://www.urbandictionary.com/define.php?term=" + URLEncoder.encode(term, "UTF-8")
            val document = Jsoup.connect(url).get()
            val definition = document.select(".def-panel").first()

            if (!definition.select(".contributor").any()) {
                event.channel.sendMessage("${event.actor.nick}, no result found!")
                return
            }

            val title = definition.select(".def-header .word").text()
            val meaning = definition.select(".meaning").text()
            val example = definition.select(".example").text()

            val definitionString = "$title is: $meaning | \"$example\""
            val shortened = if (definitionString.length > MESSAGE_LENGTH_LIMIT) {
                // try to separate at a space character
                val spaceIndex = definitionString.lastIndexOf(' ', MESSAGE_LENGTH_LIMIT)
                definitionString.substring(0, if (spaceIndex == -1) MESSAGE_LENGTH_LIMIT else spaceIndex) + "…"
            } else definitionString

            val shortUrl = urlShortener.shorten(URI(url))
            event.channel.sendMessage("${event.actor.nick}, $shortened $shortUrl")
        }
    }
}