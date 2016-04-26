/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.template

import org.mockito.Mockito
import org.testng.Assert.*
import org.testng.annotations.Test

/**
 * @author yawkat
 */
class FunctionsTest {
    val vm = Mockito.mock(VM::class.java)

    @Test
    fun `if true`() {
        assertEquals(
                Functions.If.evaluate(LazyExpressionList(vm, Parser.parse("if true abc def ghi")), Function.EvaluationMode.NORMAL),
                listOf("abc")
        )
    }

    @Test
    fun `if false`() {
        assertEquals(
                Functions.If.evaluate(LazyExpressionList(vm, Parser.parse("if false abc def ghi")), Function.EvaluationMode.NORMAL),
                listOf("def", "ghi")
        )
    }

    @Test
    fun `sum`() {
        assertEquals(
                Functions.Sum.evaluate(LazyExpressionList(vm, Parser.parse("sum 1 2 -2 4 0.5")), Function.EvaluationMode.NORMAL),
                listOf("5.5")
        )
    }

    @Test
    fun `sum with removed trailing zeroes`() {
        assertEquals(
                Functions.Sum.evaluate(LazyExpressionList(vm, Parser.parse("sum 1 2 -2 4 0.5 0.5")), Function.EvaluationMode.NORMAL),
                listOf("6")
        )
    }

    @Test
    fun `product`() {
        assertEquals(
                Functions.Product.evaluate(LazyExpressionList(vm, Parser.parse("product 3 2 -4 0.5")), Function.EvaluationMode.NORMAL),
                listOf("-12")
        )
    }
}