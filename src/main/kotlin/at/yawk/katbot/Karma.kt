package at.yawk.katbot

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Clock
import java.util.regex.Pattern
import javax.inject.Inject

internal const val NAME_PATTERN = "([\\w\\-` öäü\\[\\]]*)"

/**
 * @author yawkat
 */
class Karma @Inject constructor(val ircProvider: IrcProvider, val objectMapper: ObjectMapper) {
    companion object {
        private val log = LoggerFactory.getLogger(Karma::class.java)

        private val MANIPULATE_PATTERN = Pattern.compile("~$NAME_PATTERN(\\+\\+|--)(:? .*)?", Pattern.CASE_INSENSITIVE)
        private val VIEW_PATTERN = Pattern.compile("~karma $NAME_PATTERN(:? .*)?", Pattern.CASE_INSENSITIVE)

        private val CLOCK = Clock.systemUTC()
    }

    private val karmaFilePath = Paths.get("karma.json")
    private var holder = emptyHolder()

    private val userThrottles: MutableMap<String, MessageThrottle> = hashMapOf()

    private fun emptyHolder() = Holder(hashMapOf())

    private fun canonicalizeSubjectName(subject: String): String {
        return subject.toLowerCase()
    }

    fun start() {
        loadKarma()
        ircProvider.registerEventListener(this)
    }

    private fun loadKarma() {
        if (Files.exists(karmaFilePath)) {
            Files.newInputStream(karmaFilePath).use { holder = objectMapper.readValue<Holder>(it) }
            // canonicalize keys
            holder = Holder(holder.karma.mapKeys { canonicalizeSubjectName(it.key) })
        } else {
            holder = emptyHolder()
        }
    }

    private fun saveKarma() {
        Files.newOutputStream(karmaFilePath).use { out -> objectMapper.writeValue(out, holder) }
    }

    @Subscribe
    @Synchronized
    fun onPublicMessage(event: ChannelMessageEvent) {
        val manipulateMatcher = MANIPULATE_PATTERN.matcher(event.message)
        if (manipulateMatcher.matches()) {
            val subject = manipulateMatcher.group(1).trim { it <= ' ' }
            if (!subject.isEmpty()) {
                val throttle = userThrottles.getOrPut(event.actor.nick, { MessageThrottle(CLOCK) })

                val canonicalizedSubject = canonicalizeSubjectName(subject)
                if (!throttle.trySend(canonicalizedSubject)) {
                    throw CancelEvent
                }

                val oldValue = holder.karma[canonicalizedSubject] ?: 0
                val newValue = if (manipulateMatcher.group(2) == "++") oldValue + 1 else oldValue - 1
                holder = Holder(holder.karma + Pair(canonicalizedSubject, newValue))

                log.info("{} has changed the karma level for {} from {} to {}",
                        event.actor.nick,
                        canonicalizedSubject,
                        oldValue,
                        newValue)
                event.channel.sendMessage(
                        subject + " has a karma level of " + newValue + ", " + event.actor.nick)

                saveKarma()
                throw CancelEvent
            }
        } else {
            val viewMatcher = VIEW_PATTERN.matcher(event.message)
            if (viewMatcher.matches()) {
                val subject = viewMatcher.group(1).trim { it <= ' ' }
                if (!subject.isEmpty()) {
                    val value = holder.karma[canonicalizeSubjectName(subject)] ?: 0
                    event.channel.sendMessage(
                            subject + " has a karma level of " + value + ", " + event.actor.nick)
                    throw CancelEvent
                }
            }
        }
    }
}

internal data class Holder(val karma: Map<String, Int>)