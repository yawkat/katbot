package at.yawk.katbot

import org.kitteh.irc.client.library.element.User
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent
import java.sql.Types
import java.util.*
import java.util.regex.Pattern
import javax.inject.Inject
import javax.sql.DataSource

/**
 * @author yawkat
 */
class RoleManager @Inject constructor(val ircProvider: IrcProvider, val dataSource: DataSource) {
    companion object {
        private val EDIT_ROLE_PATTERN = "~roles ($NAME_PATTERN)((?: +[+-]\\w+)+)".toPattern(Pattern.CASE_INSENSITIVE)
        private val GET_ROLES_PATTERN = "~roles ($NAME_PATTERN)".toPattern(Pattern.CASE_INSENSITIVE)
    }

    fun start() {
        ircProvider.registerEventListener(this)
    }

    fun hasRole(user: User, role: Role): Boolean {
        return dataSource.connection.closed {
            val roleNames = (role.impliedBy + role).map { it.name }
            val statement = it.prepareStatement("select count(*) from roles where username=? and host=? and role in (?)")
            statement.setString(1, user.nick)
            statement.setString(2, user.host)
            statement.setObject(3, roleNames)
            val result = statement.executeQuery()
            result.next()
            result.getInt(1) > 0
        }
    }

    @Subscribe(priority = -1)
    fun onPublicMessage(event: ChannelMessageEvent) {
        val getMatcher = GET_ROLES_PATTERN.matcher(event.message)
        if (getMatcher.matches()) {
            val targetName = getMatcher.group(1)
            val target = event.channel.getUser(targetName).orElse(null)
            if (target == null) {
                event.channel.sendMessage("Unknown user")
                throw CancelEvent
            }
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

        val editMatcher = EDIT_ROLE_PATTERN.matcher(event.message)
        if (editMatcher.matches()) {
            if (!hasRole(event.actor, Role.ADMIN)) {
                event.channel.sendMessage("You are not allowed to do that.")
                throw CancelEvent
            }

            val targetNick = editMatcher.group(1)
            val target = event.channel.getUser(targetNick).orElse(null)
            if (target == null) {
                event.channel.sendMessage("Unknown user")
                throw CancelEvent
            }
            val changeStrings = editMatcher.group(2).split(" ").filter { it != "" }
            println(changeStrings)
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
                        val statement = it.prepareStatement(
                                if (add) "INSERT INTO roles (username, host, role) VALUES (?, ?, ?);"
                                else "remove from roles where username=? and host=? and role=?"
                        )
                        statement.setString(1, target.nick)
                        statement.setString(2, target.host)
                        statement.setString(3, role.name)
                        if (statement.executeUpdate() == 0) {
                            event.channel.sendMessage("Did not apply $change")
                        }
                    }
                }
            }
            throw CancelEvent
        }
    }
}

enum class Role(vararg impliedBy: Role) {
    ADMIN(),
    ADD_FACTOIDS(ADMIN);

    /**
     * flat [impliedBy] array.
     */
    val impliedBy: Set<Role> = impliedBy.toSet() + impliedBy.flatMap { it.impliedBy }
}