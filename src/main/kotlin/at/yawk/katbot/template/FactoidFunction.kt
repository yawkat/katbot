/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package at.yawk.katbot.template

private const val PARAMETER = "$"

/**
 * @author yawkat
 */
class FactoidFunction(name: String, value: String) : Function {
    private val nameParts = name.split(" ")
    private val varargs = nameParts.lastOrNull() == PARAMETER
    private val valueExpressions = Parser.parse(value)

    override fun evaluate(parameters: LazyExpressionList, mode: Function.EvaluationMode): List<String>? {
        if (mode == Function.EvaluationMode.VARARGS && !varargs) return null
        if (!varargs && parameters.minSize > nameParts.size) return null
        if (parameters.maxSize < nameParts.size) return null

        var valueParameters = emptyList<List<String>>()
        val valueIterator = parameters.iterator()
        for ((i, part) in nameParts.withIndex()) {
            if (!valueIterator.hasNext()) return null
            if (part == PARAMETER) {
                if (varargs && i == nameParts.lastIndex) {
                    // take remaining args
                    valueParameters = valueParameters.plusElement(valueIterator.asSequence().toList())
                } else {
                    valueParameters = valueParameters.plusElement(listOf(valueIterator.next()))
                }
            } else {
                val value = valueIterator.next()
                if (!Canonical.equalsCanonical(value, nameParts[i])) {
                    return null // mismatched parameter, bail
                }
            }
        }

        val targetVm = parameters.vm.withFunctions(
                valueParameters.mapIndexed { i, value -> ConstantFunction(listOf((i + 1).toString()), value) })
        return valueExpressions.flatMap { it.computeValue(targetVm) }
    }

    private class ConstantFunction(val name: List<String>, val value: List<String>) : Function {
        override fun evaluate(parameters: LazyExpressionList, mode: Function.EvaluationMode): List<String>? {
            if (parameters.minSize > name.size || parameters.maxSize < name.size) return null
            var i = 0
            for (parameter in parameters) {
                if (i >= name.size) return null
                if (!Canonical.equalsCanonical(name[i], parameter)) return null
                i++
            }
            if (i < name.size) return null
            return value
        }
    }

    fun nameEquals(other: FactoidFunction): Boolean {
        if (other.nameParts.size != nameParts.size) return false
        for (i in nameParts.indices) {
            if (Canonical.equalsCanonical(nameParts[i], other.nameParts[i])) {
                if ((nameParts[i] == PARAMETER) != (other.nameParts[i] == PARAMETER)) {
                    return false
                }
            } else return false
        }
        return true
    }
}