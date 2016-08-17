/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.security

import com.google.common.collect.Multimap
import org.skife.jdbi.v2.StatementContext
import org.skife.jdbi.v2.sqlobject.Bind
import org.skife.jdbi.v2.sqlobject.BindBean
import org.skife.jdbi.v2.sqlobject.SqlQuery
import org.skife.jdbi.v2.sqlobject.customizers.Mapper
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper
import org.skife.jdbi.v2.tweak.ResultSetMapper
import java.sql.ResultSet

@RegisterMapper(PermissionResultSetMapper::class)
internal interface SecurityDao {
    @SqlQuery("select role from user_roles where username = :username and host = :host")
    fun getRoles(@BindBean info: IrcUserInfo): Set<String>

    @SqlQuery("select * from role_permissions where role = :role")
    fun getPermissions(@Bind("role") role: String): Set<IrcPermission>

    @SqlQuery("select * from role_permissions")
    @Mapper(AllPermissionsMapper::class)
    fun getAllPermissions(): List<Pair<String, IrcPermission>>
}

internal class PermissionResultSetMapper : ResultSetMapper<IrcPermission> {
    override fun map(index: Int, r: ResultSet, ctx: StatementContext) = IrcPermission(
            r.getString("server"),
            r.getString("channel"),
            PermissionName.forId(r.getString("permission"))
    )
}

internal class AllPermissionsMapper : ResultSetMapper<Multimap<String, IrcPermission>> {
    override fun map(index: Int, r: ResultSet, ctx: StatementContext): Multimap<String, IrcPermission> {

    }
}