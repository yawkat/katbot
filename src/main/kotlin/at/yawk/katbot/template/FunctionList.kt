package at.yawk.katbot.template

/**
 * @author yawkat
 */
interface FunctionList {
    fun evaluate(parameters: LazyExpressionList, mode: Function.EvaluationMode): FunctionList.Result?
    fun evaluate(parameters: LazyExpressionList): Result? {
        for (mode in Function.EvaluationMode.values()) {
            val result = evaluate(parameters, mode)
            if (result != null) return result
        }
        return null
    }

    fun plusFunctionsHead(functions: List<Function>, mark: Any? = null): FunctionList
    fun plusFunctionHead(function: Function, mark: Any? = null) =
            plusFunctionsHead(listOf(function), mark)

    fun plusFunctionsTail(functions: List<Function>, mark: Any? = null): FunctionList
    fun plusFunctionTail(function: Function, mark: Any? = null) =
            plusFunctionsTail(listOf(function), mark)

    fun minusFunction(mark: Any?): FunctionList

    data class Result(val result: List<String>, val mark: Any?)
}

class FunctionListImpl private constructor(private val entries: List<Entry>) : FunctionList {
    constructor() : this(emptyList<Entry>())

    override fun plusFunctionsHead(functions: List<Function>, mark: Any?) =
            FunctionListImpl(functions.map { Entry(it, mark) } + entries)

    override fun plusFunctionsTail(functions: List<Function>, mark: Any?) =
            FunctionListImpl(entries + functions.map { Entry(it, mark) })

    override fun minusFunction(mark: Any?) =
            FunctionListImpl(entries.filter { it.mark != mark })

    override fun evaluate(parameters: LazyExpressionList, mode: Function.EvaluationMode): FunctionList.Result? {
        for (entry in entries) {
            val evaluated = entry.function.evaluate(parameters, mode)
            if (evaluated != null) return FunctionList.Result(evaluated, entry.mark)
        }
        return null
    }

    private class Entry(val function: Function, val mark: Any?)
}