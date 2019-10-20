/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.command

import org.testng.Assert.assertEquals
import org.testng.annotations.Test

/**
 * @author yawkat
 */
class CommandLineTest {
    fun parseParameters(string: String) = CommandLine.parseParameters(string)

    @Test
    fun `parse parameters`() {
        assertEquals(parseParameters("\"Kaffee?\""), listOf("Kaffee?"))
        assertEquals(parseParameters("Kaffee?"), listOf("Kaffee?"))
        assertEquals(parseParameters("Ka\"f f\"ee?"), listOf("Kaf fee?"))
        assertEquals(parseParameters("Kaf\\ fee?"), listOf("Kaf fee?"))
        assertEquals(parseParameters("Kaf fee?"), listOf("Kaf", "fee?"))
    }

    @Test
    fun `escape nothing`() {
        assertEquals(CommandLine.escape("abc"), "abc")
    }

    @Test
    fun `escape some stuff`() {
        assertEquals(CommandLine.escape("abc \"xyz\" \\\""), "abc\\ \\\"xyz\\\"\\ \\\\\\\"")
    }
}