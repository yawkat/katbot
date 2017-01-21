/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.action

import at.yawk.katbot.EventBus
import com.fasterxml.jackson.databind.ObjectMapper
import org.testng.Assert.assertTrue
import org.testng.annotations.Test

/**
 * @author yawkat
 */
class GuildWars2ItemTest {
    @Test
    fun `test simple query`() {
        val o = GuildWars2Item(
                ObjectMapper().findAndRegisterModules(),
                EventBus()
        )
        val result = o.runForQuery("divine lucky envelope", "yawkat")
        assertTrue(result.matches(
                "yawkat: Divine Lucky Envelope buys \\d+ sells \\d+ \\| Other matches: '.+'(, '.+'){3} ...".toRegex()),
                result)
    }
}