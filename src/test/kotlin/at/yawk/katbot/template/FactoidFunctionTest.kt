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
class FactoidFunctionTest {
    fun toExpr(vararg strings: String) = strings.map { Expression.Literal(it) }

    @Test
    fun `simple substitution`() {
        val vm = SimpleVM().plusFunctionTail(FactoidFunction("factoid x", "abc def ghi"))
        assertEquals(
                vm.invokeIfPresent(toExpr("factoid", "x")),
                listOf("abc", "def", "ghi")
        )
    }

    @Test
    fun `parameter`() {
        val vm = SimpleVM().plusFunctionTail(FactoidFunction("factoid $ x", "abc \${1} ghi"))
        assertEquals(
                vm.invokeIfPresent(toExpr("factoid", "def", "x")),
                listOf("abc", "def", "ghi")
        )
    }

    @Test
    fun `override parameter`() {
        val vm = SimpleVM()
                .plusFunctionTail(FactoidFunction("a $", "ar\${1} \${b st}"))
                .plusFunctionTail(FactoidFunction("b $", "br\${1}"))
        assertEquals(
                vm.invokeIfPresent(toExpr("a", "bc")),
                listOf("arbc", "brst")
        )
    }

    @Test
    fun `varargs no match`() {
        val vm = SimpleVM().plusFunctionTail(FactoidFunction("factoid x $", "abc def *\${1}"))
        assertEquals(
                vm.invokeIfPresent(toExpr("factoid", "x")),
                null
        )
    }

    @Test
    fun `varargs one match`() {
        val vm = SimpleVM().plusFunctionTail(FactoidFunction("factoid x $", "abc def *\${1}"))
        assertEquals(
                vm.invokeIfPresent(toExpr("factoid", "x", "ghi")),
                listOf("abc", "def", "ghi")
        )
    }

    @Test
    fun `varargs multi match`() {
        val vm = SimpleVM().plusFunctionTail(FactoidFunction("factoid x $", "abc def *\${1}"))
        assertEquals(
                vm.invokeIfPresent(toExpr("factoid", "x", "ghi", "jkl")),
                listOf("abc", "def", "ghi", "jkl")
        )
    }
}