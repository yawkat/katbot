/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.web

import com.google.inject.ImplementedBy
import io.undertow.server.HttpHandler

/**
 * @author yawkat
 */
@ImplementedBy(WebBootstrap::class)
interface WebProvider {
    fun addResource(resource: Any)

    fun addRootHandler(prefix: String, httpHandler: HttpHandler)
}