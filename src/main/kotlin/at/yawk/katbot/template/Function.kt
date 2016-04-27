/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.template

/**
 * @author yawkat
 */
interface Function {
    /**
     * @param mode Evaluation pass to try. This is used so more specific factoids take priority over varargs.
     */
    fun evaluate(parameters: LazyExpressionList, mode: EvaluationMode): List<String>?

    enum class EvaluationMode {
        NORMAL,
        VARARGS,
    }
}