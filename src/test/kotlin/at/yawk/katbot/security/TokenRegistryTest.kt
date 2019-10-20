/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.security

import org.testng.Assert.*
import org.testng.annotations.Test

/**
 * @author yawkat
 */
class TokenRegistryTest {
    @Test
    fun `token generation`() {
        val token = TokenRegistry().generateToken()
        assertTrue(token.matches("[0-9a-zA-Z]{16}".toRegex()), token)
    }
}