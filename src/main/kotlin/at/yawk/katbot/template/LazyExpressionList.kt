/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.template

import java.util.*

/**
 * Expression list that evaluates expressions lazily when needed.
 * @author yawkat
 */
class LazyExpressionList(val vm: VM, val expressions: List<Expression>) : Iterable<String> {
    /**
     * Computed value array
     */
    private val values = arrayOfNulls<List<String>?>(expressions.size)

    /**
     * Exact size of this expression list in value strings.
     */
    // constant time until the first variadic expression
    val size: Int
        get() = expressions.indices.sumBy { getExpressionSize(it) }

    val minSize: Int
        get() = expressions.indices.sumBy { if (values[it] != null) values[it]!!.size else expressions[it].minSize }
    val maxSize: Int
        get() = expressions.indices.sumBy { if (values[it] != null) values[it]!!.size else expressions[it].maxSize }

    fun startsWith(firstParameter: String): Boolean {
        val first = getOrNull(0)
        return first != null && Canonical.equalsCanonical(first, firstParameter)
    }

    private fun getExpressionValue(index: Int): List<String> {
        if (values[index] == null) values[index] = expressions[index].computeValue(vm)
        return values[index]!!
    }

    // constant time if not variadic
    private fun getExpressionSize(targetExpressionIndex: Int) =
            expressions[targetExpressionIndex].size ?:
                    getExpressionValue(targetExpressionIndex).size

    /**
     * Find the coordinates of the value with the given index.
     * @return Pair(expression index, index in expression value)
     */
    private fun findCoordinate(stringIndex: Int): Pair<Int, Int>? {
        var reachedStringIndex = 0
        for (expressionIndex in expressions.indices) {
            val size = getExpressionSize(expressionIndex)
            if (reachedStringIndex + size > stringIndex) {
                return Pair(expressionIndex, stringIndex - reachedStringIndex)
            } else {
                reachedStringIndex += size
            }
        }
        return null
    }

    fun getOrNull(index: Int): String? {
        val (expr, inExpr) = findCoordinate(index) ?: return null
        return getExpressionValue(expr)[inExpr]
    }

    fun tailList(startIndex: Int): List<String> {
        val (expr, inExpr) = findCoordinate(startIndex) ?: return emptyList()
        val result = ArrayList<String>()
        val firstValues = getExpressionValue(expr)
        result.addAll(firstValues.subList(inExpr, firstValues.size))
        for (i in expr + 1..expressions.size - 1) {
            result.addAll(getExpressionValue(i))
        }
        return result
    }

    override fun iterator() = object : Iterator<String> {
        var expressionIndex = 0
        var indexInExpression = 0

        var next: String? = null

        private fun computeNext(): String? {
            if (next != null) return next
            while (expressionIndex < expressions.size) {
                val expressionValue = getExpressionValue(expressionIndex)
                if (indexInExpression < expressionValue.size) {
                    next = expressionValue[indexInExpression++]
                    return next
                } else {
                    expressionIndex++
                    indexInExpression = 0
                }
            }
            return null
        }

        override fun hasNext() = computeNext() != null

        override fun next(): String {
            val next = computeNext() ?: throw NoSuchElementException()
            this.next = null
            return next
        }
    }
}