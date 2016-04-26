/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.template

/**
 * @author yawkat
 */
class SimpleVM : VM {
    private var functions = emptyList<Function>()

    fun addFunction(function: Function): RemoveHandle {
        synchronized(this) {
            functions += function
        }
        return object : RemoveHandle {
            override fun remove() {
                synchronized(this@SimpleVM) {
                    functions -= function
                }
            }
        }
    }

    fun invokeIfPresent(parameters: List<Expression>): List<String>? {
        val expressionList = LazyExpressionList(this, parameters)
        for (function in functions) {
            val evaluated = function.evaluate(expressionList)
            if (evaluated != null) return evaluated
        }
        return null
    }

    override fun invoke(parameters: List<Expression>): List<String> {
        return invokeIfPresent(parameters) ?: defaultReturn(parameters)
    }

    protected fun defaultReturn(parameters: List<Expression>): List<String> {
        return listOf("\${" + parameters.flatMap { it.computeValue(this) }.joinToString(" ") + "}")
    }

    interface RemoveHandle {
        fun remove()
    }
}