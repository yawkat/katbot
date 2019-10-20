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
class TemplateTest {
    @Test
    fun `missing substitution`() {
        assertEquals(Template("hi my name is \${the name}, it is awesome").evaluate(),
                "hi my name is \${the name}, it is awesome")
    }

    @Test
    fun `basic substitution`() {
        assertEquals(Template("hi my name is \${the name}, it is awesome").with("the name", "awesome").evaluate(),
                "hi my name is awesome, it is awesome")
    }
}