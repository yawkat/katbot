/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot

import at.yawk.katbot.template.ConstantFunction
import at.yawk.katbot.template.Parser
import at.yawk.katbot.template.SimpleVM
import java.util.*
import javax.inject.Inject
import javax.sql.DataSource

/**
 * @author yawkat
 */
class Interact @Inject constructor(
        val eventBus: EventBus,
        val config: Config,
        val dataSource: DataSource,
        val roleManager: RoleManager
) {
    private val interactions = HashMap<String, MutableList<Entry>>()
    private val baseVm = SimpleVM()

    @Synchronized
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
    @Synchronized
    fun command(event: Command) {
        val parameters = event.line.parameters
        if (parameters.size < 1) return
        val category = parameters[0].toLowerCase()
        if (category !in config.interactions) return
        val interactions = interactions.getOrPut(category) { ArrayList() }

        var target = parameters.getOrNull(1)

        if ((target == "+=" || target == "-=") && parameters.size > 2) {
            if (!roleManager.hasRole(event.actor, Role.EDIT_INTERACT)) {
                event.channel.sendMessage("You aren't allowed to do that")
                return
            }
            val value = event.line.tailParameterString(2)
            if (target == "-=") {
                if (interactions.find { it.value == value } == null) {
                    event.channel.sendMessage("No such interaction")
                    return
                }

                dataSource.connection.closed {
                    val statement = it.prepareStatement("delete from interact where category = ? and valueTemplate = ?")
                    statement.setString(1, category)
                    statement.setString(2, value)
                    statement.executeUpdate()
                }
                interactions.removeAll { it.value == value }
                event.channel.sendMessage("Interaction removed")
            } else if (target == "+=") {
                if (interactions.find { it.value == value } != null) {
                    event.channel.sendMessage("Interaction already present")
                    return
                }

                dataSource.connection.closed {
                    val statement = it.prepareStatement("insert into interact (category, valueTemplate) values (?, ?)")
                    statement.setString(1, category)
                    statement.setString(2, value)
                    statement.executeUpdate()
                }
                interactions.add(Entry(value))
                event.channel.sendMessage("Interaction added")
            }
            throw CancelEvent
        }

        if (parameters.size > 2 || interactions.isEmpty()) return

        if (target != null && (target.isEmpty() || !target.matches(SUBJECT_PATTERN.toRegex()))) return

        if (target == null || target == event.channel.client.nick) {
            target = event.actor.nick
        }
        val vm = baseVm.plusFunctions(listOf(
                ConstantFunction("target", target),
                ConstantFunction("actor", event.actor.nick)
        ))
        val result = randomChoice(interactions).expressions
                .flatMap { it.computeValue(vm) }
                .joinToString(" ")
        Factoid.sendTemplateResultToChannel(event.channel, result)
        throw CancelEvent
    }

    class Entry(val value: String) {
        val expressions = Parser.parse(value)
    }
}