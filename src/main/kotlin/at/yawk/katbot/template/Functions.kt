/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.template

/**
 * @author yawkat
 */
object Functions {
    fun isTruthy(value: String) = value != "0" && value.isNotBlank() && !value.equals("false", ignoreCase = true)

    object IfFunction : Function {
        override fun evaluate(parameters: LazyExpressionList): List<String>? {
            if (!parameters.startsWith("if")) return null
            val condition = parameters.getOrNull(1) ?: ""
            if (isTruthy(condition)) {
                val v = parameters.getOrNull(2)
                return if (v == null) emptyList() else listOf(v)
            } else {
                return parameters.tailList(3)
            }
        }
    }
}