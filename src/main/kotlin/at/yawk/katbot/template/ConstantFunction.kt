/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.template

class ConstantFunction(val name: List<String>, val valueFunction: () -> List<String>) : Function {
    constructor(name: List<String>, value: List<String>) : this(name, { value })
    constructor(name: String, value: String) : this(listOf(name), listOf(value))

    override fun evaluate(parameters: LazyExpressionList, mode: Function.EvaluationMode): List<String>? {
        if (parameters.minSize > name.size || parameters.maxSize < name.size) return null
        var i = 0
        for (parameter in parameters) {
            if (i >= name.size) return null
            if (!Canonical.equalsCanonical(name[i], parameter)) return null
            i++
        }
        if (i < name.size) return null
        return valueFunction()
    }
}