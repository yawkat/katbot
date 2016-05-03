/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot

import at.yawk.katbot.template.Parser
import java.util.*
import javax.inject.Inject
import javax.sql.DataSource

/**
 * @author yawkat
 */
class Interact @Inject constructor(
        val eventBus: EventBus,
        val config: Config,
        val dataSource: DataSource
) {
    private val interactions = HashMap<String, MutableList<Entry>>()

    fun start() {
        eventBus.subscribe(this)
        dataSource.connection.closed {
            val statement = it.createStatement()
            val query = statement.executeQuery("select * from interact")

            while (query.next()) {
                val category = query.getString("category")
                val valueTemplate = query.getString("valueTemplate")
                interactions.getOrPut(category) { ArrayList() }.add(Entry(valueTemplate))
            }
        }
    }

    @Subscribe
    fun command(event: Command) {
        val parameters = event.line.parameters
        if (parameters.size > 2 || parameters.size < 1) return
        val interactions = config.interactions[parameters[0].toLowerCase()] ?: return

        var target = parameters.getOrNull(1)
        if (target != null && (target.isEmpty() || !target.matches(SUBJECT_PATTERN.toRegex()))) return

        if (target == null || target == event.channel.client.nick) {
            target = event.actor.nick
        }
        Template(randomChoice(interactions))
                .setActorAndTarget(event.actor, target)
                .sendTo(event.channel)
        throw CancelEvent
    }

    class Entry(val value: String) {
        val expressions = Parser.parse(value)
    }
}