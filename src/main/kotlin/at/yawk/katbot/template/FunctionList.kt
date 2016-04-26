package at.yawk.katbot.template

/**
 * @author yawkat
 */
class FunctionList private constructor(private val entries: List<Entry>) {
    constructor() : this(emptyList<Entry>())

    fun plusFunctionHead(function: Function, mark: Any? = null) =
            FunctionList(entries + Entry(function, mark))

    fun plusFunctionsHead(functions: List<Function>, mark: Any? = null) =
            FunctionList(entries + functions.map { Entry(it, mark) })

    fun plusFunctionTail(function: Function, mark: Any? = null) =
            FunctionList(entries + Entry(function, mark))

    fun minusFunction(mark: Any?) =
            FunctionList(entries.filter { it.mark != mark })

    fun evaluate(parameters: LazyExpressionList): List<String>? {
        return evaluateWithMark(parameters)?.first
    }

    fun evaluateWithMark(parameters: LazyExpressionList): Pair<List<String>, Any?>? {
        for (mode in listOf(Function.EvaluationMode.NORMAL, Function.EvaluationMode.VARARGS)) {
            for (entry in entries) {
                val evaluated = entry.function.evaluate(parameters, mode)
                if (evaluated != null) return Pair(evaluated, entry.mark)
            }
        }
        return null
    }

    private class Entry(val function: Function, val mark: Any?)
}