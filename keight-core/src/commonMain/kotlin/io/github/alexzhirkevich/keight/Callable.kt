package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.JsAny
import kotlin.jvm.JvmInline

public interface Callable : JsAny {

    public suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny?

    public suspend fun call(thisArg: JsAny?, args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
        return bind(thisArg, emptyList(), runtime).invoke(args, runtime)
    }

    public suspend fun bind(thisArg: JsAny?, args: List<JsAny?>, runtime: ScriptRuntime): Callable

    override fun toKotlin(runtime: ScriptRuntime) : Any {
        val res: suspend (List<JsAny?>) -> Any? = { invoke(it, runtime)?.toKotlin(runtime) }
        return res
    }
}

public fun Callable(body: suspend ScriptRuntime.(List<JsAny?>) -> JsAny?): Callable =
    CallableImpl(body)

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


