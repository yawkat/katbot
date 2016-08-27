/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.security

import at.yawk.katbot.CancelEvent
import at.yawk.katbot.Config
import at.yawk.katbot.EventBus
import at.yawk.katbot.Subscribe
import at.yawk.katbot.command.Command
import at.yawk.katbot.web.WebProvider
import com.fasterxml.jackson.annotation.JsonUnwrapped
import org.apache.shiro.subject.Subject
import org.skife.jdbi.v2.DBI
import javax.inject.Inject
import javax.ws.rs.*
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
        private val dao: SecurityDao,
        private val dbi: DBI
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
            throw CancelEvent
        }
    }

    @GET
    @Path("/permissions")
    fun getPermissions() = PermissionName.values().toList()

    @GET
    @Path("/roles")
    fun getRoles(@Context subject: Subject): Map<String, Set<IrcPermission>> {
        subject.checkPermission(PermissionName.ADMIN)
        return dao.getAllRoles().associate { it to dao.getPermissions(it) }
    }

    @PUT
    @Path("/roles/{role:\\w+}")
    fun createOrUpdateRole(@Context subject: Subject, @PathParam("role") role: String, permissions: Set<IrcPermission>) {
        subject.checkPermission(PermissionName.ADMIN)
        dbi.inTransaction { handle, tx ->
            val txDao = handle.attach(SecurityDao::class.java)
            txDao.createOrUpdateRole(role)

            // failsafe - disallow adding ADMIN permission
            val oldPermissions = txDao.getPermissions(role)
            val removedPermissions = oldPermissions - permissions
            if (removedPermissions.any { it.permission == PermissionName.ADMIN }) {
                throw BadRequestException("Don't do that")
            }

            handle.update("DELETE FROM role_permissions WHERE role = ?", role)
            val batch = handle.prepareBatch("INSERT INTO role_permissions (role, server, channel, permission) VALUES (:role, :server, :channel, :permission)")
            for ((server, channel, permission) in permissions) {
                // failsafe - disallow adding ADMIN permission
                if (permission == PermissionName.ADMIN && oldPermissions.none { it.permission == PermissionName.ADMIN }) {
                    throw BadRequestException("Don't do that")
                }
                batch.add(mapOf(
                        "role" to role,
                        "server" to (server ?: ""),
                        "channel" to (channel ?: ""),
                        "permission" to permission.id
                ))
            }
            batch.execute()
        }
    }

    @DELETE
    @Path("/roles/{role}")
    fun deleteRole(@Context subject: Subject, @PathParam("role") role: String) {
        subject.checkPermission(PermissionName.ADMIN)
        // failsafe - disallow deleting rule with ADMIN permission
        if (dao.getPermissions(role).any { it.permission == PermissionName.ADMIN }) {
            throw BadRequestException("Don't do that")
        }
        if (role == Security.DEFAULT_ROLE_NAME) {
            throw BadRequestException("Cannot delete default role")
        }
        dao.deleteRole(role) // this cascades
    }

    @GET
    @Path("/roles/{role}")
    fun getRole(@Context subject: Subject, @PathParam("role") role: String): Set<IrcPermission> {
        subject.checkPermission(PermissionName.ADMIN)
        return dao.getPermissions(role)
    }

    data class UserRoles(@get:JsonUnwrapped val info: IrcUserInfo, val roles: List<String>)

    @GET
    @Path("/users")
    fun getUserRoles(@Context subject: Subject): List<UserRoles> {
        subject.checkPermission(PermissionName.ADMIN)
        return dao.getAllUserRoles().groupBy({ it.first }, { it.second }).map { UserRoles(it.key, it.value) }
    }

    @PUT
    @Path("/users/{username:[^@]+}@{host:.+}")
    fun updateUser(@Context subject: Subject, @PathParam("username") username: String, @PathParam("host") host: String, roles: Set<String>) {
        subject.checkPermission(PermissionName.ADMIN)
        dbi.inTransaction { handle, tx ->
            // failsafe - disallow removing own role with ADMIN permission
            if (Security.getIrcUserForSubject(subject) == IrcUserInfo(username, host)) {
                val removedRoles = handle.attach(SecurityDao::class.java).getRoles(IrcUserInfo(username, host)) - roles
                for (removedRole in removedRoles) {
                    if (dao.getPermissions(removedRole).any { it.permission == PermissionName.ADMIN }) {
                        throw BadRequestException("Don't do that")
                    }
                }
            }

            handle.update("DELETE FROM user_roles WHERE username = ? AND host = ?", username, host)
            val batch = handle.prepareBatch("INSERT INTO user_roles (username, host, role) VALUES (:username, :host, :role)")
            for (role in roles) {
                batch.add(mapOf(
                        "role" to role,
                        "host" to host,
                        "username" to username
                ))
            }
            batch.execute()
        }
    }
}