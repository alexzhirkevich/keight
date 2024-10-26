package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import kotlin.jvm.JvmInline

public interface JsAny {

    public val type : String get() = "object"

    public operator fun get(variable: Any?): Any? {
        return when(variable){
            "toString" -> ToString(this)
            else -> Unit
        }
    }

    public operator fun contains(variable: Any?): Boolean =
        get(variable) != Unit
}

@JvmInline
internal value class ToString(val value : Any?) : Callable {
    override fun invoke(args: List<Expression>, runtime: ScriptRuntime): Any? {
        return value.toString()
    }
}