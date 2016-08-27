/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.security

import org.skife.jdbi.v2.StatementContext
import org.skife.jdbi.v2.sqlobject.Bind
import org.skife.jdbi.v2.sqlobject.BindBean
import org.skife.jdbi.v2.sqlobject.SqlQuery
import org.skife.jdbi.v2.sqlobject.SqlUpdate
import org.skife.jdbi.v2.sqlobject.customizers.Mapper
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper
import org.skife.jdbi.v2.tweak.ResultSetMapper
import java.sql.ResultSet

@RegisterMapper(PermissionResultSetMapper::class)
internal interface SecurityDao {
    @SqlQuery("select role from user_roles where username = :username and host = :host order by role")
    fun getRoles(@BindBean info: IrcUserInfo): Set<String>

    @SqlQuery("select * from role_permissions where role = :role order by server, channel, permission")
    fun getPermissions(@Bind("role") role: String): Set<IrcPermission>

    @SqlQuery("select role from roles order by role")
    fun getAllRoles(): Set<String>

    @SqlQuery("select * from role_permissions order by server, channel, permission")
    @Mapper(AllPermissionsMapper::class)
    fun getAllPermissions(): List<Pair<String, IrcPermission>>

    @SqlUpdate("MERGE INTO roles (role) VALUES (:name)")
    fun createOrUpdateRole(@Bind("name") name: String)

    @SqlUpdate("DELETE FROM roles WHERE role = :name")
    fun deleteRole(@Bind("name") name: String)

    @SqlQuery("SELECT * FROM user_roles order by host, username, role")
    @Mapper(AllUserRolesMapper::class)
    fun getAllUserRoles(): List<Pair<IrcUserInfo, String>>
}

internal class PermissionResultSetMapper : ResultSetMapper<IrcPermission> {
    override fun map(index: Int, r: ResultSet, ctx: StatementContext) = IrcPermission(
            r.getString("server").let { if (it == "") null else it },
            r.getString("channel").let { if (it == "") null else it },
            PermissionName.forId(r.getString("permission"))
    )
}

internal class AllPermissionsMapper : ResultSetMapper<Pair<String, IrcPermission>> {
    override fun map(index: Int, r: ResultSet, ctx: StatementContext): Pair<String, IrcPermission> =
            r.getString("role") to PermissionResultSetMapper().map(index, r, ctx)
}

internal class AllUserRolesMapper : ResultSetMapper<Pair<IrcUserInfo, String>> {
    override fun map(index: Int, r: ResultSet, ctx: StatementContext): Pair<IrcUserInfo, String> =
            IrcUserInfo(username = r.getString("username"), host = r.getString("host")) to r.getString("role")
}