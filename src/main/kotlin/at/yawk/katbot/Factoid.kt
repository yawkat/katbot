/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot

import at.yawk.katbot.template.Canonical
import at.yawk.katbot.template.FactoidFunction
import at.yawk.katbot.template.SimpleVM
import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Ascii
import java.math.BigDecimal
import java.util.*
import javax.inject.Inject
import javax.sql.DataSource

val PASSES = 0..1

/**
 * @author yawkat
 */
class Factoid @Inject constructor(
        val eventBus: EventBus,
        val catDb: CatDb,
        val dataSource: DataSource,
        val roleManager: RoleManager,
        val commandBus: CommandBus
) {
    private val factoids = ArrayList<Entry>()
    private var vm = SimpleVM()

    @Synchronized
    private fun removeFactoid(entry: Entry, removeFromCollection: Boolean) {
        dataSource.connection.closed {
            val statement = it.prepareStatement("delete from factoids where canonicalName = ?")
            statement.setString(1, entry.name)
            statement.execute()
        }
        vm = vm.withoutFunction(entry.function)
        if (removeFromCollection) factoids.remove(entry)
    }

    @Synchronized
    private fun addFactoid(entry: Entry, insertIntoDb: Boolean) {
        val iterator = factoids.iterator()
        while (iterator.hasNext()) {
            val other = iterator.next()
            if (other.function.nameEquals(entry.function)) {
                removeFactoid(other, removeFromCollection = false)
                iterator.remove()
            }
        }
        if (insertIntoDb) {
            dataSource.connection.closed {
                val statement = it.prepareStatement("insert into factoids (canonicalName, value) values (?, ?)")
                statement.setString(1, entry.name)
                statement.setString(2, entry.value)
                statement.execute()
            }
        }
        vm = vm.withFunctions(entry.function)
        factoids.add(entry)
    }

    fun start() {
        dataSource.connection.closed {
            val statement = it.prepareStatement("select * from factoids")
            val result = statement.executeQuery()
            while (result.next()) {
                addFactoid(
                        Entry(result.getString("canonicalName"), result.getString("value")),
                        insertIntoDb = false
                )
            }
        }

        eventBus.subscribe(this)
    }

    @Synchronized
    @Subscribe(priority = 100) // low priority
    fun command(event: Command) {
        val line = event.line
        if (line.message.contains(" = ")) {
            if (!roleManager.hasRole(event.actor, Role.ADD_FACTOIDS)) {
                event.channel.sendMessage("${event.actor.nick}, you are not allowed to do that.")
                throw CancelEvent
            }

            val splitIndex = line.message.indexOf(" = ")
            val name = line.message.substring(0, splitIndex)
            val value = line.message.substring(splitIndex + " = ".length).trimEnd()
            addFactoid(Entry(name, value), insertIntoDb = true)
            event.channel.sendMessage("Factoid added.")
            throw CancelEvent
        }

        /* todo
        if (line.startsWith("raw")) {
            val match = findFactoid(line.parameterRange(1))
            if (match == null) {
                event.channel.sendMessage("No such factoid")
            } else {
                event.channel.sendMessage("~${match.first.name} = ${match.first.value}")
            }
            throw CancelEvent
        }

        if (line.startsWith("delete")) {
            if (!roleManager.hasRole(event.actor, Role.DELETE_FACTOIDS)) {
                event.channel.sendMessage("You are not allowed to do that")
                throw CancelEvent
            }
            val match = findFactoid(line.parameterRange(1))
            if (match == null) {
                event.channel.sendMessage("No such factoid")
            } else {
                removeFactoid(match.first, removeFromCollection = true)
                event.channel.sendMessage("Factoid deleted")
            }
            throw CancelEvent
        }
        */

        val match = findFactoid(line.parameters)
        if (match != null) {
            handleFactoid(event, match.first, match.second)
        }
    }

    private fun handleFactoid(event: Command) {
        // detect infinite loop
        if (event.hasCause { it.meta == factoid }) {
            event.channel.sendMessage("Infinite loop in factoid ${factoid.name}")
            throw CancelEvent
        }
        val finalTemplate = finalizeTemplate(event, match)

        if (!commandBus.parseAndFire(
                event.actor,
                event.channel,
                finalTemplate.finish(),
                event.public,
                false,
                event.userLocator,
                Cause(event, factoid)
        )) {
            finalTemplate.sendTo(event.channel)
        }
        throw CancelEvent
    }

    private fun finalizeTemplate(event: Command, match: Template, depth: Int = 0): Template {
        val finalTemplate = match
                .set("sender", event.actor.nick)
                .setActorAndTarget(event)
                .withMissingFunction {
                    val list = evaluateFactoidSubExpression(event, it, depth)
                    if (list == null) null else CommandLine(list)
                }
        return finalTemplate
    }
}

data class Entry(
        val name: String,
        val value: String
) {
    val function = FactoidFunction(name, value)
}