package at.yawk.katbot.nina

import at.yawk.katbot.Config
import at.yawk.katbot.IrcProvider
import at.yawk.katbot.paste.Paste
import at.yawk.katbot.paste.PasteProvider
import at.yawk.katbot.sendMessageSafe
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private val log = LoggerFactory.getLogger(NinaListener::class.java)

class NinaListener @Inject constructor(
        private val pasteProvider: PasteProvider,
        private val config: Config,
        private val executor: ScheduledExecutorService,
        private val ircProvider: IrcProvider
) {
    private var seenWarningsByChannel: MutableMap<String, Set<String>> = mutableMapOf()

    private fun poll() {
        val warnings = Nina.fetchWarnings()
        val now = Instant.now()
        for (channel in config.nina) {
            val point = channel.point
            val relevant = warnings
                    .filter { ann -> ann.bestInfo.area.any { area -> area.polygon.any { poly -> poly.contains(point) } } }
                    .filter { it.bestInfo.expires == null || it.bestInfo.expires.toInstant().isAfter(now) }
            val seen = seenWarningsByChannel[channel.channel]
            if (seen != null) {
                for (announcement in relevant) {
                    if (announcement.identifier !in seen) {
                        val uri = pasteProvider.createPaste(Paste(Paste.Type.TEXT, announcement.toText()))
                        ircProvider.findChannels(listOf(channel.channel)).forEach {
                            it.sendMessageSafe("[NINA] ${announcement.bestInfo.event}/${announcement.bestInfo.urgency}/${announcement.bestInfo.severity}: ${announcement.bestInfo.headline} $uri")
                        }
                    }
                }
            }
            seenWarningsByChannel[channel.channel] = relevant.map { it.identifier }.toSet()
        }
    }

    fun start() {
        executor.scheduleWithFixedDelay({
            try {
                poll()
            } catch (e: Throwable) {
                log.error("Failed to poll NINA warnings", e)
            }
        }, 0, 1, TimeUnit.MINUTES)
    }
}