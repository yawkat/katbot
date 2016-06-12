/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot

import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.io.SyndFeedInput
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.slf4j.LoggerFactory
import org.xml.sax.InputSource
import java.net.URI
import java.time.Instant
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider

/**
 * @author yawkat
 */
class RssFeedListener @Inject constructor(
        val ircProvider: IrcProvider,
        val executor: ScheduledExecutorService,
        val config: Config,
        val httpClient: HttpClient,
        // lazy init
        val urlShortener: Provider<UrlShortener>
) {
    companion object {
        private val log = LoggerFactory.getLogger(RssFeedListener::class.java)
    }

    val input = SyndFeedInput()
    val lastPollTimes = hashMapOf<URI, Instant>()

    private fun loadFeed(uri: URI): SyndFeed {
        val response = httpClient.execute(HttpGet(uri))
        response.entity.content.use { `in` -> return input.build(InputSource(`in`)) }
    }

    @Synchronized
    private fun poll() {
        for (conf in config.feeds.entries) {
            val uri = conf.key

            val feed = loadFeed(uri)
            val deadline = lastPollTimes[uri]
            var newDeadline: Instant? = null
            for (feedEntry in feed.entries) {
                val entryTime = feedEntry.publishedDate.toInstant()
                if (newDeadline == null || entryTime.isAfter(newDeadline)) {
                    newDeadline = entryTime
                }
                if (deadline != null && entryTime.isAfter(deadline)) {
                    fire(conf.value, feedEntry)
                }
            }
            if (newDeadline != null) {
                lastPollTimes[uri] = newDeadline
            }
        }
    }

    private fun fire(conf: FeedConfiguration, feedEntry: SyndEntry) {
        val message = conf.messagePattern
                .with("title", feedEntry.title)
                .with("uri", feedEntry.uri)
                .with("uri.short", urlShortener.get().shorten(URI.create(feedEntry.uri)).toString())

        log.info("Sending feed update '{}' to {} channels", message, conf.channels.size)

        message.sendTo(ircProvider.findChannels(conf.channels))
    }

    fun start() {
        executor.scheduleWithFixedDelay({
            try {
                poll()
            } catch (e: Exception) {
                log.error("Failed to poll RSS feeds", e)
            }
        }, 0, 1, TimeUnit.MINUTES)
    }

    data class FeedConfiguration(
            val channels: List<String>,
            val messagePattern: at.yawk.katbot.template.Template
    )
}