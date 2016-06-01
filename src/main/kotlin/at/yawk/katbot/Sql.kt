/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot

import at.yawk.paste.client.PasteClient
import at.yawk.paste.model.TextPasteData
import java.sql.SQLException
import java.util.*
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
        val roleManager: RoleManager,
        val pasteClient: PasteClient
) {
    fun start() {
        eventBus.subscribe(this)
    }

    @Subscribe
    fun command(command: Command) {
        if (command.line.message.startsWith("sql ")) {
            if (!roleManager.hasRole(command.actor, Role.ADMIN)) {
                command.channel.sendMessageSafe("You are not allowed to do that.")
                throw CancelEvent
            }
            val results = ArrayList<String>()
            try {
                dataSource.connection.closed {
                    val statement = it.createStatement()
                    val query = statement.executeQuery(command.line.message.substring("sql ".length))

                    while (query.next()) {
                        var row = "| "
                        for (i in 1..query.metaData.columnCount) {
                            row += query.getObject(i)
                            row += " | "
                        }
                        results.add(row)
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
                val data = TextPasteData()
                data.text = results.joinToString("\n")
                val uri = pasteClient.save(data)
                command.channel.sendMessageSafe(uri)
            }
            throw CancelEvent
        }
    }
}