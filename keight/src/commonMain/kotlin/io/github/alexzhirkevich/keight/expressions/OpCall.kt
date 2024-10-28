package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.js.TypeError
import io.github.alexzhirkevich.keight.fastMap
import io.github.alexzhirkevich.keight.invoke
import io.github.alexzhirkevich.keight.js.SyntaxError
import kotlinx.coroutines.async
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.jvm.JvmInline

internal fun OpCall(
    callable : Expression,
    arguments : List<Expression>,
    isOptional : Boolean
) = Expression {
    OpCallImpl(
        runtime = it,
        callable = callable,
        arguments = arguments,
        isOptional = isOptional
    )
}

private tailrec suspend fun OpCallImpl(
    runtime: ScriptRuntime,
    callable: Any?,
    arguments: List<Expression>,
    isOptional: Boolean
) : Any? {
    return when {
        callable is Expression -> OpCallImpl(runtime, callable(runtime), arguments, isOptional)
        callable is Callable -> callable.invoke(arguments, runtime)
        callable is Function<*> -> execKotlinFunction(runtime, callable, arguments.fastMap { runtime.toKotlin(it(runtime)) })
        isOptional && (callable == null || callable == Unit) -> Unit
        else -> throw TypeError("$callable is not a function")
    }
}

internal fun Function<*>.asCallable() : Callable = KotlinCallable(this)

@JvmInline
private value class KotlinCallable(
    val function: Function<*>
) : Callable {
    override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): Any? {
        return execKotlinFunction(runtime, function, args.fastMap { it(runtime) })
    }
}

private val SUSPENDED : Any get()  =  kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED

@Suppress("unchecked_cast")
private suspend fun execKotlinFunction(
    runtime: ScriptRuntime,
    function: Function<*>,
    args: List<Any?>,
) : Any? {

    return when (function) {

        is Function0<*> -> (function as Function0<Any?>)
            .invoke()

        is Function1<*, *> -> withInvalidArgsCheck {
            function as Function1<Any?, Any?>
            when (args.size){
                1 -> function.invoke(args[0])
                0 -> runtime.async<Any?> {
                    suspendCancellableCoroutine { cont ->
                        function.invoke(cont).also {
                            if (it != SUSPENDED){
                                cont.resume(it)
                            }
                        }
                    }
                }
                else -> notEnoughArgs()
            }
        }

        is Function2<*, *, *> -> withInvalidArgsCheck {
            function as Function2<Any?, Any?, Any?>
            when (args.size){
                2 -> function.invoke(args[0], args[1])
                1 -> runtime.async<Any?> {
                    suspendCancellableCoroutine { cont ->
                        function.invoke(cont, args[0]).also {
                            if (it != SUSPENDED){
                                cont.resume(it)
                            }
                        }
                    }
                }
                else -> notEnoughArgs()
            }
        }

        is Function3<*, *, *, *> -> withInvalidArgsCheck {
            function as Function3<Any?, Any?, Any?, Any?>
            when (args.size){
                3 -> function .invoke(args[0], args[1], args[2])
                2 -> runtime.async<Any?> {
                    suspendCancellableCoroutine { cont ->
                        function.invoke(cont, args[0], args[1]).also {
                            if (it != SUSPENDED){
                                cont.resume(it)
                            }
                        }
                    }
                }
                else -> notEnoughArgs()
            }
        }

        is Function4<*, *, *, *, *> -> withInvalidArgsCheck {
            function as Function4<Any?, Any?, Any?, Any?, Any?>
            when (args.size) {
                4 -> function.invoke(args[0], args[1], args[2], args[3])
                3 -> runtime.async<Any?> {
                    suspendCancellableCoroutine { cont ->
                        function.invoke(cont, args[0], args[1], args[2]).also {
                            if (it != SUSPENDED){
                                cont.resume(it)
                            }
                        }
                    }
                }

                else -> notEnoughArgs()
            }
        }

        is Function5<*, *, *, *, *, *> -> withInvalidArgsCheck {
            function as Function5<Any?, Any?, Any?, Any?, Any?, Any?>
            when (args.size) {
                5 -> function.invoke(args[0], args[1], args[2], args[3], args[4])
                4 -> runtime.async<Any?> {
                    suspendCancellableCoroutine { cont ->
                        function.invoke(cont, args[0], args[1], args[2], args[3]).also {
                            if (it != SUSPENDED){
                                cont.resume(it)
                            }
                        }
                    }
                }

                else -> notEnoughArgs()
            }
        }

        is Function6<*, *, *, *, *, *, *> -> withInvalidArgsCheck {
            function as Function6<Any?, Any?, Any?, Any?, Any?, Any?, Any?>
            when (args.size) {
                6 -> function.invoke(args[0], args[1], args[2], args[3], args[4], args[6])
                5 -> runtime.async<Any?> {
                    suspendCancellableCoroutine { cont ->
                        function.invoke(cont, args[0], args[1], args[2], args[3], args[4]).also {
                            if (it != SUSPENDED){
                                cont.resume(it)
                            }
                        }
                    }
                }

                else -> notEnoughArgs()
            }
        }
        is Function7<*, *, *, *, *, *, *, *> -> withInvalidArgsCheck {
            function as Function7<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?>
            when (args.size) {
                7 -> function.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6])
                6 -> runtime.async<Any?> {
                    suspendCancellableCoroutine { cont ->
                        function.invoke(cont, args[0], args[1], args[2], args[3], args[4], args[5]).also {
                            if (it != SUSPENDED){
                                cont.resume(it)
                            }
                        }
                    }
                }

                else -> notEnoughArgs()
            }
        }

        is Function8<*, *, *, *, *, *, *, *, *> -> withInvalidArgsCheck {
            function as Function8<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?>
            when (args.size) {
                8 -> function.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7])
                7 -> runtime.async<Any?> {
                    suspendCancellableCoroutine { cont ->
                        function.invoke(cont, args[0], args[1], args[2], args[3], args[4], args[5], args[6]).also {
                            if (it != SUSPENDED){
                                cont.resume(it)
                            }
                        }
                    }
                }

                else -> notEnoughArgs()
            }
        }

        else -> error("${function::class.simpleName} has too many arguments to be called from JS")
    }
}

private inline fun <T> withInvalidArgsCheck(block : () -> T): T {
    return try {
        block()
    } catch (c: ClassCastException) {
        if (c.message?.contains("Continuation", ignoreCase = true) == false) {
            notEnoughArgs()
        } else {
            throw c
        }
    }
}

private fun notEnoughArgs() : Nothing = throw SyntaxError("Not enough arguments passed to the function call")

