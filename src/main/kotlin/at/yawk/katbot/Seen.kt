package at.yawk.katbot

import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import javax.inject.Inject
import javax.sql.DataSource

/**
 * @author yawkat
 */
class Seen @Inject constructor(val eventBus: EventBus, val dataSource: DataSource) {
    companion object {
        private val PATTERN = "seen $NICK_PATTERN".toPattern(Pattern.CASE_INSENSITIVE)
    }

    fun start() {
        eventBus.subscribe(this)
    }

    @Subscribe
    fun command(command: Command) {
        val matcher = PATTERN.matcher(command.message)
        if (matcher.matches()) {
            val nick = matcher.group(1)
            val seen = dataSource.connection.closed {
                val statement = it.prepareStatement("select seen from seen where nick=?")
                statement.setString(1, nick)
                val result = statement.executeQuery()
                if (result.next()) result.getTimestamp("seen").toInstant() else null
            }
            if (seen == null) {
                command.channel.sendMessage("I have never seen $nick")
            } else {
                val datetime = LocalDateTime.ofInstant(seen, ZoneId.of("Europe/Berlin"))
                command.channel.sendMessage("I have last seen $nick on ${datetime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}")
            }
            throw CancelEvent
        }
    }

    @Subscribe(priority = 1000)
    fun onPublicMessage(event: ChannelMessageEvent) {
        dataSource.connection.closed {
            val statement = it.prepareStatement("merge into seen (nick, seen) values (?, ?)")
            statement.setString(1, event.actor.nick)
            statement.setTimestamp(2, Timestamp.from(Instant.now()))
            statement.execute()
        }
    }
}