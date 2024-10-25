package io.github.alexzhirkevich.keight.es

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.common.Callable
import kotlin.jvm.JvmInline

public interface ESAny {

    public val type : String get() = "object"

    public operator fun get(variable: Any?): Any? {
        return when(variable){
            "toString" -> ToString(this)
            else -> Unit
        }
    }

    public operator fun contains(variable: Any?): Boolean =
        get(variable) != Unit

    public operator fun invoke(
        function: String,
        context: ScriptRuntime,
        arguments: List<Expression>
    ): Any? {
        return when {
            else -> throw TypeError("${type}.$function is not a function")
        }
    }
}

@JvmInline
internal value class ToString(val value : Any?) : Callable {
    override fun invoke(args: List<Expression>, runtime: ScriptRuntime): Any? {
        return value.toString()
    }
}