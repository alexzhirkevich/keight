package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.JsAny
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

public interface Script {

    public val runtime : ScriptRuntime

    /**
     * Invoke compiled script
     *
     * @param runtime runtime to invoke the script in.
     * The script can only be invoked in the runtime or a sub-runtime of the engine it compiled with.
     *
     * @see ScriptEngine.runtime
     * @see JsAny.toKotlin
     * */
    public suspend operator fun invoke(runtime: ScriptRuntime = this.runtime): JsAny?
}

/**
 * Run the script synchronously.
 *
 * This is only available for runtimes with [ScriptRuntime.isSuspendAllowed] set to false.
 *
 * Such script is not allowed to perform top-level await calls, but you can return a Promise and
 * cast it to [Job] or [Deferred] with [JsAny.toKotlin]
 * */
public fun Script.invokeSync(runtime: ScriptRuntime = this.runtime): JsAny? = runtime.runSync {
    check(!isSuspendAllowed) {
        "Synchronous invocation is only available in runtimes with suspending calls disabled. See ScriptRuntime.isSuspendAllowed"
    }
    invoke(this)
}



