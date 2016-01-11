package at.yawk.katbot

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Connection
import java.time.Clock
import java.util.regex.Pattern
import javax.inject.Inject
import javax.sql.DataSource

internal const val NAME_PATTERN = "([\\w\\-` öäü\\[\\]]*)"

/**
 * @author yawkat
 */
class Karma @Inject constructor(val ircProvider: IrcProvider, val objectMapper: ObjectMapper, val dataSource: DataSource) {
    companion object {
        private val log = LoggerFactory.getLogger(Karma::class.java)

        private val MANIPULATE_PATTERN = Pattern.compile("~$NAME_PATTERN(\\+\\+|--)(:? .*)?", Pattern.CASE_INSENSITIVE)
        private val VIEW_PATTERN = Pattern.compile("~karma $NAME_PATTERN(:? .*)?", Pattern.CASE_INSENSITIVE)

        private val CLOCK = Clock.systemUTC()
    }

    private val userThrottles: MutableMap<String, MessageThrottle> = hashMapOf()

    private fun canonicalizeSubjectName(subject: String): String {
        return subject.toLowerCase()
    }

    fun start() {
        // karma.json migration
        val karmaFilePath = Paths.get("karma.json")
        if (Files.exists(karmaFilePath)) {
            val node = Files.newInputStream(karmaFilePath).use { objectMapper.readTree(it) }
            dataSource.connection.closed {
                val karma = node.get("karma") as ObjectNode
                karma.fields().forEach { entry ->
                    val statement = it.prepareStatement("insert into karma (canonicalName, karma) values (?, ?)")
                    statement.setString(1, canonicalizeSubjectName(entry.key))
                    statement.setLong(2, entry.value.longValue())
                    statement.execute()
                }
            }
            Files.move(karmaFilePath, Paths.get("karma.old.json"))
        }

        ircProvider.registerEventListener(this)
    }

    @Subscribe
    fun onPublicMessage(event: ChannelMessageEvent) {
        val manipulateMatcher = MANIPULATE_PATTERN.matcher(event.message)
        if (manipulateMatcher.matches()) {
            val subject = manipulateMatcher.group(1).trim { it <= ' ' }
            if (!subject.isEmpty()) {
                val throttle = userThrottles.getOrPut(event.actor.nick, { MessageThrottle(CLOCK) })

                val canonicalizedSubject = canonicalizeSubjectName(subject)
                if (!throttle.trySend(canonicalizedSubject)) {
                    return
                }

                val newKarma = dataSource.connection.closed {
                    val update = it.prepareStatement("update karma set karma = karma + ? where canonicalName = ?")
                    update.setInt(1, if (manipulateMatcher.group(2) == "++") 1 else -1)
                    update.setString(2, canonicalizedSubject)
                    update.execute()

                    getKarma(it, canonicalizedSubject)
                }

                log.info("{} has changed the karma level for {} to {}",
                        event.actor.nick,
                        canonicalizedSubject,
                        newKarma)
                event.channel.sendMessage("$subject has a karma level of $newKarma, ${event.actor.nick}")
            }
        } else {
            val viewMatcher = VIEW_PATTERN.matcher(event.message)
            if (viewMatcher.matches()) {
                val subject = viewMatcher.group(1).trim { it <= ' ' }
                if (!subject.isEmpty()) {
                    val value = dataSource.connection.closed { getKarma(it, canonicalizeSubjectName(subject)) }
                    event.channel.sendMessage(
                            subject + " has a karma level of " + value + ", " + event.actor.nick)
                }
            }
        }
    }

    private fun getKarma(connection: Connection, canonicalizedSubject: String): Long {
        val select = connection.prepareStatement("select karma from karma where canonicalName = ?")
        select.setString(1, canonicalizedSubject)
        val result = select.executeQuery()
        if (!result.next()) return 0 // no entry
        return result.getLong("karma")
    }
}

inline fun <C : AutoCloseable, T> C.closed(arg: (C) -> T): T {
    try {
        return arg.invoke(this)
    } finally {
        this.close()
    }
}
