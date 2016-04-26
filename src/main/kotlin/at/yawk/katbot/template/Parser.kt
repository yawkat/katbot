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
    fun parse(input: String): List<Expression> {
        return parseComponents(input, 0, true).first
    }

    private fun parseComponents(input: String, start: Int, outermost: Boolean): Pair<List<Expression>, Int> {
        val expressions = ArrayList<Expression>()
        var currentExpression = emptyList<Expression>()
        val currentLiteral = StringBuilder()

        fun flushLiteral() {
            if (currentLiteral.length > 0) {
                currentExpression += Expression.Literal(currentLiteral.toString())
                currentLiteral.setLength(0)
            }
        }

        fun flushExpression() {
            flushLiteral()
            expressions.add(when (currentExpression.size) {
                0 -> Expression.Literal("")
                1 -> currentExpression[0]
                else -> Expression.ConcatNeighbours(currentExpression)
            })
            currentExpression = emptyList()
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
                    i += if (explodedExpression) 3 else 2
                    val (expression, end) = parseComponents(input, i, false)
                    flushLiteral()
                    currentExpression +=
                            if (explodedExpression && !quoted) Expression.ExplodedInvocation(expression)
                            else Expression.SimpleInvocation(expression)
                    i = end
                } else if (!outermost && input[i] == '}') {
                    i++
                    break
                } else if (input[i] == '"') {
                    quoted = !quoted
                    i++
                } else if (input[i] == ' ' && !quoted) {
                    flushExpression()
                    i++
                } else {
                    currentLiteral.append(input[i++])
                }
            }
        }
        flushExpression()
        return Pair(expressions, i)
    }
}