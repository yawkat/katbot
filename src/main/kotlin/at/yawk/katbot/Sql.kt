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
        if (command.message.startsWith("sql ")) {
            if (!roleManager.hasRole(command.actor, Role.ADMIN)) {
                command.channel.sendMessage("You are not allowed to do that.")
                throw CancelEvent
            }
            val results = ArrayList<String>()
            try {
                dataSource.connection.closed {
                    val statement = it.createStatement()
                    val query = statement.executeQuery(command.message.substring("sql ".length))

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
                command.channel.sendMessage(e.toString())
                return
            }
            if (results.isEmpty()) {
                command.channel.sendMessage("No results.")
            } else if (results.size <= 2) {
                results.forEach { command.channel.sendMessage(it) }
            } else {
                val data = TextPasteData()
                data.text = results.joinToString("\n")
                val uri = pasteClient.save(data)
                command.channel.sendMessage(uri)
            }
            throw CancelEvent
        }
    }
}