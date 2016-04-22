/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot

import org.kitteh.irc.client.library.element.MessageReceiver
import java.util.*

private const val MAX_MESSAGE_LENGTH = 450
private val DEFAULT_PARAMETERS = mapOf<String, () -> String>(Pair("$", { "$" }))

private fun truncate(string: String) =
        if (string.length > MAX_MESSAGE_LENGTH) string.substring(0, MAX_MESSAGE_LENGTH) else string

// ${name}
private fun toTemplateExpression(name: String) = "\${$name}"

/**
 * @author yawkat
 */
class Template(
        data: String,
        val simpleParameters: Map<String, () -> String> = DEFAULT_PARAMETERS,
        val missingFunction: (CommandLine) -> CommandLine? = { null }
) {
    private val data = truncate(data)

    fun set(name: String, value: String) = set(name) { value }
    fun set(name: String, value: () -> String) = Template(data, simpleParameters + Pair(name, value), missingFunction)

    fun withMissingFunction(missingFunction: (CommandLine) -> CommandLine?) = Template(data, simpleParameters, missingFunction)

    private fun evaluateSubExpression(start: Int, outermost: Boolean): Pair<List<String>, Int> {
        val currentValue = StringBuilder()
        val items = ArrayList<String>()

        var escaped = false
        var quoted = false
        var i = start
        while (i < data.length) {
            val c = data[i++]
            if (!escaped) {
                if (c == '\\') {
                    escaped = true
                    continue
                } else if (c == '$' && i < data.length - 1 && data[i] == '{') {
                    val (subValue, subEnd) = evaluateSubExpression(i + 1, false)
                    for ((j, value) in subValue.withIndex()) {
                        if (j > 0) {
                            items.add(currentValue.toString())
                            currentValue.setLength(0)
                        }
                        currentValue.append(value)
                    }
                    i = subEnd
                    continue
                } else if (!outermost && c == '}') {
                    break
                } else if (c == '"') {
                    quoted = !quoted
                    continue
                } else if (c == ' ' && !quoted) {
                    items.add(currentValue.toString())
                    currentValue.setLength(0)
                    continue
                }
            } else {
                escaped = false
                continue
            }
            currentValue.append(c)
            if (currentValue.length >= MAX_MESSAGE_LENGTH) break
        }
        items.add(currentValue.toString())
        if (outermost) {
            return Pair(items, i)
        } else {
            val joined = items.joinToString(" ")
            val matchingParameter = simpleParameters[joined]
            if (matchingParameter != null) return Pair(listOf(matchingParameter()), i)
            val evaluatedFunction = missingFunction(CommandLine(items))
            if (evaluatedFunction != null) return Pair(evaluatedFunction.parameters, i)
            return Pair(listOf(toTemplateExpression(joined)), i)
        }
    }

    fun finish() = evaluateSubExpression(0, true).first.joinToString(" ")

    fun sendTo(vararg messageReceiver: MessageReceiver) {
        sendTo(messageReceiver.asList())
    }

    fun sendTo(messageReceivers: List<MessageReceiver>) {
        var msg = finish()
        if (msg.startsWith("/me ")) {
            val ctcp = "ACTION ${msg.substring(4)}"
            messageReceivers.forEach { it.sendCTCPMessage(ctcp) }
        } else {
            // escape /me and ~ prefix with /send
            if (msg.startsWith("/send ")) msg = msg.substring("/send ".length)
            messageReceivers.forEach { it.sendMessage(msg) }
        }
    }

    fun setActorAndTarget(command: Command): Template {
        return set("actor", command.actor.nick)
                .set("target", (command.target ?: command.actor).nick)
    }

    fun sendAsReply(command: Command) {
        setActorAndTarget(command).sendTo(command.channel)
    }
}