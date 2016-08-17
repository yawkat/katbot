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
import org.apache.shiro.authc.credential.CredentialsMatcher
import org.apache.shiro.realm.AuthenticatingRealm
import javax.inject.Inject

/**
 * @author yawkat
 */
internal class TokenAuthenticatingRealm @Inject constructor(
        private val tokenRegistry: TokenRegistry
) : AuthenticatingRealm() {
    init {
        setAuthenticationTokenClass(WebAuthenticationToken::class.java)
        credentialsMatcher = AllowAllCredentialsMatcher()
    }

    override fun doGetAuthenticationInfo(token: AuthenticationToken): AuthenticationInfo {
        val user = tokenRegistry.getUserForToken((token as WebAuthenticationToken).token)
                ?: throw AuthenticationException("Unknown token")
        return IrcAuthenticationInfo(user)
    }

    data class WebAuthenticationToken(val token: String) : AuthenticationToken {
        override fun getCredentials() = token
        override fun getPrincipal() = null
    }
}