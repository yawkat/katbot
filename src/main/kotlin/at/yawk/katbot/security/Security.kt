/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.security

import org.apache.shiro.authc.AuthenticationToken
import org.apache.shiro.mgt.SecurityManager
import org.apache.shiro.subject.Subject
import org.kitteh.irc.client.library.element.Channel
import org.kitteh.irc.client.library.element.User

/**
 * @author yawkat
 */
object Security {
    val DEFAULT_ROLE_NAME = "DEFAULT"

    fun createAuthenticationToken(actor: User): AuthenticationToken = IrcAuthenticationToken(createUserInfo(actor))

    fun createUserInfo(actor: User) = IrcUserInfo(actor.nick, actor.host)

    fun getSubjectForUser(securityManager: SecurityManager, actor: User): Subject {
        val subject = Subject.Builder(securityManager).buildSubject()
        subject.login(Security.createAuthenticationToken(actor))
        return subject
    }

    fun getIrcUserForSubject(subject: Subject): IrcUserInfo = subject.principal as IrcUserInfo

    fun createPermissionForChannelAndName(channel: Channel?, name: PermissionName) = IrcPermission(null, channel?.name, name)
}