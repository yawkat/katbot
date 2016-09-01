/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.web

import at.yawk.katbot.security.TokenAuthenticatingRealm
import org.apache.shiro.authc.AuthenticationException
import org.apache.shiro.mgt.SecurityManager
import org.apache.shiro.subject.Subject
import org.jboss.resteasy.spi.ResteasyProviderFactory
import javax.annotation.Priority
import javax.inject.Inject
import javax.ws.rs.Priorities
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter

/**
 * @author yawkat
 */
@Priority(Priorities.AUTHENTICATION)
internal class SecurityContainerRequestFilter @Inject constructor(
        private val securityManager: SecurityManager
) : ContainerRequestFilter {

    override fun filter(requestContext: ContainerRequestContext) {
        val subject = Subject.Builder(securityManager).buildSubject()

        val header: String? = requestContext.getHeaderString("Authorization")
        if (header != null && header.startsWith("Token ")) {
            val token = header.substring("Token ".length)
            try {
                subject.login(TokenAuthenticatingRealm.WebAuthenticationToken(token))
            } catch (e: AuthenticationException) {
                // ignore
            }
        }

        ResteasyProviderFactory.pushContext(Subject::class.java, subject)
    }
}