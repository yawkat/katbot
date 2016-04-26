/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.template

/**
 * @author yawkat
 */
open class SimpleVM(
        val functions: FunctionList = FunctionList(),
        private val interceptor: InvocationInterceptor = InvocationInterceptor.Default
) : VM {
    open fun invokeIfPresent(parameters: List<Expression>): List<String>? {
        return interceptor.evaluate(functions, LazyExpressionList(this, parameters))
    }

    override fun invoke(parameters: List<Expression>): List<String> {
        return invokeIfPresent(parameters) ?: defaultReturn(parameters)
    }

    fun withInterceptor(interceptor: InvocationInterceptor) = SimpleVM(functions, interceptor)

    private fun withFunctions(functions: FunctionList) = SimpleVM(functions, interceptor)

    override fun plusFunctions(functions: List<Function>) =
            withFunctions(this.functions.plusFunctionsHead(functions))

    fun plusFunctionTail(function: Function, mark: Any? = null) =
            withFunctions(this.functions.plusFunctionTail(function, mark))

    fun withoutFunction(function: Function) = SimpleVM(functions.withoutFunction(function), interceptor)

    protected open fun defaultReturn(parameters: List<Expression>): List<String> {
        return listOf("\${" + parameters.flatMap { it.computeValue(this) }.joinToString(" ") + "}")
    }

    interface InvocationInterceptor {
        fun evaluate(functionList: FunctionList, parameters: LazyExpressionList): List<String>?

        object Default : InvocationInterceptor {
            override fun evaluate(functionList: FunctionList, parameters: LazyExpressionList): List<String>?
                    = functionList.evaluate(parameters)
        }
    }
}