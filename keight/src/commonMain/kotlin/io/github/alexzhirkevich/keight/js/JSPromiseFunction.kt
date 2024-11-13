package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.callableOrNull
import io.github.alexzhirkevich.keight.expressions.OpConstant
import io.github.alexzhirkevich.keight.expressions.ThrowableValue
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlin.coroutines.cancellation.CancellationException
import kotlin.jvm.JvmInline

internal class JSPromiseFunction : JSFunction(
    name = "Promise",
    prototype = Object {
        "catch" eq Catch(Job())
        "then" eq Then(Job())
        "finally" eq Finally(Job())
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
        throw TypeError("Promise constructor cannot be invoked without 'new'")
    }

    override suspend fun construct(args: List<Any?>, runtime: ScriptRuntime): Any {
        val resolveReject = args.getOrNull(0)?.callableOrNull()

        typeCheck(resolveReject is Callable) {
            "Promise resolver is not a function"
        }

        val deferred = CompletableDeferred<Any?>()

        val resolve = { x : Any? -> deferred.complete(x); Unit }
        val reject = { x : Any? -> deferred.completeExceptionally(if (x is ThrowableValue) x else ThrowableValue(x)); Unit}

        return runtime.async {
            resolveReject.invoke(listOf(resolve, reject), runtime)
            deferred.await()
        }
    }

    @JvmInline
    private value class Then(val value: Job) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Deferred<*> {
            val arg = args.getOrNull(0)
            val callable = arg?.callableOrNull()
            typeCheck(callable is Callable) {
                "$arg is not a function"
            }

            val job = runtime.toKotlin(value) as Job
            val res = if (job is Deferred<*>) job::await else job::join

            return runtime.async {
                callable.invoke(res().listOf(), runtime)
            }
        }

        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return Then((thisArg as? Job) ?: Job())
        }
    }

    @JvmInline
    private value class Catch(val value: Job) : Callable {

        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Deferred<*> {
            val arg = args.getOrNull(0)
            val callable = arg?.callableOrNull()
            typeCheck(callable is Callable) {
                "$arg is not a function"
            }

            val job = runtime.toKotlin(value) as Job

            return runtime.async {
                try {
                    if (job is Deferred<*>) {
                        job.await()
                    } else {
                        job.joinSuccess()
                    }
                } catch (t: CancellationException) {
                    throw t
                } catch (t: Throwable) {
                    val value = when (t){
                        is ThrowableValue -> t.value
                        is JSError -> t
                        else -> JSError(t.message, cause = t)
                    }
                    callable.invoke(value.listOf(), runtime)
                }
            }
        }

        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return Catch((thisArg as? Job) ?: Job())
        }
    }

    @JvmInline
    private value class Finally(val value: Job) : Callable {

        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Deferred<Unit> {
            val arg = args.getOrNull(0)
            val callable = arg?.callableOrNull()
            typeCheck(callable is Callable) {
                "$arg is not a function"
            }

            return runtime.async {
                try {
                    value.joinSuccess()
                } finally {
                    callable.invoke(emptyList(), runtime)
                }
            }
        }

        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return Finally((thisArg as? Job) ?: Job())
        }
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