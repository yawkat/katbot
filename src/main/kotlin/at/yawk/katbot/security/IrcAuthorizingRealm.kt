/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.security

import org.apache.shiro.authc.AuthenticationException
import org.apache.shiro.authc.AuthenticationInfo
import org.apache.shiro.authc.AuthenticationToken
import org.apache.shiro.authc.credential.AllowAllCredentialsMatcher
import org.apache.shiro.authz.AuthorizationInfo
import org.apache.shiro.authz.SimpleAuthorizationInfo
import org.apache.shiro.authz.permission.PermissionResolver
import org.apache.shiro.realm.AuthorizingRealm
import org.apache.shiro.subject.PrincipalCollection
import javax.inject.Inject

/**
 * @author yawkat
 */
internal class IrcAuthorizingRealm @Inject constructor(val dao: SecurityDao) : AuthorizingRealm() {
    init {
        setAuthenticationTokenClass(IrcAuthenticationToken::class.java)
        credentialsMatcher = AllowAllCredentialsMatcher()
        permissionResolver = PermissionResolver { id -> IrcPermission(null, null, PermissionName.forId(id)) }
    }

    override fun doGetAuthenticationInfo(token: AuthenticationToken): AuthenticationInfo = when (token) {
        is IrcAuthenticationToken -> IrcAuthenticationInfo(token.info)
        else -> throw AuthenticationException("Unsupported token type ${token.javaClass.name}")
    }

    override fun doGetAuthorizationInfo(principals: PrincipalCollection): AuthorizationInfo {
        var roles = dao.getRoles(principals.primaryPrincipal as IrcUserInfo)
        if (roles.isEmpty()) roles = setOf(Security.DEFAULT_ROLE_NAME)
        val info = SimpleAuthorizationInfo(roles)
        info.addObjectPermissions(info.roles.flatMap { dao.getPermissions(it) })
        return info
    }
}