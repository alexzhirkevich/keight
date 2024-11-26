package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.callableOrNull
import io.github.alexzhirkevich.keight.callableOrThrow
import io.github.alexzhirkevich.keight.expressions.OpConstant
import io.github.alexzhirkevich.keight.expressions.ThrowableValue
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck
import io.github.alexzhirkevich.keight.js.interpreter.typeError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlin.coroutines.cancellation.CancellationException

internal class JSPromiseFunction : JSFunction(
    name = "Promise",
    prototype = Object {
        "catch".func("onrejected") { args ->
            val value = toKotlin(thisRef) as Job
            val arg = args.getOrNull(0)
            val callable = arg.callableOrThrow(this)
            val job = toKotlin(value) as Job

            async {
                try {
                    if (job is Deferred<*>) {
                        job.await()
                    } else {
                        job.joinSuccess()
                    }
                } catch (t: CancellationException) {
                    throw t
                } catch (t: Throwable) {
                    callable.invoke(t.toJS().listOf(), this@func)
                }
            }
        }
        "then".func(
            FunctionParam("onfulfilled"),
            FunctionParam("onrejected", default = OpArgOmitted),
        ) { args ->
            val onFulfilled = args.getOrNull(0)
            val job = toKotlin(thisRef) as Job

            async {
                try {
                    val res = if (job is Deferred<*>) job.await() else job.joinSuccess()
                    onFulfilled.callableOrThrow(this@func)
                        .invoke(res.listOf(), this@func)
                } catch (t: CancellationException) {
                    throw t
                } catch (t: Throwable) {
                    args.argOrElse(1) { throw t }
                        .callableOrThrow(this@func)
                        .invoke(t.toJS().listOf(), this@func)
                }
            }
        }
        "finally".func("handle") { args ->
            val arg = args.getOrNull(0)
            val callable = arg?.callableOrNull()
            val value = fromKotlin(thisRef) as Job
            typeCheck(callable is Callable) {
                "$arg is not a function"
            }

            async {
                try {
                    value.joinSuccess()
                } finally {
                    callable.invoke(emptyList(), this@func)
                }
            }
        }
    },
    properties = listOf(
        "resolve".func( FunctionParam("value", default = OpConstant(Unit))) {
            CompletableDeferred(it[0])
        },
        "reject".func(FunctionParam("reason", default = OpConstant(Unit))) {
            val v = it[0]

            CompletableDeferred<Unit>().apply {
                completeExceptionally(
                    if (v is Throwable) v else ThrowableValue(v)
                )
            }
        },
        "all".func(FunctionParam("values", isVararg = true)) { args ->
            async {
                (args[0] as Iterable<*>).map {
                    val job = toKotlin(it)
                    typeCheck(job is Job){ "$job is not a Promise" }
                    if (job is Deferred<*>) job.await() else job.joinSuccess()
                }
            }
        }
    ).associateBy { it.name }.toMutableMap()
) {

    override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any {
        runtime.typeError { "Promise constructor cannot be invoked without 'new'" }
    }

    override suspend fun construct(args: List<Any?>, runtime: ScriptRuntime): Any {
        val resolveReject = args.getOrNull(0).callableOrThrow(runtime)

        val deferred = CompletableDeferred<Any?>()

        val resolve = { x : Any? -> deferred.complete(x); Unit }
        val reject = { x : Any? -> deferred.completeExceptionally(if (x is ThrowableValue) x else ThrowableValue(x)); Unit}

        return runtime.async {
            resolveReject.invoke(listOf(resolve, reject), runtime)
            deferred.await()
        }
    }
}

private fun Throwable.toJS()  = when (this) {
    is ThrowableValue -> value
    is JSError -> this
    else -> JSError(message, cause = this)
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