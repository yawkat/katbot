package at.yawk.katbot.template

/**
 * @author yawkat
 */
class FunctionList private constructor(private val entries: List<Entry>) {
    constructor() : this(emptyList<Entry>())

    fun plusFunctionsHead(functions: List<Function>, mark: Any? = null) =
            FunctionList(functions.map { Entry(it, mark) } + entries)

    fun plusFunctionTail(function: Function, mark: Any? = null) =
            FunctionList(entries + Entry(function, mark))

    fun minusFunction(mark: Any?) =
            FunctionList(entries.filter { it.mark != mark })

    fun evaluate(parameters: LazyExpressionList): Result? {
        for (mode in Function.EvaluationMode.values()) {
            for (entry in entries) {
                val evaluated = entry.function.evaluate(parameters, mode)
                if (evaluated != null) return Result(evaluated, entry.mark)
            }
        }
        return null
    }

    private class Entry(val function: Function, val mark: Any?)

    data class Result(val result: List<String>, val mark: Any?)
}