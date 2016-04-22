/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot

import java.util.*

/**
 * @author yawkat
 */
class CommandLine private constructor(
        /** user input - do not trust */
        val message: String,
        /** user input - do not trust */
        val parameters: List<String> = CommandLine.parseParameters(message)
) {
    constructor(message: String) : this(message, parseParameters(message))

    constructor(parameters: List<String>) : this(parameters.map { escape(it) }.joinToString(" "), parameters)

    fun messageIs(string: String) = message.equals(string, ignoreCase = true)
    fun startsWith(parameterString: String) = !parameters.isEmpty() && parameters[0].equals(parameterString, ignoreCase = true)

    fun parameterRange(start: Int, end: Int = parameters.size) = parameters.subList(start, end)

    companion object {
        internal fun parseParameters(message: String): List<String> {
            val parameters = ArrayList<String>()
            val parameterValue = StringBuilder()

            fun flushParameter() {
                if (parameterValue.length > 0) {
                    parameters.add(parameterValue.toString())
                    parameterValue.setLength(0)
                }
            }

            var quoted = false
            var escaped = false
            for (c in message) {
                if (!escaped) {
                    if (c == '"') {
                        quoted = !quoted
                        continue
                    } else if (c == '\\') {
                        escaped = true
                        continue
                    } else if (c == ' ' && !quoted) {
                        flushParameter()
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
}