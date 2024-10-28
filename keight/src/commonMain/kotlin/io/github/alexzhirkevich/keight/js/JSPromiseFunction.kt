package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.callableOrNull
import io.github.alexzhirkevich.keight.expressions.OpConstant
import io.github.alexzhirkevich.keight.expressions.ThrowableValue
import io.github.alexzhirkevich.keight.invoke
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.jvm.JvmInline

internal class JSPromiseFunction : JSFunction(name = "Promise") {

    init {
        setPrototype(Object {
            val job = SupervisorJob()
            "catch" eq Catch(job)
            "then" eq Then(job)
            "finally" eq Finally(job)
        })
    }

    override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): Any {
        throw TypeError("Promise constructor cannot be invoked without 'new'")
    }

    override suspend fun construct(args: List<Expression>, runtime: ScriptRuntime): Any {
        val resolveReject = args.getOrNull(0)?.invoke(runtime)?.callableOrNull()

        typeCheck(resolveReject is Callable) {
            "Promise resolver is not a function"
        }

        val deferred = CompletableDeferred<Any?>()

        val resolve = { x : Any? -> deferred.complete(x); Unit }
        val reject = { x : Any? -> deferred.completeExceptionally(if (x is ThrowableValue) x else ThrowableValue(x)); Unit}

        return runtime.async {
            resolveReject.invoke(listOf(OpConstant(resolve), OpConstant(reject)), runtime)
            deferred.await()
        }
    }

    @JvmInline
    private value class Then(val value: Job) : Callable {
        override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): Any? {
            val arg = args.getOrNull(0)?.invoke(runtime)
            val callable = arg?.callableOrNull()
            typeCheck(callable is Callable) {
                "$arg is not a function"
            }

            val job = runtime.toKotlin(value) as Job
            val res = if (job is Deferred<*>) job::await else job::join

            return runtime.async {
                callable.invoke(listOf(OpConstant(res())), runtime)
            }
        }

        override suspend fun bind(args: List<Expression>, runtime: ScriptRuntime): Callable {
            return Then(args.firstJobOrEmpty(runtime))
        }
    }

    @JvmInline
    private value class Catch(val value: Job) : Callable {

        override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): Any? {
            val arg = args.getOrNull(0)?.invoke(runtime)
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
                        job.join()
                    }
                } catch (t: CancellationException) {
                    throw t
                } catch (t: Throwable) {
                    val value = when (t){
                        is ThrowableValue -> t.value
                        is JSError -> t
                        else -> JSError(t.message, t)
                    }
                    callable.invoke(listOf(OpConstant(value)), runtime)
                }
            }
        }

        override suspend fun bind(args: List<Expression>, runtime: ScriptRuntime): Callable {
            return Catch(args.firstJobOrEmpty(runtime))
        }
    }

    @JvmInline
    private value class Finally(val value: Job) : Callable {

        override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): Any? {
            val arg = args.getOrNull(0)?.invoke(runtime)
            val callable = arg?.callableOrNull()
            typeCheck(callable is Callable) {
                "$arg is not a function"
            }

            return runtime.launch {
                try {
                    value.join()
                } finally {
                    callable.invoke(emptyList(), runtime)
                }
            }
        }

        override suspend fun bind(args: List<Expression>, runtime: ScriptRuntime): Callable {
            return Finally(args.firstJobOrEmpty(runtime))
        }
    }
}

private suspend fun List<Expression>.firstJobOrEmpty(runtime: ScriptRuntime) : Job {
    return firstOrNull()?.invoke(runtime)?.let(runtime::fromKotlin) as? Job ?: Job()
}