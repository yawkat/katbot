/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.action

import org.testng.Assert
import org.testng.annotations.Test

/**
 * @author yawkat
 */
class KatTempTest {
    @Test
    fun `format simple`() = Assert.assertEquals(
            KatTemp.format(0.1),
            "0.1"
    )

    @Test
    fun `format round`() = Assert.assertEquals(
            KatTemp.format(0.05),
            "0.1"
    )

    @Test
    fun `format hide decimal place`() = Assert.assertEquals(
            KatTemp.format(1.0),
            "1"
    )
}