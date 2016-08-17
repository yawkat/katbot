/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.security

import org.apache.shiro.guice.ShiroModule
import org.skife.jdbi.v2.DBI

/**
 * @author yawkat
 */
class SecurityModule(val dbi: DBI) : ShiroModule() {
    override fun configureShiro() {
        bind(SecurityDao::class.java).toInstance(dbi.onDemand(SecurityDao::class.java))

        bindRealm().to(IrcAuthorizingRealm::class.java)
        bindRealm().to(TokenAuthenticatingRealm::class.java)
    }
}
