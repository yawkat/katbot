/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot

import com.google.common.base.Ascii
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
        val commandManager: CommandManager
) {
    companion object {
        private val canonicalChars = BitSet()

        init {
            for (c in '\u0000'..'\uffff') {
                if (c.isLetterOrDigit()) {
                    canonicalChars.set(c.toInt())
                }
            }
            canonicalChars.set(' '.toInt())
        }

        fun isCanonicalChar(char: Char) = canonicalChars.get(char.toInt())

        fun equalsCanonical(a: String, b: String): Boolean {
            var i = 0
            var j = 0
            while (true) {
                if (i < a.length && !isCanonicalChar(a[i])) {
                    i++
                    continue
                }
                if (j < b.length && !isCanonicalChar(b[j])) {
                    j++
                    continue
                }

                if (i >= a.length || j >= b.length) {
                    return i >= a.length && j >= b.length
                }

                if (Ascii.toLowerCase(a[i]) != Ascii.toLowerCase(b[j]) ||
                        !a[i].equals(b[j], ignoreCase = true)) {
                    return false // mismatch
                }
                i++
                j++
            }
        }
    }

    private val factoids = ArrayList<Entry>()

    @Synchronized
    private fun removeFactoid(entry: Entry, removeFromCollection: Boolean) {
        dataSource.connection.closed {
            val statement = it.prepareStatement("delete from factoids where canonicalName = ?")
            statement.setString(1, entry.name)
            statement.execute()
        }
        if (removeFromCollection) factoids.remove(entry)
    }

    @Synchronized
    private fun addFactoid(entry: Entry, insertIntoDb: Boolean) {
        val iterator = factoids.iterator()
        while (iterator.hasNext()) {
            val other = iterator.next()
            if (other.components.size == entry.components.size &&
                    equalsCanonical(other.name, entry.name)) {
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

        val match = findFactoid(line.parameters)
        if (match != null) {
            handleFactoid(event, match.first, match.second)
        }
    }

    private fun findFactoid(parameters: List<String>): Pair<Entry, Template>? {
        for (pass in PASSES) {
            for (factoid in factoids) {
                val match = factoid.match(parameters, pass)
                if (match != null) {
                    return Pair(factoid, match)
                }
            }
        }
        return null
    }

    private fun isTruthy(string: String): Boolean {
        return !string.isEmpty() && string != "0" && string != "false"
    }

    private fun handleFactoid(event: Command, factoid: Entry, match: Template) {
        val finalTemplate = finalizeTemplate(event, factoid, match)

        if (!commandManager.parseAndFire(
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

    private fun finalizeTemplate(event: Command, factoid: Entry, match: Template, depth: Int = 0): Template {
        // detect infinite loop
        if (event.hasCause { it.meta == factoid } || depth >= 16) {
            event.channel.sendMessage("Infinite loop in factoid ${factoid.name}")
            throw CancelEvent
        }

        val finalTemplate = match
                .set("sender", event.actor.nick)
                .setActorAndTarget(event)
                .withMissingFunction {
                    val list = evaluateFactoidSubExpression(event, it, depth)
                    if (list == null) null else CommandLine(list)
                }
        return finalTemplate
    }

    private fun evaluateFactoidSubExpression(parent: Command, subExpression: CommandLine, depth: Int): List<String>? {
        if (subExpression.startsWith("upper")) return subExpression.parameterRange(1).map { it.toUpperCase() }
        if (subExpression.startsWith("lower")) return subExpression.parameterRange(1).map { it.toLowerCase() }
        if (subExpression.startsWith("escape")) return subExpression.parameterRange(1).map { CommandLine.escape(it) }

        if (subExpression.startsWith("cat")) {
            return listOf(catDb.getImage(*subExpression.parameterRange(1).toTypedArray()).url)
        }
        if (subExpression.startsWith("random")) {
            return listOf(randomChoice(subExpression.parameterRange(1)))
        }
        if (subExpression.startsWith("if")) {
            if (subExpression.parameters.size > 2) {
                if (isTruthy(subExpression.parameters[1])) {
                    return listOf(subExpression.parameters[2])
                } else if (subExpression.parameters.size > 3) {
                    return subExpression.parameterRange(3)
                }
            }
            return listOf("")
        }
        if (subExpression.startsWith("equals")) {
            // check for only one unique value
            return listOf((subExpression.parameterRange(1).toSet().size <= 1).toString())
        }
        if (subExpression.startsWith("eval")) {
            val match = findFactoid(subExpression.parameterRange(1))
            if (match != null) {
                return listOf(finalizeTemplate(parent, match.first, match.second, depth + 1).finish())
            }
        }

        return null
    }
}

data class Entry(
        val name: String,
        val value: String
) {
    val components = CommandLine.parseParameters(name)
    private val varargs = components.last() == "$"
    var response = Template(value)

    fun match(parameters: List<String>, pass: Int): Template? {
        // fast paths
        if (parameters.size < components.size) return null
        if (parameters.size > components.size && (pass < 1 || !varargs)) return null

        var result = response
        var argumentIndex = 1
        for ((i, component) in components.withIndex()) {
            if (component == "$") {
                val argumentValue = if (i == components.size - 1) {
                    parameters.subList(i, parameters.size).joinToString(" ")
                } else {
                    parameters[i]
                }
                result = result.set(argumentIndex++.toString(), argumentValue)
            } else if (!Factoid.equalsCanonical(components[i], parameters[i])) {
                return null
            }
        }

        return result
    }
}