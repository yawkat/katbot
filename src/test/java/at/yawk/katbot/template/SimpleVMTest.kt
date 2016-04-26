/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.template

import org.testng.Assert.assertEquals
import org.testng.annotations.Test

/**
 * @author yawkat
 */
class SimpleVMTest {
    @Test
    fun `basic if expression result`() {
        val vm = SimpleVM().withFunctions(Functions.If)

        assertEquals(
                vm.invoke(Parser.parse("if true abc def")),
                listOf("abc")
        )
        assertEquals(
                vm.invoke(Parser.parse("if false abc def ghi")),
                listOf("def", "ghi")
        )
    }

    @Test
    fun `lazy evaluation if true branch`() {
        var vm = SimpleVM().withFunctions(Functions.If)

        var callCountA = 0
        var callCountB = 0
        vm = vm.withFunctions(object : Function {
            override fun evaluate(parameters: LazyExpressionList, mode: Function.EvaluationMode): List<String>? {
                when (parameters.getOrNull(0)) {
                    "a" -> callCountA++
                    "b" -> callCountB++
                    else -> return null
                }
                return listOf("abc", "def")
            }
        })

        assertEquals(
                vm.invoke(Parser.parse("if true \${a} \${b}")),
                listOf("abc def")
        )
        assertEquals(callCountA, 1)
        assertEquals(callCountB, 0)
    }

    @Test
    fun `lazy evaluation if false branch`() {
        var vm = SimpleVM().withFunctions(Functions.If)

        var callCountA = 0
        var callCountB = 0
        vm = vm.withFunctions(object : Function {
            override fun evaluate(parameters: LazyExpressionList, mode: Function.EvaluationMode): List<String>? {
                when (parameters.getOrNull(0)) {
                    "a" -> callCountA++
                    "b" -> callCountB++
                    else -> return null
                }
                return listOf("abc", "def")
            }
        })

        assertEquals(
                vm.invoke(Parser.parse("if false \${a} \${b}")),
                listOf("abc def")
        )
        assertEquals(callCountA, 0)
        assertEquals(callCountB, 1)
    }
}