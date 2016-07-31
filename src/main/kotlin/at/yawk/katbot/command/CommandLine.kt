/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.command

import java.util.*

fun CommandLine(parameters: List<String>): CommandLine {
    val message = StringBuilder()
    val _parameters = ArrayList<CommandLine.Parameter>()
    for (parameter in parameters) {
        if (!message.isEmpty()) message.append(' ')
        _parameters.add(CommandLine.Parameter(parameter, message.length))
        message.append(CommandLine.escape(parameter))
    }
    return CommandLine(message.toString(), _parameters)
}

/**
 * @author yawkat
 */
class CommandLine internal constructor(
        /** user input - do not trust */
        val message: String,
        /** user input - do not trust */
        private val _parameters: List<Parameter>
) {
    val parameters = _parameters.map { it.value }

    constructor(message: String) : this(message, parseParameters(message))

    fun messageIs(string: String) = message.equals(string, ignoreCase = true)
    fun startsWith(parameterString: String) = !parameters.isEmpty() && parameters[0].equals(parameterString, ignoreCase = true)

    fun parameterRange(start: Int, end: Int = parameters.size) = parameters.subList(start, end)

    fun tailParameterString(startIndex: Int) = message.substring(_parameters[startIndex].startPosition)

    companion object {
        internal fun parseParameters(message: String): List<Parameter> {
            val parameters = ArrayList<Parameter>()
            val parameterValue = StringBuilder()
            var currentParameterStart = 0

            fun flushParameter() {
                if (parameterValue.length > 0) {
                    parameters.add(Parameter(parameterValue.toString(), currentParameterStart))
                    parameterValue.setLength(0)
                }
            }

            var quoted = false
            var escaped = false
            for ((i, c) in message.withIndex()) {
                if (!escaped) {
                    if (c == '"') {
                        quoted = !quoted
                        continue
                    } else if (c == '\\') {
                        escaped = true
                        continue
                    } else if (c == ' ' && !quoted) {
                        flushParameter()
                        currentParameterStart = i + 1
                        continue
                    }
                } else {
                    escaped = false
                }
                parameterValue.append(c)
            }
            flushParameter()
            return parameters
        }

        fun escape(message: String) = message
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace(" ", "\\ ")
    }

    data class Parameter(val value: String, val startPosition: Int)
}