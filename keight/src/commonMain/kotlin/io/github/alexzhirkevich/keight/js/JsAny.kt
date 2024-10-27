package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.expressions.OpConstant
import kotlin.jvm.JvmInline

public interface JsAny {

    public val type : String get() = "object"

    public val keys: List<String> get() = emptyList()

    public suspend fun proto(runtime: ScriptRuntime) : Any? = Unit

    public suspend fun get(property: Any?, runtime: ScriptRuntime): Any? {
        return when(property){
            "toString" -> ToString(this)
            "__proto__" -> proto(runtime)
            else -> {
                val proto = proto(runtime)
                if (proto is JsAny){
                    val value = proto.get(property, runtime)
                    if (value is Callable){
                        value.bind(listOf(OpConstant(this)), runtime)
                    } else {
                        value
                    }
                } else {
                    Unit
                }
            }
        }
    }

    public suspend fun contains(property: Any?, runtime: ScriptRuntime): Boolean =
        get(property, runtime) != Unit
}

@JvmInline
internal value class ToString(val value : Any?) : Callable {
    override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): Any? {
        return value.toString()
    }
}