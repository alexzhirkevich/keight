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

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {
        return when (property.toString()) {
            "call" -> Call(this)
            "bind" -> Bind(this)
            "apply" -> Apply(this)
            else -> super.get(property, runtime)
        }
    }

    @JvmInline
    private value class Call(val thisRef: Callable) : Callable {

        override suspend fun bind(
            thisArg: JsAny?,
            args: List<JsAny?>,
            runtime: ScriptRuntime
        ): Callable {
            return Call(thisArg.callableOrThrow(runtime))
        }

        override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
            return thisRef.call(args.getOrNull(0), args.drop(1), runtime)
        }
    }

    @JvmInline
    private value class Bind(val thisRef: Callable) : Callable {

        override suspend fun bind(
            thisArg: JsAny?,
            args: List<JsAny?>,
            runtime: ScriptRuntime
        ): Callable {
            return Bind(thisArg.callableOrThrow(runtime))
        }

        override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
            return thisRef.bind(args.getOrNull(0), args.drop(1), runtime)
        }
    }

    @JvmInline
    private value class Apply(val thisRef: Callable) : Callable {

        override suspend fun bind(
            thisArg: JsAny?,
            args: List<JsAny?>,
            runtime: ScriptRuntime
        ): Callable {
            return Apply(thisArg.callableOrThrow(runtime))
        }

        override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
            return thisRef.call(
                thisArg = args.getOrNull(0),
                args = (args.getOrNull(1) as? List<JsAny?>).orEmpty(),
                runtime = runtime
            )
        }
    }
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

internal fun Any.callableOrNull() : Callable? {
    return when(this){
        is Callable -> this
        is Function<*> -> asCallable()
        else -> null
    }
}

internal suspend fun Any?.callableOrThrow(runtime: ScriptRuntime) : Callable {
    return this?.callableOrNull() ?: runtime.typeError { "$this it is not a function" }
}

