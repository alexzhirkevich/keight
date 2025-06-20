package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.expressions.asCallable
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.interpreter.typeError
import io.github.alexzhirkevich.keight.js.js
import kotlin.jvm.JvmInline

public interface Callable : JsAny {

    public suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny?

    public suspend fun call(thisArg: JsAny?, args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
        return bind(thisArg, emptyList(), runtime).invoke(args, runtime)
    }

    public suspend fun bind(thisArg: JsAny?, args: List<JsAny?>, runtime: ScriptRuntime): Callable

    override fun toKotlin(runtime: ScriptRuntime): suspend (List<JsAny?>) -> Any? =
        { invoke(it, runtime)?.toKotlin(runtime) }
}

public fun Callable(body: suspend ScriptRuntime.(List<JsAny?>) -> JsAny?): Callable =
    CallableImpl(body)

internal fun JsAny.callableOrNull() : Callable? {
    return when(this){
        is Callable -> this
        is Function<*> -> asCallable()
        else -> null
    }
}

internal suspend fun JsAny?.callableOrThrow(runtime: ScriptRuntime) : Callable {
    return this?.callableOrNull() ?: runtime.typeError { "$this it is not a function".js }
}

@JvmInline
private value class CallableImpl(
    val body: suspend ScriptRuntime.(List<JsAny?>) -> JsAny?
) : Callable {

    override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
        return body(runtime, args)
    }

    override suspend fun call(thisArg: JsAny?, args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
        return runtime.withScope(thisArg) {
            invoke(args, it)
        }
    }

    override suspend fun bind(
        thisArg: JsAny?,
        args: List<JsAny?>,
        runtime: ScriptRuntime
    ): Callable = BindedCallable(thisArg, args, body)
}

private class BindedCallable(
    val thisArg: JsAny?,
    val args : List<JsAny?>,
    val body: suspend ScriptRuntime.(List<JsAny?>) -> JsAny?
) : Callable {

    override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
        return runtime.withScope(thisArg) {
            body(it, this@BindedCallable.args + args)
        }
    }

    override suspend fun call(thisArg: JsAny?, args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
        return invoke(this@BindedCallable.args + args, runtime)
    }

    override suspend fun bind(
        thisArg: JsAny?,
        args: List<JsAny?>,
        runtime: ScriptRuntime
    ): Callable {
        return BindedCallable(this.thisArg, this.args + args, body)
    }
}


