package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.JsAny
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmInline

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
public fun Script.invokeSync(runtime: ScriptRuntime): JsAny? {
    check(!runtime.isSuspendAllowed) {
        "Synchronous script invocation is only available in runtimes with suspending calls disabled. See ScriptRuntime.isSuspendAllowed"
    }
    val res = try {
        @Suppress("UNCHECKED_CAST")
        (::invoke as Function1<Continuation<*>, JsAny?>)
            .invoke(EmptyContinuation(runtime.coroutineContext))
    } catch (t : ClassCastException){
        throw Exception("Synchronous invocation is not compatible with your version of kotlinx.coroutines. Please report this issue", t)
    }
    check(res !== SUSPENDED) {
        "Runtime with disallowed suspension was suspended. That should never happen. Please report this issue"
    }

    return res
}

@JvmInline
private value class EmptyContinuation(override val context: CoroutineContext) : Continuation<Any?> {
    override fun resumeWith(result: Result<Any?>) {}
}

private val SUSPENDED : Any get()  =  kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED

