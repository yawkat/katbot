/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.passive

import at.yawk.katbot.EventBus
import at.yawk.katbot.Subscribe
import at.yawk.katbot.action.isPrintableAsciiChar
import at.yawk.katbot.sendMessageSafe
import org.jsoup.Jsoup
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.Executors
import javax.inject.Inject

private val URL_PATTERN = "(https?://)?(([0-9]{1,3}\\.){3}[0-9]{1,3}|\\w+\\.\\w{2,8})(\\S+[^\\s\\.\"'])?".toPattern()

/**
 * @author yawkat
 */
class UrlTitleLoader @Inject constructor(
        val eventBus: EventBus
) {
    private val executor = Executors.newSingleThreadExecutor()

    fun start() {
        eventBus.subscribe(this)
    }

    @Subscribe(priority = 1000)
    fun onPublicMessage(event: ChannelMessageEvent) {
        val matcher = URL_PATTERN.matcher(event.message)
        if (!matcher.find()) return
        val url = try {
            URL(matcher.group())
        } catch (e: MalformedURLException) {
            return
        }
        if (url.protocol != "http" && url.protocol != "https") return
        if (url.host == "s.yawk.at") return
        executor.execute {
            try {
                val document = Jsoup.connect(url.toString()).timeout(1000).get()
                val title = document.title()
                if (title != null) {
                    val canonicalTitle = title.filter { isPrintableAsciiChar(it) }
                    event.channel.sendMessageSafe("${event.actor.nick}'s title: $canonicalTitle")
                }
            } catch(e: IOException) {
            }
        }
    }
}