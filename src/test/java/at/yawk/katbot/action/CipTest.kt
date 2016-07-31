/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.action

import at.yawk.katbot.EventBus
import at.yawk.katbot.action.Cip
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.impl.client.HttpClientBuilder
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * @author yawkat
 */
class CipTest {
    lateinit var cip: Cip

    @BeforeMethod
    fun setUp() {
        cip = Cip(ObjectMapper().findAndRegisterModules(), HttpClientBuilder.create().build(), EventBus())
    }

    @Test
    fun testLoadMap() {
        println(cip.loadMap())
    }

    @Test
    fun testLoadState() {
        println(cip.loadState())
    }
}