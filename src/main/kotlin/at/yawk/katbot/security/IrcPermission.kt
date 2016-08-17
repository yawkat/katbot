/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.security

import org.apache.shiro.authz.Permission
import org.skife.jdbi.v2.StatementContext
import org.skife.jdbi.v2.tweak.ResultSetMapper
import java.sql.ResultSet

data class IrcPermission(val server: String?, val channel: String?, val permission: PermissionName) : Permission {
    private fun implies(p: IrcPermission) =
            this.permission == p.permission &&
                    (this.server == null || this.server == p.server) &&
                    (this.channel == null || this.channel == p.channel)

    private fun implies(p: PermissionName) =
            this.permission == p && this.server == null && this.channel == null

    override fun implies(p: Permission) =
            (p is IrcPermission && implies(p)) || (p is PermissionName && implies(p))

}