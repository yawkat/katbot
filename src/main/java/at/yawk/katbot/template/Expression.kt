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
    companion object {
        fun fromParserComponents(components: List<Component>): List<Expression> {
            val expressions = ArrayList<Expression>()
            var unseparated = emptyList<Expression>()
            fun flushExpression() {
                expressions.add(when (unseparated.size) {
                    0 -> ConstantExpression("")
                    1 -> unseparated[0]
                    else -> ConcatNeighboursExpression(unseparated)
                })
                unseparated = emptyList<Expression>()
            }

            for (component in components) {
                when (component) {
                    is Component.Separator -> flushExpression()
                    is Component.Literal ->
                        unseparated += ConstantExpression(component.value)
                    is Component.SimpleExpression ->
                        unseparated += SimpleInvocationExpression(fromParserComponents(component.components))
                    is Component.ExplodedExpression ->
                        unseparated += ExplodedInvocationExpression(fromParserComponents(component.components))
                }
            }
            return expressions
        }
    }

    /**
     * Size of the return value of [computeValue], if known.
     */
    open val size: Int? = null

    abstract fun computeValue(vm: VM): List<String>

    class ConstantExpression(val value: String) : Expression() {
        override val size: Int? = 1
        override fun computeValue(vm: VM) = listOf(value)
    }

    class SimpleInvocationExpression(val parameters: List<Expression>) : Expression() {
        override val size: Int? = 1
        override fun computeValue(vm: VM) = listOf(vm.invoke(parameters).joinToString(" "))
    }

    class ExplodedInvocationExpression(val parameters: List<Expression>) : Expression() {
        override fun computeValue(vm: VM) = vm.invoke(parameters)
    }

    /**
     * `[abc, def] + [ghi] + [] + [jkl, mno] = [abc, defghijkl, mno]`
     */
    class ConcatNeighboursExpression(val expressions: List<Expression>) : Expression() {
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
    }
}

