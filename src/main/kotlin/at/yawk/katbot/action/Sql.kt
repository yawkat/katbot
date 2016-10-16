/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.action

import at.yawk.katbot.CancelEvent
import at.yawk.katbot.EventBus
import at.yawk.katbot.Subscribe
import at.yawk.katbot.command.Command
import at.yawk.katbot.paste.Paste
import at.yawk.katbot.paste.PasteProvider
import at.yawk.katbot.security.PermissionName
import at.yawk.katbot.sendMessageSafe
import java.sql.ResultSet
import java.sql.SQLException
import java.util.ArrayList
import javax.inject.Inject
import javax.inject.Singleton
import javax.sql.DataSource

/**
 * @author yawkat
 */
@Singleton
class Sql @Inject constructor(
        val eventBus: EventBus,
        val dataSource: DataSource,
        val pasteClient: PasteProvider
) {
    fun start() {
        eventBus.subscribe(this)
    }

    @Subscribe
    fun command(command: Command) {
        if (command.line.message.startsWith("sql ")) {
            command.checkPermission(PermissionName.ADMIN)
            val results = ArrayList<String>()
            try {
                dataSource.connection.closed {
                    val statement = it.createStatement()
                    statement.execute(command.line.message.substring("sql ".length))
                    val query: ResultSet? = statement.resultSet

                    if (query == null) {
                        results.add("done.")
                    } else {
                        while (query.next()) {
                            var row = "| "
                            for (i in 1..query.metaData.columnCount) {
                                row += query.getObject(i)
                                row += " | "
                            }
                            results.add(row)
                        }
                    }
                }
            } catch (e: SQLException) {
                results.addAll(e.toString().split('\n'))
            }
            if (results.isEmpty()) {
                command.channel.sendMessageSafe("No results.")
            } else if (results.size <= 2) {
                results.forEach { command.channel.sendMessageSafe(it) }
            } else {
                val uri = pasteClient.createPaste(Paste(Paste.Type.TEXT, results.joinToString("\n")))
                command.channel.sendMessageSafe(uri.toASCIIString())
            }
            throw CancelEvent
        }
    }
}