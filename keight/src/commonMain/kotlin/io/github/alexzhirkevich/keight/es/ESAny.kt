package io.github.alexzhirkevich.keight.es

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime

public interface ESAny {

    public val type : String get() = "object"

    public operator fun get(variable: Any?): Any?

    public operator fun contains(variable: Any?): Boolean

    public operator fun invoke(
        function: String,
        context: ScriptRuntime,
        arguments: List<Expression>
    ): Any? {
        return when {
            function == "toString" && arguments.isEmpty() -> toString()
            else -> throw TypeError("${type}.$function is not a function")
        }
    }
}