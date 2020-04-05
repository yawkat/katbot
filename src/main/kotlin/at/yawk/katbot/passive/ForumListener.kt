/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.passive

import at.yawk.katbot.Config
import at.yawk.katbot.IrcProvider
import at.yawk.katbot.UrlShortener
import at.yawk.katbot.template.Template
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.ArrayList
import java.util.HashMap
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
            } catch (e: Throwable) {
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
                    val newPost = oldReplyCount == null
                    val threadUri = if (newPost) thread.threadUri else thread.latestUri
                    val message = configuration.messagePattern
                            .with("title", thread.title)
                            .with("author", thread.author)
                            .with("uri", threadUri.toString())
                            .with("uri.short", { urlShortener.get().shorten(threadUri).toString() })
                    val targetChannels = if (newPost) {
                        configuration.channels.filter { it.showNewPosts }
                    } else {
                        configuration.channels.filter { it.showUpdates }
                    }
                    log.info("Sending forum update '{}' to {} channels", message, targetChannels.size)
                    message.sendTo(ircProvider.findChannels(targetChannels.map { it.name }))
                }
            }
            firstPass = false
        }

        private fun fetchThreads(): List<ThreadInfo> {
            val threads = ArrayList<ThreadInfo>()
            val document = Jsoup.connect(uri.toString()).get()
            for (element in document.select(".all_threads_container .thread_content")) {
                if (element.hasClass("thread_separator")) {
                    continue
                }

                val titleTag = element.select(".name > a").first()
                val replyCountText = element.select(".replycount").text().replace("[^\\d]".toRegex(), "")
                val href = titleTag.attr("href")
                val idEnd = href.indexOf('-')
                val lastPostTag = element.select(".lastpost a").single()
                threads.add(ThreadInfo(
                        id = Integer.parseInt(href.substring(href.indexOf('/') + 1, if (idEnd == -1) href.length else idEnd)),
                        threadUri = URI.create(titleTag.absUrl("href")),
                        latestUri = URI.create(lastPostTag.absUrl("href")),
                        title = titleTag.text(),
                        replyCount = if (replyCountText.isEmpty()) 0 else Integer.parseInt(replyCountText),
                        author = element.select(".topicstart > a").text()
                ))
            }
            return threads
        }
    }

    data class ThreadInfo(
            val id: Int,
            val threadUri: URI,
            val latestUri: URI,
            val title: String,
            val replyCount: Int,
            val author: String
    )

    data class ForumConfiguration(
            val channels: List<ForumChannelConfiguration>,
            val messagePattern: Template
    ) {
        data class ForumChannelConfiguration(
                val name: String,
                val showNewPosts: Boolean,
                val showUpdates: Boolean
        )
    }
}