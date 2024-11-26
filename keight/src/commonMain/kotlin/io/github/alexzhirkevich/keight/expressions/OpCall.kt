package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.callableOrNull
import io.github.alexzhirkevich.keight.fastMap
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.SyntaxError
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck
import io.github.alexzhirkevich.keight.js.interpreter.typeError
import kotlinx.coroutines.async
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.jvm.JvmInline

internal fun OpCall(
    callable : Expression,
    arguments : List<Expression>,
    isOptional : Boolean
) : Expression {

    return when {
        callable is OpIndex -> Expression { r ->
            val receiver = callable.receiver.invoke(r)

            if ((receiver == null || receiver == Unit) && callable.isOptional) {
                return@Expression Unit
            }
            (receiver as JsAny).call(
                func = callable.index(r),
                thisRef = receiver,
                args = arguments.fastMap() { it(r) },
                isOptional = callable.isOptional,
                runtime = r
            )
        }

        callable is OpGetProperty && callable.receiver != null -> Expression { r ->
            val receiver = callable.receiver.invoke(r)
            if ((receiver !is JsAny) && callable.isOptional) {
                return@Expression Unit
            }

            r.typeCheck (receiver is JsAny) {
                "Can't get properties (${callable.name}) of $receiver"
            }
            receiver.call(
                func = callable.name,
                thisRef = receiver,
                args = arguments.fastMap { it(r) },
                isOptional = callable.isOptional,
                runtime = r
            )
        }

        else -> Expression { r ->
            OpCallImpl(
                runtime = r,
                callable = callable,
                arguments = arguments.fastMap { it(r) },
                isOptional = isOptional
            )
        }
    }
}
internal suspend fun JsAny.call(
    func : Any?,
    thisRef : Any?,
    args: List<Any?>,
    isOptional : Boolean,
    runtime: ScriptRuntime,
) : Any? {
    val v = get(func, runtime)
    val callable = v?.callableOrNull()
    if (callable == null && isOptional) {
        return Unit
    }
    runtime.typeCheck(callable != null) {
        "$func is not a function"
    }
    return callable.call(thisRef, args, runtime)
}

private tailrec suspend fun OpCallImpl(
    runtime: ScriptRuntime,
    callable: Any?,
    arguments: List<Any?>,
    isOptional: Boolean
) : Any? {
    return when {
        callable is Expression -> OpCallImpl(runtime, callable(runtime), arguments, isOptional)
        callable is Callable -> callable.invoke(arguments, runtime)
        callable is Function<*> -> execKotlinFunction(runtime, callable, arguments.fastMap(runtime::toKotlin))
        isOptional && (callable == null || callable == Unit) -> Unit
        else -> runtime.typeError { "$callable is not a function" }
    }
}

internal fun Function<*>.asCallable() : Callable = KotlinCallable(this)

@JvmInline
private value class KotlinCallable(
    val function: Function<*>
) : Callable {
    override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
        return thisArg?.callableOrNull()!!
    }

    override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
        return execKotlinFunction(runtime, function, args)
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

