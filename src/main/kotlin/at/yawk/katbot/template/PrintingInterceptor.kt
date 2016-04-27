/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.template

class PrintingInterceptor : SimpleVM.InvocationInterceptor {
    var depth = 0

    override fun evaluate(functionList: FunctionList, parameters: LazyExpressionList): FunctionList.Result? {
        val indent = " ".repeat(Math.min(depth, 20))
        depth++
        println("${indent}Evaluating < ${parameters.expressions}")
        val result = functionList.evaluate(parameters)
        println("${indent}Evaluated  > ::: $result")
        depth--
        return result
    }
}