/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot

import org.testng.Assert.assertEquals
import org.testng.annotations.Test

/**
 * @author yawkat
 */
class TemplateTest {
    @Test
    fun `normal`() {
        assertEquals(
                Template("abc def ghi").finish(),
                "abc def ghi"
        )
    }

    @Test
    fun `missing arg`() {
        assertEquals(
                Template("abc \${1} ghi").finish(),
                "abc \${1} ghi"
        )
    }

    @Test
    fun `set arg`() {
        assertEquals(
                Template("abc \${1} ghi").set("1", { "def" }).finish(),
                "abc def ghi"
        )
    }

    fun reversed(commandLine: CommandLine) = CommandLine(commandLine.parameters.map { it.reversed() }.reversed())

    @Test
    fun `missing val`() {
        assertEquals(
                Template("abc \${def} ghi").withMissingFunction { reversed(it) }.finish(),
                "abc fed ghi"
        )
    }

    @Test
    fun `nested`() {
        assertEquals(
                Template("abc \${def \${1}} ghi").set("1", "xyz").withMissingFunction { reversed(it) }.finish(),
                "abc zyx fed ghi"
        )
    }

    @Test
    fun `nested linked parameters`() {
        assertEquals(
                Template("abc \${def \${1}} ghi").set("1", "a b c").withMissingFunction {
                    assertEquals(it.parameters, listOf("def", "a b c"))
                    CommandLine("lll")
                }.finish(),
                "abc lll ghi"
        )
    }
}