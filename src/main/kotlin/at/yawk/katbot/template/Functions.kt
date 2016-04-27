/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.template

import java.math.BigDecimal

private val TRUE = listOf("true")
private val FALSE = listOf("false")

/**
 * @author yawkat
 */
object Functions {
    fun isTruthy(value: String) = value != "0" && value.isNotBlank() && !value.equals("false", ignoreCase = true)

    object If : Function {
        override fun evaluate(parameters: LazyExpressionList, mode: Function.EvaluationMode): List<String>? {
            if (mode != Function.EvaluationMode.NORMAL) return null
            if (!parameters.startsWith("if")) return null
            val condition = parameters.getOrNull(1) ?: ""
            if (isTruthy(condition)) {
                val v = parameters.getOrNull(2)
                return if (v == null) emptyList() else listOf(v)
            } else {
                return parameters.tailList(3)
            }
        }
    }

    object Sum : Function {
        override fun evaluate(parameters: LazyExpressionList, mode: Function.EvaluationMode): List<String>? {
            if (mode != Function.EvaluationMode.NORMAL) return null
            if (!parameters.startsWith("sum")) return null

            var value = BigDecimal.ZERO
            for (s in parameters.tailList(1)) {
                try {
                    value += BigDecimal(s)
                } catch(e: NumberFormatException) {
                    return listOf("NaN")
                }
            }
            return listOf(value.stripTrailingZeros().toPlainString())
        }
    }

    object Product : Function {
        override fun evaluate(parameters: LazyExpressionList, mode: Function.EvaluationMode): List<String>? {
            if (mode != Function.EvaluationMode.NORMAL) return null
            if (!parameters.startsWith("product")) return null

            var value = BigDecimal.ONE
            for (s in parameters.tailList(1)) {
                try {
                    value *= BigDecimal(s)
                } catch(e: NumberFormatException) {
                    return listOf("NaN")
                }
            }
            return listOf(value.stripTrailingZeros().toPlainString())
        }
    }

    object Equal : Function {
        override fun evaluate(parameters: LazyExpressionList, mode: Function.EvaluationMode): List<String>? {
            if (mode != Function.EvaluationMode.NORMAL) return null
            if (!parameters.startsWith("equal")) return null

            // only up to one unique parameter
            return if (parameters.tailList(1).toSet().size <= 1) TRUE else FALSE
        }
    }

    private inline fun compareNeighbours(parameters: LazyExpressionList, cmp: (BigDecimal, BigDecimal) -> Boolean): List<String> {
        val tailList = parameters.tailList(1)
        if (tailList.isEmpty()) return TRUE
        try {
            var prev = BigDecimal(tailList[0])
            for (i in 1..tailList.size - 1) {
                val here = BigDecimal(tailList[i])
                if (!cmp(prev, here)) return FALSE
                prev = here
            }
            return TRUE
        } catch (e: NumberFormatException) {
            return FALSE
        }
    }

    object NumberCompare : Function {
        override fun evaluate(parameters: LazyExpressionList, mode: Function.EvaluationMode): List<String>? {
            if (mode != Function.EvaluationMode.NORMAL) return null
            return when (parameters.getOrNull(0)) {
                "lt" -> compareNeighbours(parameters) { left, right -> left < right }
                "gt" -> compareNeighbours(parameters) { left, right -> left > right }
                "leq" -> compareNeighbours(parameters) { left, right -> left <= right }
                "geq" -> compareNeighbours(parameters) { left, right -> left >= right }
                else -> null
            }
        }
    }
}