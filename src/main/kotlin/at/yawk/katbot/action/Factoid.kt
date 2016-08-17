/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.action

import at.yawk.katbot.*
import at.yawk.katbot.command.Cause
import at.yawk.katbot.command.Command
import at.yawk.katbot.command.CommandBus
import at.yawk.katbot.security.PermissionName
import at.yawk.katbot.template.*
import at.yawk.katbot.template.Function
import org.kitteh.irc.client.library.element.MessageReceiver
import java.util.*
import javax.inject.Inject
import javax.sql.DataSource

/**
 * @author yawkat
 */
class Factoid @Inject constructor(
        val eventBus: EventBus,
        val catDb: CatDb,
        val dataSource: DataSource,
        val commandBus: CommandBus
) {
    companion object {
        fun sendTemplateResultToChannel(channel: MessageReceiver, message: String) {
            var msg = message
            if (msg.startsWith("/me ")) {
                val ctcp = "ACTION ${msg.substring(4)}"
                channel.sendCTCPMessage(ctcp)
            } else {
                // escape /me with /send
                if (msg.startsWith("/send ")) msg = msg.substring("/send ".length)
                channel.sendMessageSafe(msg)
            }
        }
    }

    private var factoids = emptyList<Entry>()
    private val vm = SimpleVM(FactoidFunctionList().plusFunctionsHead(Functions.DEFAULT_FUNCTIONS))

    @Synchronized
    private fun removeFactoid(entry: Entry) {
        dataSource.connection.closed {
            val statement = it.prepareStatement("delete from factoids where canonicalName = ?")
            statement.setString(1, entry.name)
            statement.execute()
        }
        factoids -= entry
    }

    @Synchronized
    private fun addFactoid(entry: Entry, insertIntoDb: Boolean) {
        for (other in factoids) {
            if (other.function.nameEquals(entry.function)) {
                removeFactoid(other)
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
        factoids = (factoids + entry).sortedBy { it.function.parameterCount }
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

    @Subscribe(priority = 100) // low priority
    fun command(event: Command) {
        val line = event.line
        if (line.message.contains(" = ")) {
            event.checkPermission(PermissionName.ADD_FACTOIDS)

            val splitIndex = line.message.indexOf(" = ")
            val name = line.message.substring(0, splitIndex)
            val value = line.message.substring(splitIndex + " = ".length).trimEnd()
            addFactoid(Entry(name, value), insertIntoDb = true)
            event.channel.sendMessageSafe("Factoid added.")
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
                event.channel.sendMessageSafe("No such factoid")
            } else {
                event.channel.sendMessageSafe("~${match.name} = ${match.value}")
            }
            throw CancelEvent
        }

        if (line.startsWith("delete")) {
            event.checkPermission(PermissionName.DELETE_FACTOIDS)
            val match = findFactoidForRawAndDelete()
            if (match == null) {
                event.channel.sendMessageSafe("No such factoid")
            } else {
                removeFactoid(match)
                event.channel.sendMessageSafe("Factoid deleted")
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
                    event.channel.sendMessageSafe("Infinite loop in factoid ${mark.name}")
                } else {
                    event.channel.sendMessageSafe("Infinite loop")
                }
            } else {
                val finalString = result.result.joinToString(" ")
                if (!commandBus.parseAndFire(
                        event.context,
                        finalString,
                        false,
                        Cause(event, mark)
                )) {
                    sendTemplateResultToChannel(event.channel, finalString)
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

    /**
     * [FunctionList] implementation that uses the [factoids] field. Allows us to keep factoids sorted properly.
     */
    private inner class FactoidFunctionList(
            val head: FunctionList = FunctionListImpl(),
            val tail: FunctionList = FunctionListImpl()
    ) : FunctionList {
        override fun evaluate(parameters: LazyExpressionList, mode: Function.EvaluationMode): FunctionList.Result? {
            head.evaluate(parameters, mode)?.let { return it }
            for (factoid in factoids) {
                val result = factoid.function.evaluate(parameters, mode)
                if (result != null) return FunctionList.Result(result, factoid)
            }
            return tail.evaluate(parameters, mode)
        }

        override fun plusFunctionsHead(functions: List<Function>, mark: Any?) =
                FactoidFunctionList(head.plusFunctionsHead(functions, mark), tail)

        override fun plusFunctionsTail(functions: List<Function>, mark: Any?) =
                FactoidFunctionList(head.plusFunctionsTail(functions, mark), tail)

        override fun minusFunction(mark: Any?) =
                FactoidFunctionList(head.minusFunction(mark), tail.minusFunction(mark))
    }

    data class Entry(val name: String, val value: String) {
        val function = FactoidFunction(name, value)
    }
}