package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.expressions.asCallable
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.interpreter.typeError
import kotlin.jvm.JvmInline

public interface Callable : JsAny {

    public suspend fun call(thisArg: JsAny?, args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
        return bind(thisArg, emptyList(), runtime).invoke(args, runtime)
    }

    public suspend fun bind(thisArg: JsAny?, args: List<JsAny?>, runtime: ScriptRuntime): Callable

    public suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny?
}

internal fun Callable(body: suspend ScriptRuntime.(List<JsAny?>) -> JsAny?) = object :Callable {
    override suspend fun bind(thisArg: JsAny?, args: List<JsAny?>, runtime: ScriptRuntime): Callable {
        return this
    }

    override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
        return with(runtime) {
            body(args)
        }
    }
}

internal fun JsAny.callableOrNull() : Callable? {
    return when(this){
        is Callable -> this
        is Function<*> -> asCallable()
        else -> null
    }
}

internal suspend fun JsAny?.callableOrThrow(runtime: ScriptRuntime) : Callable {
    return this?.callableOrNull() ?: runtime.typeError { "$this it is not a function" }
}

