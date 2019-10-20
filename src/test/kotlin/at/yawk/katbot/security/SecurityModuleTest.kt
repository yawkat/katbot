/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.security

import com.google.inject.Module
import org.apache.shiro.authc.AuthenticationException
import org.apache.shiro.authz.UnauthorizedException
import org.apache.shiro.mgt.SecurityManager
import org.apache.shiro.subject.Subject
import org.flywaydb.core.Flyway
import org.h2.jdbcx.JdbcConnectionPool
import org.skife.jdbi.v2.DBI
import org.testng.ITestContext
import org.testng.annotations.Guice
import org.testng.annotations.Test
import test.guice.ModuleFactory
import java.util.*
import javax.inject.Inject

/**
 * @author yawkat
 */
@Guice(moduleFactory = SecurityModuleFactory::class)
class SecurityModuleTest {
    @Inject lateinit var securityManager: SecurityManager
    @Inject lateinit var tokenRegistry: TokenRegistry

    @Test
    fun `string permission`() {
        val subject = Subject.Builder(securityManager).buildSubject()
        subject.login(IrcAuthenticationToken(IrcUserInfo("yawkat", "cats.coffee")))

        subject.checkPermission("addFactoids")
    }

    @Test
    fun `global perm object`() {
        val subject = Subject.Builder(securityManager).buildSubject()
        subject.login(IrcAuthenticationToken(IrcUserInfo("yawkat", "cats.coffee")))

        subject.checkPermission(IrcPermission(null, null, PermissionName.ADD_FACTOIDS))
    }

    @Test
    fun `inherited perm object`() {
        val subject = Subject.Builder(securityManager).buildSubject()
        subject.login(IrcAuthenticationToken(IrcUserInfo("yawkat", "cats.coffee")))

        subject.checkPermission(IrcPermission("bla", "blo", PermissionName.ADD_FACTOIDS))
    }

    @Test(expectedExceptions = arrayOf(AuthenticationException::class))
    fun `failed token auth`() {
        val subject = Subject.Builder(securityManager).buildSubject()
        subject.login(TokenAuthenticatingRealm.WebAuthenticationToken("abc"))
    }

    @Test
    fun `successful token auth`() {
        val tk = tokenRegistry.createToken(IrcUserInfo("yawkat", "cats.coffee"))

        val subject = Subject.Builder(securityManager).buildSubject()
        subject.login(TokenAuthenticatingRealm.WebAuthenticationToken(tk))

        subject.checkPermission(IrcPermission("bla", "blo", PermissionName.ADD_FACTOIDS))
    }
}

class SecurityModuleFactory : ModuleFactory() {
    override fun createModule(context: ITestContext, testClass: Class<*>): Module {
        val dataSource = JdbcConnectionPool.create("jdbc:h2:mem:test", "", "")

        val flyway = Flyway()
        flyway.dataSource = dataSource
        flyway.migrate()

        val dbi = DBI(dataSource)
        return SecurityModule(dbi)
    }
}