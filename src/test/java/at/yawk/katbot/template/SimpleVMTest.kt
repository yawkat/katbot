/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.template

import org.testng.Assert.*
import org.testng.annotations.Test

/**
 * @author yawkat
 */
class SimpleVMTest {
    @Test
    fun `integration test`() {
        val vm = SimpleVM()
        vm.addFunction(Functions.IfFunction)

        assertEquals(
                vm.invoke(Expression.fromParserComponents(Parser.parse("if true abc def"))),
                listOf("abc")
        )
        assertEquals(
                vm.invoke(Expression.fromParserComponents(Parser.parse("if false abc def ghi"))),
                listOf("def", "ghi")
        )
    }
}