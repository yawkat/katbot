/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot

import at.yawk.katbot.template.*
import at.yawk.katbot.template.Function
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import javax.inject.Inject
import javax.sql.DataSource

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
    private var vm = SimpleVM().plusFunctions(listOf(
            Functions.If,
            Functions.Sum,
            Functions.Product,
            Functions.Equal,
            Functions.NumberCompare,
            RandomFunction
    ))

    @Synchronized
    private fun removeFactoid(entry: Entry, removeFromCollection: Boolean) {
        dataSource.connection.closed {
            val statement = it.prepareStatement("delete from factoids where canonicalName = ?")
            statement.setString(1, entry.name)
            statement.execute()
        }
        vm = vm.minusFunction(entry)
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
        vm = vm.plusFunctionTail(entry.function, entry)
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

        val targetVm = vm.plusFunctions(listOf(
                ConstantFunction("target", (event.target ?: event.actor).nick),
                ConstantFunction("actor", event.actor.nick),
                CachedCatFunction()
        )).withInterceptor(object : SimpleVM.InvocationInterceptor {
            var invocationCount = 0

            override fun evaluate(functionList: FunctionList, parameters: LazyExpressionList): FunctionList.Result? {
                if (invocationCount > 1000) return null
                invocationCount++
                return SimpleVM.InvocationInterceptor.Default.evaluate(functionList, parameters)
            }
        })

        fun findFactoidForRawAndDelete(): Entry? {
            val expressionList = LazyExpressionList(targetVm, line.parameterRange(1).map { Expression.Literal(it) })
            for (mode in Function.EvaluationMode.values()) {
                val found = factoids.firstOrNull { it.function.canEvaluate(expressionList, mode) }
                if (found != null) return found
            }
            return null
        }

        if (line.startsWith("raw")) {
            val match = findFactoidForRawAndDelete()
            if (match == null) {
                event.channel.sendMessage("No such factoid")
            } else {
                event.channel.sendMessage("~${match.name} = ${match.value}")
            }
            throw CancelEvent
        }

        if (line.startsWith("delete")) {
            if (!roleManager.hasRole(event.actor, Role.DELETE_FACTOIDS)) {
                event.channel.sendMessage("You are not allowed to do that")
                throw CancelEvent
            }
            val match = findFactoidForRawAndDelete()
            if (match == null) {
                event.channel.sendMessage("No such factoid")
            } else {
                removeFactoid(match, removeFromCollection = true)
                event.channel.sendMessage("Factoid deleted")
            }
            throw CancelEvent
        }

        val result = targetVm.invokeWithMark(event.line.parameters.map { Expression.Literal(it) })
        if (result != null) {
            // the DEF_FUNCTION_MARK is used for functions which have no mark because they are not factoids, for
            // example 'if'. This mark is used for loop detection.
            val mark = result.mark ?: "DEF_FUNCTION_MARK"
            // detect infinite loop
            if (event.hasCause { it.meta == result.mark }) {
                if (mark is Entry) {
                    event.channel.sendMessage("Infinite loop in factoid ${mark.name}")
                } else {
                    event.channel.sendMessage("Infinite loop")
                }
            } else {
                val finalString = result.result.joinToString(" ")
                if (!commandBus.parseAndFire(
                        event.actor,
                        event.channel,
                        finalString,
                        event.public,
                        false,
                        event.userLocator,
                        Cause(event, mark)
                )) {
                    event.channel.sendMessage(finalString)
                }
            }
            throw CancelEvent
        }
    }

    private inner class CachedCatFunction : Function {
        val results = HashMap<List<String>, List<String>?>()

        override fun evaluate(parameters: LazyExpressionList, mode: Function.EvaluationMode): List<String>? {
            if (!parameters.startsWith("cat")) return null

            val tags = parameters.tailList(1)
            return results.getOrPut(tags) {
                if (results.size >= 2) return null // at most two images per query
                listOf(catDb.getImage(*tags.toTypedArray()).url)
            }
        }
    }

    private object RandomFunction : Function {
        override fun evaluate(parameters: LazyExpressionList, mode: Function.EvaluationMode): List<String>? {
            if (!parameters.startsWith("random")) return null
            val size = parameters.size
            if (size <= 0) return null
            val index = ThreadLocalRandom.current().nextInt(size - 1)
            return listOf(parameters.getOrNull(index + 1)!!)
        }
    }

    data class Entry(val name: String, val value: String) {
        val function = FactoidFunction(name, value)
    }
}