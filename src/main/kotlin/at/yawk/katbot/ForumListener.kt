/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot

import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.*
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider

/**
 * @author yawkat
 */
class ForumListener @Inject constructor(
        val executor: ScheduledExecutorService,
        val config: Config,
        // lazy init
        val urlShortener: Provider<UrlShortener>,
        val ircProvider: IrcProvider
) {

    companion object {
        val log = LoggerFactory.getLogger(ForumListener::class.java)
    }

    private val forums = HashMap<URI, ForumHolder>()

    fun start() {
        executor.scheduleWithFixedDelay({
            try {
                pollAll()
            } catch (e: Exception) {
                log.error("Failed to poll forums", e)
            }
        }, 0, 1, TimeUnit.MINUTES)
    }

    @Synchronized
    private fun pollAll() {
        for (entry in config.forums.entries) {
            forums.computeIfAbsent(entry.key) { it -> ForumHolder(it, entry.value) }
                    .poll()
        }
    }

    private inner class ForumHolder(
            val uri: URI,
            val configuration: ForumConfiguration
    ) {

        private val sentThreadReplyCounts = HashMap<Int, Int>()
        private var firstPass = true

        fun poll() {
            assert(Thread.holdsLock(this@ForumListener))

            val threads = fetchThreads()
            for (thread in threads) {
                val oldReplyCount = sentThreadReplyCounts.put(thread.id, thread.replyCount)
                if (thread.replyCount != oldReplyCount && !firstPass) {
                    val message = configuration.messagePattern
                            .with("title", thread.title)
                            .with("author", thread.author)
                            .with("uri", thread.uri.toString())
                            .with("uri.short", { urlShortener.get().shorten(thread.uri).toString() })
                    log.info("Sending forum update '{}' to {} channels", message, configuration.channels.size)
                    message.sendTo(ircProvider.findChannels(configuration.channels))
                }
            }
            firstPass = false
        }

        private fun fetchThreads(): ArrayList<ThreadInfo> {
            val threads = ArrayList<ThreadInfo>()
            val document = Jsoup.connect(uri.toString()).get()
            for (element in document.select(".all_threads_container .thread_content")) {
                if (element.hasClass("thread_separator")) {
                    continue
                }

                val titleTag = element.select(".name > a").first()
                val replyCountText = element.select(".replycount").text().replace("[^\\d]".toRegex(), "")
                val href = titleTag.attr("href")
                threads.add(ThreadInfo(
                        Integer.parseInt(href.substring(href.indexOf('/') + 1, href.indexOf('-'))),
                        URI.create(titleTag.absUrl("href")),
                        titleTag.text(),
                        if (replyCountText.isEmpty()) 0 else Integer.parseInt(replyCountText),
                        element.select(".topicstart > a").text()))
            }
            return threads
        }
    }

    data class ThreadInfo(
            val id: Int,
            val uri: URI,
            val title: String,
            val replyCount: Int,
            val author: String
    )

    data class ForumConfiguration(
            val channels: List<String>,
            val messagePattern: at.yawk.katbot.template.Template
    )
}