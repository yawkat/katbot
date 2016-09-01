/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.action

import at.yawk.katbot.CancelEvent
import at.yawk.katbot.EventBus
import at.yawk.katbot.MessageThrottle
import at.yawk.katbot.Subscribe
import at.yawk.katbot.command.Command
import at.yawk.katbot.security.PermissionName
import at.yawk.katbot.sendMessageSafe
import at.yawk.katbot.web.WebProvider
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.skife.jdbi.v2.DBI
import org.skife.jdbi.v2.StatementContext
import org.skife.jdbi.v2.sqlobject.Bind
import org.skife.jdbi.v2.sqlobject.SqlQuery
import org.skife.jdbi.v2.sqlobject.SqlUpdate
import org.skife.jdbi.v2.sqlobject.customizers.Mapper
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper
import org.skife.jdbi.v2.tweak.ResultSetMapper
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.ResultSet
import java.time.Clock
import java.time.Instant
import java.util.HashMap
import java.util.regex.Pattern
import javax.inject.Inject
import javax.ws.rs.DefaultValue
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.QueryParam

internal const val SUBJECT_PATTERN = "([\\w\\-` öäü\\[\\]]*)"
internal const val NICK_PATTERN = "([\\w\\-`öäü\\[\\]]+)"

/**
 * @author yawkat
 */
@Path("/karma/")
class Karma @Inject constructor(
        val eventBus: EventBus,
        val objectMapper: ObjectMapper,
        val web: WebProvider,
        val dbi: DBI
) {
    companion object {
        private val log = LoggerFactory.getLogger(Karma::class.java)

        private val MANIPULATE_PATTERN = Pattern.compile("$SUBJECT_PATTERN(\\+\\+|--)(:? .*)?", Pattern.CASE_INSENSITIVE)
        private val VIEW_PATTERN = Pattern.compile("karma(p?) $SUBJECT_PATTERN", Pattern.CASE_INSENSITIVE)

        private val CLOCK = Clock.systemUTC()

        fun canonicalizeSubjectName(subject: String): String {
            return subject.toLowerCase()
        }
    }

    private val dao = dbi.onDemand(KarmaDao::class.java)
    private val userThrottles: MutableMap<String, MessageThrottle> = hashMapOf()

    fun start() {
        // karma.json migration
        val karmaFilePath = Paths.get("karma.json")
        if (Files.exists(karmaFilePath)) {
            val node = Files.newInputStream(karmaFilePath).use { objectMapper.readTree(it) }
            val karma = node.get("karma") as ObjectNode
            karma.fields().forEach { entry ->
                addKarma(
                        canonicalizeSubjectName(entry.key),
                        entry.value.longValue(),
                        actor = null,
                        comment = "Imported from karma.json",
                        requireInsert = true
                )
            }
            Files.move(karmaFilePath, Paths.get("karma.old.json"))
        }

        eventBus.subscribe(this)
        web.addResource(this)
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
                        && !event.isPermitted(PermissionName.IGNORE_THROTTLE)) {
                    throw CancelEvent
                }

                if (!event.public) {
                    event.channel.sendMessageSafe("Not here, sorry.")
                    throw CancelEvent
                }

                val delta = if (manipulateMatcher.group(2) == "++") 1L else -1L
                addKarma(canonicalizedSubject, delta, actor = event.actor.nick + '@' + event.actor.host)
                val newKarma = dao.getKarma(canonicalizedSubject)

                log.info("{} has changed the karma level for {} to {}",
                        event.actor.nick,
                        canonicalizedSubject,
                        newKarma)
                event.channel.sendMessageSafe("$subject has a karma level of $newKarma, ${event.actor.nick}")
                throw CancelEvent
            }
        } else {
            val viewMatcher = VIEW_PATTERN.matcher(event.line.message)
            if (viewMatcher.matches()) {
                val primes = viewMatcher.group(1) == "p"
                val subject = viewMatcher.group(2).trim { it <= ' ' }
                if (!subject.isEmpty()) {
                    val value = getKarma(canonicalizeSubjectName(subject))
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
                    event.channel.sendMessageSafe("$subject has a karma level of $valueText, ${event.actor.nick}")
                    throw CancelEvent
                }
            }
        }
    }

    @GET
    @Path("/")
    fun searchKarma(@QueryParam("search") search: String?, @QueryParam("page") @DefaultValue("0") page: Int): List<Entry> =
            if (search.isNullOrBlank()) dao.searchKarma(page * 50)
            else dao.searchKarma("%${canonicalizeSubjectName(search!!)}%", page * 50)

    @GET
    @Path("/{name}")
    fun getKarmaForName(@PathParam("name") name: String): Entry {
        val canonicalizedSubjectName = canonicalizeSubjectName(name)
        return Entry(
                canonicalizedSubjectName,
                getKarma(canonicalizedSubjectName)
        )
    }

    @GET
    @Path("/{name}/history")
    fun getHistoryForName(@PathParam("name") name: String): List<HistoryEntry> =
            dao.getKarmaLog(canonicalizeSubjectName(name))

    private fun getKarma(canonicalizedSubject: String): Long =
            dao.getKarma(canonicalizedSubject) ?: 0L

    private fun addKarma(canonicalizedSubject: String, delta: Long, actor: String?, comment: String? = null, requireInsert: Boolean = false) {
        dbi.inTransaction { handle, tx ->
            val txDao = handle.attach(KarmaDao::class.java)
            if (requireInsert || txDao.tryAddKarma(canonicalizedSubject, delta) == 0) {
                txDao.tryCreateKarma(canonicalizedSubject, delta)
            }
            txDao.logKarma(canonicalizedSubject, delta, actor, comment)
        }
    }
}

@RegisterMapper(KarmaDao.EntryMapper::class)
private interface KarmaDao {
    @SqlQuery("select karma from karma where canonicalName = :canonicalName")
    fun getKarma(@Bind("canonicalName") canonicalName: String): Long?

    @SqlUpdate("update karma set karma = karma + :delta where canonicalName = :canonicalName")
    fun tryAddKarma(@Bind("canonicalName") canonicalName: String, @Bind("delta") delta: Long): Int

    @SqlUpdate("insert into karma (canonicalName, karma) values (:canonicalName, :karma)")
    fun tryCreateKarma(@Bind("canonicalName") canonicalName: String, @Bind("karma") karma: Long)

    @SqlUpdate("INSERT INTO karma_history (canonicalName, delta, actor, comment) VALUES (:canonicalName, :delta, :actor, :comment)")
    fun logKarma(@Bind("canonicalName") canonicalName: String, @Bind("delta") delta: Long, @Bind("actor") actor: String?, @Bind("comment") comment: String?)

    @Mapper(HistoryEntryMapper::class)
    @SqlQuery("SELECT timestamp, delta, actor, comment FROM karma_history WHERE canonicalName = :canonicalName")
    fun getKarmaLog(@Bind("canonicalName") canonicalName: String): List<HistoryEntry>

    @SqlQuery("select canonicalName, karma from karma where karma <> 0 order by karma desc limit 50 offset :offset")
    fun searchKarma(@Bind("offset") offset: Int): List<Entry>

    @SqlQuery("select canonicalName, karma from karma where canonicalName like :search and karma <> 0 order by karma desc limit 50 offset :offset")
    fun searchKarma(@Bind("search") search: String, @Bind("offset") offset: Int): List<Entry>

    class EntryMapper : ResultSetMapper<Entry> {
        override fun map(index: Int, r: ResultSet, ctx: StatementContext) =
                Entry(r.getString("canonicalName"), r.getLong("karma"))
    }

    class HistoryEntryMapper : ResultSetMapper<HistoryEntry> {
        override fun map(index: Int, r: ResultSet, ctx: StatementContext) =
                HistoryEntry(r.getTimestamp("timestamp").toInstant(), r.getLong("delta"), r.getString("actor"), r.getString("comment"))
    }
}

data class Entry(val canonicalName: String, val karma: Long)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class HistoryEntry(
        @JsonFormat(shape = JsonFormat.Shape.STRING) val timestamp: Instant,
        val delta: Long,
        val actor: String?,
        val comment: String?
)

inline fun <C : AutoCloseable, T> C.closed(arg: (C) -> T): T {
    try {
        return arg.invoke(this)
    } finally {
        this.close()
    }
}
