/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Connection
import java.time.Clock
import java.util.*
import java.util.regex.Pattern
import javax.inject.Inject
import javax.sql.DataSource

internal const val SUBJECT_PATTERN = "([\\w\\-` öäü\\[\\]]*)"
internal const val NICK_PATTERN = "([\\w\\-`öäü\\[\\]]+)"

/**
 * @author yawkat
 */
class Karma @Inject constructor(
        val eventBus: EventBus,
        val objectMapper: ObjectMapper,
        val dataSource: DataSource,
        val roleManager: RoleManager
) {
    companion object {
        private val log = LoggerFactory.getLogger(Karma::class.java)

        private val MANIPULATE_PATTERN = Pattern.compile("$SUBJECT_PATTERN(\\+\\+|--)(:? .*)?", Pattern.CASE_INSENSITIVE)
        private val VIEW_PATTERN = Pattern.compile("karma(p?) $SUBJECT_PATTERN", Pattern.CASE_INSENSITIVE)

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

        eventBus.subscribe(this)
    }

    @Subscribe
    fun command(event: Command) {
        val manipulateMatcher = MANIPULATE_PATTERN.matcher(event.line.message)
        if (manipulateMatcher.matches()) {
            val subject = manipulateMatcher.group(1).trim { it <= ' ' }
            if (!subject.isEmpty()) {
                val throttle = userThrottles.getOrPut(event.actor.nick, { MessageThrottle(CLOCK) })

                val canonicalizedSubject = canonicalizeSubjectName(subject)
                if (!throttle.trySend(canonicalizedSubject)
                        && !roleManager.hasRole(event.actor, Role.IGNORE_THROTTLE)) {
                    throw CancelEvent
                }

                if (!event.public) {
                    event.channel.sendMessage("Not here, sorry.")
                    throw CancelEvent
                }

                val newKarma = dataSource.connection.closed {
                    val update = it.prepareStatement("update karma set karma = karma + ? where canonicalName = ?")
                    update.setInt(1, if (manipulateMatcher.group(2) == "++") 1 else -1)
                    update.setString(2, canonicalizedSubject)
                    if (update.executeUpdate() == 0) {
                        val insert = it.prepareStatement("insert into karma (canonicalName, karma) values (?, ?)")
                        insert.setString(1, canonicalizedSubject)
                        insert.setInt(2, if (manipulateMatcher.group(2) == "++") 1 else -1)
                        insert.execute()
                    }

                    getKarma(it, canonicalizedSubject)
                }

                log.info("{} has changed the karma level for {} to {}",
                        event.actor.nick,
                        canonicalizedSubject,
                        newKarma)
                event.channel.sendMessage("$subject has a karma level of $newKarma, ${event.actor.nick}")
                throw CancelEvent
            }
        } else {
            val viewMatcher = VIEW_PATTERN.matcher(event.line.message)
            if (viewMatcher.matches()) {
                val primes = viewMatcher.group(1) == "p"
                val subject = viewMatcher.group(2).trim { it <= ' ' }
                if (!subject.isEmpty()) {
                    val value = dataSource.connection.closed { getKarma(it, canonicalizeSubjectName(subject)) }
                    val valueText = if (primes && Math.abs(value) > 1) {
                        var remainder = Math.abs(value)
                        val counts = HashMap<Long, Int>()
                        while (remainder > 1) {
                            for (i in 2..remainder) {
                                if (remainder % i == 0L) {
                                    counts.compute(i, { k, old -> (old ?: 0) + 1 })
                                    remainder /= i
                                    break
                                }
                            }
                        }
                        val string = counts.map { if (it.value <= 1) it.key.toString() else "${it.key}^${it.value}" }
                                .joinToString(" * ")
                        if (value < 0) "-$string" else string
                    } else {
                        value.toString()
                    }
                    event.channel.sendMessage("$subject has a karma level of $valueText, ${event.actor.nick}")
                    throw CancelEvent
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
