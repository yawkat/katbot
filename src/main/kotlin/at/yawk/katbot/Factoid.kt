package at.yawk.katbot

import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent
import org.kitteh.irc.lib.net.engio.mbassy.listener.Handler
import javax.inject.Inject

/**
 * @author yawkat
 */
class Factoid @Inject constructor(val ircProvider: IrcProvider, val config: Config, val catDb: CatDb) {
    fun start() {
        ircProvider.registerEventListener(this)
    }

    private fun getFactoids(): Map<String, () -> String> {
        // wrap in function
        return config.factoids.mapValues { { it.value } } + mapOf(
                Pair("morning", { catDb.getImage("yawn").url })
        )
    }

    @Handler
    fun onPublicMessage(event: ChannelMessageEvent) {
        for (factoid in getFactoids()) {
            if (event.message.trimEnd().toLowerCase() == "~${factoid.key}") {
                event.channel.sendMessage(
                        Template(factoid.value.invoke())
                                .set("sender", event.actor.nick)
                                .finish()
                )
                break
            }
        }
    }
}