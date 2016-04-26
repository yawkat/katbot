/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.template

import java.util.*

/**
 * @author yawkat
 */
object Parser {
    fun parse(input: String): List<Component> {
        return parseComponents(input, 0, true).first
    }

    private fun parseComponents(input: String, start: Int, outermost: Boolean): Pair<List<Component>, Int> {
        val components = ArrayList<Component>()
        val currentLiteral = StringBuilder()

        fun flushLiteral() {
            if (currentLiteral.length > 0) {
                components.add(Component.Literal(currentLiteral.toString()))
                currentLiteral.setLength(0)
            }
        }

        var i = start
        var quoted = false
        while (i < input.length) {
            if (input[i] == '\\' && i < input.length - 1) {
                i++
                currentLiteral.append(input[i++])
            } else {
                val explodedExpression = input.startsWith("*\${", i)
                val simpleExpression = input.startsWith("\${", i)
                if (simpleExpression || explodedExpression) {
                    flushLiteral()
                    i += if (explodedExpression) 3 else 2
                    val (expression, end) = parseComponents(input, i, false)
                    components.add(
                            if (explodedExpression && !quoted) Component.ExplodedExpression(expression)
                            else Component.SimpleExpression(expression))
                    i = end
                } else if (!outermost && input[i] == '}') {
                    i++
                    break
                } else if (input[i] == '"') {
                    quoted = !quoted
                    i++
                } else if (input[i] == ' ' && !quoted) {
                    flushLiteral()
                    components.add(Component.Separator)
                    i++
                } else {
                    currentLiteral.append(input[i++])
                }
            }
        }
        flushLiteral()
        return Pair(components, i)
    }
}

sealed class Component {
    object Separator : Component() {
        override fun toString() = "Separator()"
    }

    class Literal(val value: String) : Component() {
        override fun hashCode() = value.hashCode() + 1
        override fun equals(other: Any?) = other is Literal && other.value == value
        override fun toString() = "Literal($value)"
    }

    class SimpleExpression(val components: List<Component>) : Component() {
        override fun hashCode() = components.hashCode() + 1
        override fun equals(other: Any?) = other is SimpleExpression && other.components == components
        override fun toString() = "SimpleExpression($components)"
    }

    class ExplodedExpression(val components: List<Component>) : Component() {
        override fun hashCode() = components.hashCode() + 1
        override fun equals(other: Any?) = other is ExplodedExpression && other.components == components
        override fun toString() = "ExplodedExpression($components)"
    }
}