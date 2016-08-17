/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.security

import org.apache.shiro.authc.AuthenticationInfo
import org.apache.shiro.subject.SimplePrincipalCollection

internal data class IrcAuthenticationInfo(val info: IrcUserInfo) : AuthenticationInfo {
    override fun getPrincipals() = SimplePrincipalCollection(info, "ircRealm")
    override fun getCredentials() = null
}