package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.JsAny
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job

public fun interface Script {

    public suspend operator fun invoke(runtime: ScriptRuntime): JsAny?
}

/**
 * Run the script synchronously.
 *
 * This is only available for runtimes with [ScriptRuntime.isSuspendAllowed] set to false.
 *
 * Such script is not allowed to perform top-level await calls, but you can return a Promise and
 * cast it to [Job] or [Deferred] with [JsAny.toKotlin]
 * */
public fun Script.invokeSync(runtime: ScriptRuntime): JsAny? = runtime.runSync(::invoke)



