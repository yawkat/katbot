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
sealed class Expression {
    /**
     * Size of the return value of [computeValue], if known.
     */
    open val size: Int?
        get() = null
    /**
     * Minimum size of [computeValue], or `0` if unknown.
     */
    open val minSize: Int
        get() = size ?: 0
    /**
     * Maximum size of [computeValue], or [Int.MAX_VALUE] if unknown.
     */
    open val maxSize: Int
        get() = size ?: Int.MAX_VALUE

    abstract fun computeValue(vm: VM): List<String>

    class Literal(val value: String) : Expression() {
        override val size: Int? = 1
        override fun computeValue(vm: VM) = listOf(value)

        override fun toString() = "ConstantExpression($value)"
        override fun equals(other: Any?) = other is Literal && other.value == value
        override fun hashCode() = value.hashCode() + 1
    }

    class SimpleInvocation(val parameters: List<Expression>) : Expression() {
        override val size: Int? = 1
        override fun computeValue(vm: VM) = listOf(vm.invoke(parameters).joinToString(" "))

        override fun toString() = "SimpleInvocationExpression($parameters)"
        override fun equals(other: Any?) = other is SimpleInvocation && other.parameters == parameters
        override fun hashCode() = parameters.hashCode() + 1
    }

    class ExplodedInvocation(val parameters: List<Expression>) : Expression() {
        override fun computeValue(vm: VM) = vm.invoke(parameters)

        override fun toString() = "ExplodedInvocationExpression($parameters)"
        override fun equals(other: Any?) = other is ExplodedInvocation && other.parameters == parameters
        override fun hashCode() = parameters.hashCode() + 1
    }

    /**
     * `[abc, def] + [ghi] + [] + [jkl, mno] = [abc, defghijkl, mno]`
     */
    class ConcatNeighbours(val expressions: List<Expression>) : Expression() {
        override val size: Int? = computeSize()

        private fun computeSize(): Int? {
            var size = 0
            for (expression in expressions) {
                val hereSize = expression.size ?: return null
                if (hereSize > 0) {
                    if (size > 0) {
                        size += hereSize - 1
                    } else {
                        size = hereSize
                    }
                }
            }
            return size
        }

        override fun computeValue(vm: VM): List<String> {
            val folded = ArrayList<String>()
            for (expression in expressions) {
                val subValue = expression.computeValue(vm)
                if (!subValue.isEmpty()) {
                    if (!folded.isEmpty()) {
                        folded[folded.size - 1] += subValue[0]
                        folded.addAll(subValue.subList(1, subValue.size))
                    } else {
                        folded.addAll(subValue)
                    }
                }
            }
            return folded
        }

        override fun toString() = "ConcatNeighboursExpression($expressions)"
        override fun equals(other: Any?) = other is ConcatNeighbours && other.expressions == expressions
        override fun hashCode() = expressions.hashCode() + 1
    }
}

