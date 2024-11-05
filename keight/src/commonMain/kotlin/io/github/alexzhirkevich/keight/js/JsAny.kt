package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.callableOrNull
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck
import kotlin.jvm.JvmInline

public interface JsAny {

    public val type : String get() = "object"

    public val keys: List<String> get() = emptyList()

    public suspend fun proto(runtime: ScriptRuntime) : Any? = Unit

    public suspend fun delete(property: Any?, runtime: ScriptRuntime) {
    }

    public suspend fun get(property: Any?, runtime: ScriptRuntime): Any? {
        return when(property){
            "toString" -> ToString(this)
            "__proto__" -> proto(runtime)
            else -> {
                val proto = proto(runtime)
                if (proto is JsAny){
                    return proto.get(property, runtime)
                } else {
                    Unit
                }
            }
        }
    }

    public suspend fun contains(property: Any?, runtime: ScriptRuntime): Boolean =
        get(property, runtime) != Unit

    @JvmInline
    private value class ToString(val value : Any?) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
            return value.toString()
        }

        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return ToString(thisArg)
        }
    }
}

internal suspend fun JsAny.call(
    func : Any?,
    thisRef : Any?,
    args: List<Any?>,
    isOptional : Boolean,
    runtime: ScriptRuntime,
) : Any? {
    val v = get(func, runtime)
    val callable = v?.callableOrNull()
    if (callable == null && isOptional) {
        return Unit
    }
    typeCheck(callable != null) {
        "$v is not a function"
    }
    return callable.call(thisRef, args, runtime)
}