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
        var valueParameters = findParameters(parameters, mode) ?: return null

        val targetVm = parameters.vm.plusFunctions(
                valueParameters.mapIndexed { i, value -> ConstantFunction(listOf((i + 1).toString()), value) })
        return valueExpressions.flatMap { it.computeValue(targetVm) }
    }

    /**
     * `true` if a call to [evaluate] with the same parameters will yield a non-`null` result. Does not evaluate the
     * result, so it is faster.
     */
    fun canEvaluate(parameters: LazyExpressionList, mode: Function.EvaluationMode) =
            findParameters(parameters, mode) != null

    private fun findParameters(parameters: LazyExpressionList, mode: Function.EvaluationMode): List<List<String>>? {
        if (mode == Function.EvaluationMode.VARARGS && !varargs) return null

        if (mode != Function.EvaluationMode.VARARGS && parameters.minSize > nameParts.size) return null
        if (parameters.maxSize < nameParts.size) return null

        var valueParameters = emptyList<List<String>>()
        val valueIterator = parameters.iterator()
        for ((i, part) in nameParts.withIndex()) {
            if (!valueIterator.hasNext()) return null
            if (part == PARAMETER) {
                if (mode == Function.EvaluationMode.VARARGS && i == nameParts.lastIndex) {
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
        return valueParameters
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