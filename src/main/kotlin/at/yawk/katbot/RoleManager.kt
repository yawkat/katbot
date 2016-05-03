/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot

import com.google.inject.ImplementedBy
import org.kitteh.irc.client.library.element.User
import java.sql.SQLException
import java.util.*
import javax.inject.Inject
import javax.sql.DataSource

/**
 * @author yawkat
 */
@ImplementedBy(RoleManagerImpl::class)
interface RoleManager {
    fun hasRole(user: User, role: Role): Boolean
}

class RoleManagerImpl @Inject constructor(val eventBus: EventBus, val dataSource: DataSource) : RoleManager {
    fun start() {
        eventBus.subscribe(this)
    }

    override fun hasRole(user: User, role: Role): Boolean {
        return dataSource.connection.closed {
            val roleNames = (role.impliedBy + role).map { it.name }
            val statement = it.prepareStatement("select count(*) from roles where username=? and host=? and array_contains(?, role)")
            statement.setString(1, user.nick)
            statement.setString(2, user.host)
            statement.setObject(3, roleNames.toTypedArray())
            val result = statement.executeQuery()
            result.next()
            result.getInt(1) > 0
        }
    }

    @Subscribe(priority = -1)
    fun command(event: Command) {
        if (!event.line.startsWith("roles")) return
        if (event.line.parameters.size < 2) return

        val targetName = event.line.parameters[1]
        val target = event.userLocator.getUser(targetName)
        if (target == null) {
            event.channel.sendMessage("Unknown user")
            throw CancelEvent
        }

        if (event.line.parameters.size > 2) {
            if (!hasRole(event.actor, Role.ADMIN)) {
                event.channel.sendMessage("You are not allowed to do that.")
                throw CancelEvent
            }

            val changeStrings = event.line.parameterRange(2).map { it }
            dataSource.connection.closed {
                for (change in changeStrings) {
                    val add = change[0] == '+'
                    val role = try {
                        Role.valueOf(change.substring(1).toUpperCase())
                    } catch(e: IllegalArgumentException) {
                        // role not found
                        event.channel.sendMessage("Unknown role $change")
                        null
                    }
                    if (role != null) {
                        try {
                            val statement = it.prepareStatement(
                                    if (add) "INSERT INTO roles (username, host, role) VALUES (?, ?, ?);"
                                    else "delete from roles where username=? and host=? and role=?"
                            )
                            statement.setString(1, target.nick)
                            statement.setString(2, target.host)
                            statement.setString(3, role.name)
                            if (statement.executeUpdate() == 0) {
                                event.channel.sendMessage("Did not apply $change")
                            }
                        } catch(e: SQLException) {
                            event.channel.sendMessage((e.message ?: "Error").replace("\\s+".toRegex(), " "))
                        }
                    }
                }
            }
            event.channel.sendMessage("Done.")
            throw CancelEvent
        } else {
            dataSource.connection.closed {
                val statement = it.prepareStatement("select role from roles where username=? and host=?")
                statement.setString(1, target.nick)
                statement.setString(2, target.host)
                val result = statement.executeQuery()

                val roles = ArrayList<String>()
                while (result.next()) {
                    roles.add(result.getString("role"))
                }

                if (roles.isEmpty()) {
                    event.channel.sendMessage("No roles")
                } else {
                    event.channel.sendMessage(roles.joinToString(", "))
                }
            }
            throw CancelEvent
        }
    }
}

enum class Role(vararg impliedBy: Role) {
    ADMIN(),
    ADD_FACTOIDS(ADMIN),
    DELETE_FACTOIDS(ADMIN),
    EDIT_INTERACT(ADMIN),
    EDIT_WOSCH(ADMIN),
    IGNORE_THROTTLE(ADMIN),
    IGNORE_RESTRICT(ADMIN);

    /**
     * flat [impliedBy] array.
     */
    val impliedBy: Set<Role> = impliedBy.toSet() + impliedBy.flatMap { it.impliedBy }
}