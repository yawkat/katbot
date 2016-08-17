/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.security

import at.yawk.katbot.Config
import at.yawk.katbot.EventBus
import at.yawk.katbot.Subscribe
import at.yawk.katbot.command.Command
import at.yawk.katbot.web.WebProvider
import org.apache.shiro.subject.Subject
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.core.Context

/**
 * @author yawkat
 */
@Path("/security")
class WebSecurityEditor @Inject internal constructor(
        private val eventBus: EventBus,
        private val tokenRegistry: TokenRegistry,
        private val webProvider: WebProvider,
        private val config: Config,
        private val dao: SecurityDao
) {
    fun start() {
        webProvider.addResource(this)
        eventBus.subscribe(this)
    }

    @Subscribe
    fun command(command: Command) {
        if (command.line.messageIs("web")) {
            val token = tokenRegistry.createToken(Security.createUserInfo(command.actor))
            var url = config.web.externalHost
            if (!url.endsWith("/")) url += "/"
            url += "#auth:$token"
            command.actor.sendNotice(url)
        }
    }

    @GET
    @Path("/rolePermissions")
    fun getRolePermissionMatrix(@Context subject: Subject) {
        subject.checkPermission(PermissionName.ADMIN)


    }
}