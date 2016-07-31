/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.action

import at.yawk.katbot.action.Substitution
import at.yawk.katbot.action.woschinize
import org.testng.Assert.assertEquals
import org.testng.annotations.Test

private val testSubs = listOf(
        Substitution("thread", "faden"),
        Substitution("cip", "rechnerschwimmbecken"),
        Substitution("include", "einbinde"),
        Substitution("if", "falls", wordBoundary = true)
)

/**
 * @author yawkat
 */
class WoschTest {
    @Test
    fun `no wosch`() {
        assertEquals(woschinize(testSubs, "lorem ipsum dolor sit amet"), "lorem ipsum dolor sit amet")
    }

    @Test
    fun `one wosch`() {
        assertEquals(woschinize(testSubs, "lorem ipsum dolthreador sit amet"), "lorem ipsum dolfadenor sit amet")
    }

    @Test
    fun `two wosch`() {
        assertEquals(woschinize(testSubs, "lorcipem ipsum dolthreador sit amcipet"), "lorrechnerschwimmbeckenem ipsum dolfadenor sit amrechnerschwimmbeckenet")
    }

    @Test
    fun `capitalize`() {
        assertEquals(woschinize(testSubs, "thread Thread"), "faden Faden")
    }

    @Test
    fun `boundary`() {
        assertEquals(woschinize(testSubs, "if-schleife"), "falls-schleife")
    }

    @Test
    fun `includen`() {
        assertEquals(woschinize(testSubs, "includen"), "einbinden")
    }
}