/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.web

import org.apache.shiro.ShiroException
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper

/**
 * @author yawkat
 */
object ErrorHandlers {
    private data class Entity(val message: String?)

    class ShiroExceptionMapper : ExceptionMapper<ShiroException> {
        override fun toResponse(exception: ShiroException) =
                Response.status(Response.Status.FORBIDDEN).entity(Entity(exception.message)).build()!!
    }
}