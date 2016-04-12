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
class WoschTest {
    @Test
    fun `no wosch`() {
        assertEquals(woschinize("lorem ipsum dolor sit amet"), "lorem ipsum dolor sit amet")
    }

    @Test
    fun `one wosch`() {
        assertEquals(woschinize("lorem ipsum dolthreador sit amet"), "lorem ipsum dolfadenor sit amet")
    }

    @Test
    fun `two wosch`() {
        assertEquals(woschinize("lorcipem ipsum dolthreador sit amcipet"), "lorrechnerschwimmbeckenem ipsum dolfadenor sit amrechnerschwimmbeckenet")
    }

    @Test
    fun `capitalize`() {
        assertEquals(woschinize("thread Thread"), "faden Faden")
    }
}