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
        "catch".js().func("onrejected") { args ->
            val arg = args.getOrNull(0)
            val callable = arg.callableOrThrow(this)
            val job = thisRef.toKotlin(this) as Job

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
                    callable.invoke(t.js().listOf(), this@func)
                }
            }.js()
        }
        "then".js().func(
            FunctionParam("onfulfilled"),
            FunctionParam("onrejected", default = OpArgOmitted),
        ) { args ->
            val onFulfilled = args.getOrNull(0)
            val job = thisRef.toKotlin(this) as Job

            async {
                try {
                    val res = if (job is Deferred<*>) {
                        job.await() as JsAny?
                    } else {
                        job.joinSuccess()
                        Undefined
                    }
                    onFulfilled.callableOrThrow(this@func)
                        .invoke(res.listOf(), this@func)
                } catch (t: CancellationException) {
                    throw t
                } catch (t: Throwable) {
                    args.argOrElse(1) { throw t }
                        .callableOrThrow(this@func)
                        .invoke(t.js().listOf(), this@func)
                }
            }.js()
        }
        "finally".js().func("handle") { args ->
            val arg = args.getOrNull(0)
            val callable = arg?.callableOrNull()
            val value = thisRef as Job
            typeCheck(callable is Callable) {
                "$arg is not a function".js()
            }

            async {
                try {
                    value.joinSuccess()
                    Undefined
                } finally {
                    callable.invoke(emptyList(), this@func)
                }
            }.js()
        }
    },
    properties = listOf(
        "resolve".func( FunctionParam("value", default = OpConstant(Undefined))) {
            CompletableDeferred(it[0]).js()
        },
        "reject".func(FunctionParam("reason", default = OpConstant(Undefined))) {
            val v = it[0]

            CompletableDeferred<Undefined>().apply {
                completeExceptionally(
                    if (v is Throwable) v else ThrowableValue(v)
                )
            }.js()
        },
        "all".func("values".vararg()) { args ->
            async {
                (args[0] as Iterable<JsAny?>).map {
                    val job = it?.toKotlin(this@func)
                    typeCheck(job is Job){ "$job is not a Promise".js() }
                    if (job is Deferred<*>) {
                        job.await() as JsAny
                    } else {
                        job.joinSuccess()
                        Undefined
                    }
                }.js()
            }.js()
        }
    ).associateBy { it.name.js() }.toMutableMap()
) {

    override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
        runtime.typeError { "Promise constructor cannot be invoked without 'new'".js() }
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
                if (x is ThrowableValue) x else ThrowableValue(x)
            ); Undefined
        }

        return runtime.async {
            resolveReject.invoke(listOf(resolve, reject), runtime)
            deferred.await()
        }.js()
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