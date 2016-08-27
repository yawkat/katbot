/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.security

import com.fasterxml.jackson.annotation.JsonFormat
import org.apache.shiro.authz.Permission
import java.util.*

/**
 * @author yawkat
 */
@JsonFormat(shape = JsonFormat.Shape.STRING)
enum class PermissionName(val id: String): Permission {
    ADD_FACTOIDS("addFactoids"),
    DELETE_FACTOIDS("deleteFactoids"),
    EDIT_INTERACT("editInteract"),
    IGNORE_THROTTLE("ignoreThrottle"),
    IGNORE_RESTRICT("ignoreRestrict"),
    EDIT_WOSCH("editWosch"),
    EDIT_MARKOV("editMarkov"),
    INVITE("invite"),

    ADMIN("admin"),
    ;

    companion object {
        fun forId(id: String) = PermissionName.values().find { it.id == id } ?: throw NoSuchElementException(id)
    }

    override fun implies(p: Permission) = false
}