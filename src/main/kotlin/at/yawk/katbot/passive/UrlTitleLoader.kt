/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.passive

import at.yawk.katbot.EventBus
import at.yawk.katbot.Subscribe
import at.yawk.katbot.security.PermissionName
import at.yawk.katbot.security.Security
import at.yawk.katbot.sendMessageSafe
import org.apache.shiro.util.ThreadContext
import org.jsoup.Jsoup
import org.jsoup.helper.HttpConnection
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.ThreadLocalRandom
import javax.inject.Inject

private val URL_PATTERN = "(https?://)?(([0-9]{1,3}\\.){3}[0-9]{1,3}|\\w+\\.\\w{2,8})(\\S+[^\\s\\.\"'])?".toPattern()

fun isPrintableAsciiChar(it: Char) = it in ' '..'~'

class UrlTitleLoader @Inject constructor(
        val eventBus: EventBus
) {
    private val executor = Executors.newSingleThreadExecutor()

    fun start() {
        eventBus.subscribe(this)
    }

    @Subscribe(priority = 1000)
    fun onPublicMessage(event: ChannelMessageEvent) {
        val permission = Security.createPermissionForChannelAndName(event.channel, PermissionName.LOAD_URL)
        if (!ThreadContext.getSubject().isPermitted(permission)) {
            return
        }

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
            val title = getTitle(url)
            if (title != null) {
                val canonicalTitle = title.filter { isPrintableAsciiChar(it) }
                if (canonicalTitle.isNotEmpty()) {
                    val nick = event.actor.nick
                    val safeNick = nick.substring(0, nick.length / 2) + '\u200B' + nick.substring(nick.length / 2)
                    event.channel.sendMessageSafe("$safeNick's title: $canonicalTitle")
                }
            }
        }
    }

    companion object {
        fun getTitle(url: URL): String? {
            try {
                val document = Jsoup.connect(url.toString())
                        // we set this everywhere, why not
                        // https://github.com/ytdl-org/youtube-dl/blob/master/youtube_dl/extractor/youtube.py#L265
                    .cookie("CONSENT", "YES+cb.20210328-17-p0.en+FX+" + ThreadLocalRandom.current().nextInt(100, 999))
                    .timeout(1000)
                    .get()
                return document.title()
            } catch (e: IOException) {
                return null
            }
        }
    }
}