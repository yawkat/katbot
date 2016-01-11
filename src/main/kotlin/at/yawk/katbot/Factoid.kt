package at.yawk.katbot

import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent
import javax.inject.Inject
import javax.sql.DataSource

/**
 * @author yawkat
 */
class Factoid @Inject constructor(val ircProvider: IrcProvider, val config: Config, val catDb: CatDb, val dataSource: DataSource) {
    fun start() {
        ircProvider.registerEventListener(this)
    }

    private fun canonicalizeFactoidName(name: String): String {
        // only keep word characters
        return name.replace("[^\\w]".toRegex(), "")
    }

    @Subscribe(priority = 100) // low priority
    fun onPublicMessage(event: ChannelMessageEvent) {
        if (event.message.startsWith("~")) {
            if (event.message.contains(" is ")) {
                if (event.actor.nick != "yawkat") {
                    // todo
                    event.channel.sendMessage("${event.actor.nick}, you are not allowed to do that.")
                    throw CancelEvent
                }

                val splitIndex = event.message.indexOf(" is ")
                val canonical = canonicalizeFactoidName(event.message.substring(1, splitIndex))
                val value = event.message.substring(splitIndex + 4).trimEnd()
                dataSource.connection.closed {
                    val statement = it.prepareStatement("merge into factoids (canonicalName, value) values (?, ?)")
                    statement.setString(1, canonical)
                    statement.setString(2, value)
                    statement.execute()
                }
                event.channel.sendMessage("Factoid added.")
                throw CancelEvent
            }

            val canonical = canonicalizeFactoidName(event.message.substring(1))
            if (canonical.isNotEmpty()) {
                val value = dataSource.connection.closed {
                    val statement = it.prepareStatement("select value from factoids where canonicalName = ?")
                    statement.setString(1, canonical)
                    val result = statement.executeQuery()
                    if (result.next()) result.getString("value") else null
                }
                if (value != null) {
                    Template(value)
                            .set("sender", event.actor.nick)
                            .set("cat", { catDb.getImage().url })
                            .setWithParameter("cat", { tags ->
                                catDb.getImage(*tags.split("|").toTypedArray()).url
                            })
                            .sendTo(event.channel)
                    throw CancelEvent
                }
            }
        }
    }
}