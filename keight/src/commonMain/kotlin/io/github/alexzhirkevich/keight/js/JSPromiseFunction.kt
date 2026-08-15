package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.CallFrame
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.asyncFormStack
import io.github.alexzhirkevich.keight.js.JSFunction
import io.github.alexzhirkevich.keight.callableOrNull
import io.github.alexzhirkevich.keight.callableOrThrow
import io.github.alexzhirkevich.keight.expressions.OpConstant
import io.github.alexzhirkevich.keight.expressions.ThrowableValue
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck
import io.github.alexzhirkevich.keight.js.interpreter.typeError
import io.github.alexzhirkevich.keight.requireThisRef
import io.github.alexzhirkevich.keight.thisRef
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlin.coroutines.cancellation.CancellationException

/**
 * Push a call frame for a callback that is invoked programmatically (e.g. a
 * Promise `then`/`catch`/`finally` handler). Such callbacks are not reached
 * through an `OpCall` expression, so they would otherwise never get a frame on
 * the async task's isolated call stack.
 */
private suspend fun ScriptRuntime.pushCallbackFrame(callable: Callable?) {
    val jsFn = callable as? JSFunction
    val name = jsFn?.name?.takeIf { it.isNotEmpty() }
    val loc = jsFn?.sourceLocation
    pushCallFrame(
        CallFrame(
            functionName = name,
            fileName = loc?.fileName,
            lineNumber = loc?.line,
            columnNumber = loc?.column,
        )
    )
}

/**
 * Implement JS `Promise` resolution: if a `.then`/`.catch`/`.finally` callback
 * returns a thenable (a Promise, represented here by a [Job]), the resulting
 * promise must *adopt* that thenable's state rather than resolving with the
 * job object itself. So we recursively await returned jobs until we reach a
 * non-promise value (or a rejection, which propagates out).
 *
 * This mirrors the Promise Resolution Procedure and is what makes
 * `Promise.resolve().then(() => Promise.resolve().then(...))` chains unwrap,
 * and what lets an error thrown deep inside the chain surface at the `await`.
 */
private suspend fun resolveThenable(result: JsAny?, runtime: ScriptRuntime): JsAny? {
    var value: JsAny? = result
    while (value is Job) {
        value = if (value is Deferred<*>) {
            value.await() as JsAny?
        } else {
            value.joinSuccess()
            Undefined
        }
    }
    return value
}

internal class JSPromiseFunction : JSFunction(
    name = "Promise",
    prototype = Object {
        "catch".js.func("onrejected") { args ->
            val arg = args.getOrNull(0)
            val callable = arg.callableOrThrow(this)
            val job = requireThisRef("Promise.prototype.catch").toKotlin(this) as Job

            async {
                try {
                    if (job is Deferred<*>) {
                        job.await() as JsAny
                    } else {
                        job.joinSuccess()
                        Undefined
                    }
                } catch (t: CancellationException) {
                    throw t
                } catch (t: Throwable) {
                    this@func.pushCallbackFrame(callable)
                    try {
                        resolveThenable(callable.invoke(t.js.listOf(), this@func), this@func)
                    } finally {
                        this@func.popCallFrame()
                    }
                }
            }.js
        }
        "then".js.func(
            FunctionParam("onfulfilled"),
            FunctionParam("onrejected", default = OpArgOmitted),
        ) { args ->
            val onFulfilled = args.getOrNull(0)
            val job = requireThisRef("Promise.prototype.then").toKotlin(this) as Job

            async {
                try {
                    val res = if (job is Deferred<*>) {
                        job.await() as JsAny?
                    } else {
                        job.joinSuccess()
                        Undefined
                    }
                    val cb = onFulfilled.callableOrThrow(this@func)
                    this@func.pushCallbackFrame(cb)
                    try {
                        resolveThenable(cb.invoke(res.listOf(), this@func), this@func)
                    } finally {
                        this@func.popCallFrame()
                    }
                } catch (t: CancellationException) {
                    throw t
                } catch (t: Throwable) {
                    val rej = args.argOrElse(1) { throw t }.callableOrThrow(this@func)
                    this@func.pushCallbackFrame(rej)
                    try {
                        resolveThenable(rej.invoke(t.js.listOf(), this@func), this@func)
                    } finally {
                        this@func.popCallFrame()
                    }
                }
            }.js
        }
        "finally".js.func("handle") { args ->
            val arg = args.getOrNull(0)
            val callable = arg?.callableOrNull()
            val value = thisRef<Job>()
            typeCheck(callable is Callable) {
                "$arg is not a function".js
            }

            async {
                try {
                    value.joinSuccess()
                    Undefined
                } finally {
                    this@func.pushCallbackFrame(callable)
                    try {
                        resolveThenable(callable.invoke(emptyList(), this@func), this@func)
                    } finally {
                        this@func.popCallFrame()
                    }
                }
            }.js
        }
    },
    properties = listOf(
        "resolve".func( FunctionParam("value", default = OpConstant(Undefined))) {
            CompletableDeferred(it[0]).js
        },
        "reject".func(FunctionParam("reason", default = OpConstant(Undefined))) {
            val v = it[0]

            CompletableDeferred<Undefined>().apply {
                completeExceptionally(
                    v as? Throwable ?: ThrowableValue(v)
                )
            }.js
        },
        "all".func(FunctionParam("values")) { args ->
            async {
                @Suppress("UNCHECKED_CAST")
                val iterable = args[0] as? Iterable<JsAny?>
                    ?: typeError { "${args.getOrNull(0)} is not iterable".js }
                // Launch every member concurrently and await them all, mirroring
                // the ECMAScript semantics: the returned promise resolves with an
                // array of the resolved values (preserving input order) and
                // rejects with the reason of the first member that rejects.
                val settled = iterable.map { el ->
                    async {
                        val job = el?.toKotlin(this@func)
                        if (job is Job) {
                            if (job is Deferred<*>) {
                                job.await() as JsAny?
                            } else {
                                job.joinSuccess()
                                Undefined
                            }
                        } else {
                            // Non-promise values are treated as already-resolved.
                            el
                        }
                    }
                }
                settled.awaitAll().js
            }.js
        }
    ).associateBy { it.name.js }.toMutableMap()
) {

    override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
        runtime.typeError { "Promise constructor cannot be invoked without 'new'".js }
    }

    override suspend fun construct(args: List<JsAny?>, runtime: ScriptRuntime): JsAny {
        val resolveReject = args.getOrNull(0).callableOrThrow(runtime)

        val deferred = CompletableDeferred<JsAny?>()

        val resolve = "resolve".func("value") {
            deferred.complete(it.argOrElse(0) { Undefined }); Undefined
        }

        val reject = "reject".func("value") {
            val x = it.argOrElse(0) { Undefined }
            deferred.completeExceptionally(
                x as? Throwable ?: ThrowableValue(x)
            ); Undefined
        }
        return runtime.asyncFormStack {
            resolveReject.invoke(listOf(resolve, reject), runtime)
            deferred.await()
        }.js
    }
}


internal suspend fun Job.joinSuccess(){
    join()
    ensureSuccess()
}

@OptIn(InternalCoroutinesApi::class)
internal fun Job.ensureSuccess() {
    if (isCancelled) {
        val err = try {
            getCancellationException()
        } catch (t: Throwable) {
            throw CancellationException("Promise was rejected or cancelled", t)
        }
        throw err.cause ?: err
    }
}