/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package db.migration

import at.yawk.katbot.action.Karma
import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Connection
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.HashMap
import java.util.regex.Pattern

/**
 * @author yawkat
 */
private const val SKIP = "Add -DskipKarmaHistoryFromLogMigration=true to command line if you wish to skip this migration."

private val log = LoggerFactory.getLogger(V10__karma_history_from_log::class.java)

class V10__karma_history_from_log : JdbcMigration {
    override fun migrate(connection: Connection) {
        if (System.getProperty("skipKarmaHistoryFromLogMigration") == "true") {
            log.warn("Skipping migration of karma history from log.")
            return
        }

        val logPath = Paths.get("katbot.log")
        if (!Files.exists(logPath)) {
            throw Exception("Failed to find katbot.log for karma migration. $SKIP")
        }

        val statement = connection.prepareStatement("INSERT INTO karma_history (canonicalName, delta, actor, comment, timestamp) VALUES (?, ?, ?, ?, ?)")
        val pattern = Pattern.compile("""(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}) .* (.*?) has changed the karma level for (.*?)(?: by -?1| from -?\d+|) to (-?\d+)""")
        val timestampPattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        val zone = ZoneId.of("Europe/Paris")

        // analyze log

        val lastKarma = HashMap<String, Long>()
        var error = false
        // too lazy to close
        val reader = Files.newBufferedReader(logPath)
        var lineNumber = 0
        for (line in reader.lineSequence()) {
            lineNumber++
            val matcher = pattern.matcher(line)
            if (matcher.matches()) {
                val timestamp = LocalDateTime.parse(matcher.group(1), timestampPattern).atZone(zone).toInstant()
                val actor = matcher.group(2)
                val target = Karma.canonicalizeSubjectName(matcher.group(3))
                val newValue = matcher.group(4).toLong()

                val old = lastKarma[target] ?: 0L
                var delta = newValue - old

                // this happened a few times when karma was broken
                if (delta == 0L) {
                    continue
                }
                // I flipped katbots karma once :D
                if (target == "katbot" && newValue == 36L && old == -37L) {
                    statement.setString(1, target)
                    statement.setLong(2, 37L * 2)
                    statement.setString(3, null)
                    statement.setString(4, "DB meddling")
                    statement.setTimestamp(5, Timestamp.from(timestamp))
                    statement.addBatch()

                    delta = -1
                }

                if (delta != 1L && delta != -1L) {
                    log.error("$lineNumber: Karma jumped by |delta| != 1 (target: $target, actor: $actor, old: $old, new: $newValue)")
                    error = true
                }

                if (!error) {
                    statement.setString(1, target)
                    statement.setLong(2, delta)
                    statement.setString(3, actor)
                    statement.setString(4, null)
                    statement.setTimestamp(5, Timestamp.from(timestamp))
                    statement.addBatch()
                }

                lastKarma[target] = newValue
            }
        }

        if (!error) statement.executeBatch() else statement.close()

        if (!error) {
            // match values up with karma table
            lastKarma.forEach { target, value ->
                val st = connection.prepareStatement("SELECT karma FROM karma WHERE canonicalName = ?")
                st.setString(1, target)
                val result = st.executeQuery()
                if (!result.next()) {
                    log.error("Karma modification to $target (value: $value) found in log, but not in karma table.")
                    error = true
                } else {
                    val inKarmaTable = result.getLong("karma")
                    if (inKarmaTable != value) {
                        log.error("Karma value for $target inconsistent with karma table (log: $value, table: $inKarmaTable).")
                        error = true
                    }
                }
            }

            // confirm every karma value was in the log
            val allNamesFromTable = connection.prepareStatement("SELECT canonicalName, karma FROM karma").executeQuery()
            while (allNamesFromTable.next()) {
                val target = allNamesFromTable.getString("canonicalName")
                if (!lastKarma.containsKey(target)) {
                    val karma = allNamesFromTable.getLong("karma")
                    log.error("Karma value for $target present in table ($karma) but not in log.")
                    error = true
                }
            }
        }

        if (error) {
            throw Exception("Karma inconsistencies in katbot.log, aborting. $SKIP")
        }
    }
}