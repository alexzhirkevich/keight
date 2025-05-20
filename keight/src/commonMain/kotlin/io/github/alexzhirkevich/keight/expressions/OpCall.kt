package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.callableOrNull
import io.github.alexzhirkevich.keight.fastMap
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.SyntaxError
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck
import io.github.alexzhirkevich.keight.js.interpreter.typeError
import io.github.alexzhirkevich.keight.js.js
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

            if ((receiver == null || receiver == Undefined) && callable.isOptional) {
                return@Expression Undefined
            }

            r.typeCheck(receiver != null) {
                "Can't get properties of $receiver"
            }

            receiver.call(
                func = callable.index.invoke(r),
                thisRef = receiver,
                args = arguments.fastMap { it(r) },
                isOptional = callable.isOptional,
                runtime = r
            )
        }

        callable is OpGetProperty && callable.receiver != null -> Expression { r ->
            val receiver = callable.receiver.invoke(r)

            if ((receiver == null || receiver == Undefined) && callable.isOptional) {
                return@Expression Undefined
            }

            r.typeCheck(receiver != null) {
                "Can't get properties (${callable.name}) of $receiver"
            }

            receiver.call(
                func = callable.name.js(),
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
    func : JsAny?,
    thisRef : JsAny?,
    args: List<JsAny?>,
    isOptional : Boolean,
    runtime: ScriptRuntime,
) : JsAny? {
    val v = get(func, runtime)
    val callable = v?.callableOrNull()

    if (callable == null && isOptional) {
        return Undefined
    }
    runtime.typeCheck(callable != null) {
        "$func is not a function"
    }
    return callable.call(thisRef, args, runtime)
}

private tailrec suspend fun OpCallImpl(
    runtime: ScriptRuntime,
    callable: Any?,
    arguments: List<JsAny?>,
    isOptional: Boolean
) : JsAny? {
    return when {
        callable is Expression -> OpCallImpl(runtime, callable(runtime), arguments, isOptional)
        callable is Callable -> callable.invoke(arguments, runtime)
        callable is Function<*> -> execKotlinFunction(runtime, callable, arguments.fastMap(runtime::toKotlin))
        isOptional && (callable == null || callable == Undefined) -> Undefined
        else -> runtime.typeError { "$callable is not a function" }
    }
}

internal fun Function<*>.asCallable() : Callable = KotlinCallable(this)

@JvmInline
private value class KotlinCallable(
    val function: Function<*>
) : Callable {
    override suspend fun bind(thisArg: JsAny?, args: List<JsAny?>, runtime: ScriptRuntime): Callable {
        return thisArg?.callableOrNull()!!
    }

    override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
        return execKotlinFunction(runtime, function, args)
    }
}

private val SUSPENDED : Any get()  =  kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED

private fun Any?.jsAnyOrUndefined() : JsAny? {
    return if (this is JsAny?) this else Undefined
}
@Suppress("unchecked_cast")
private suspend fun execKotlinFunction(
    runtime: ScriptRuntime,
    function: Function<*>,
    args: List<Any?>,
) : JsAny? {

    return when (function) {

        is Function0<*> -> (function as Function0<Any?>)
            .invoke() as? JsAny?

        is Function1<*, *> -> withInvalidArgsCheck {
            function as Function1<Any?, Any?>
            when (args.size){
                1 -> function.invoke(args[0]).jsAnyOrUndefined()
                0 -> runtime.async {
                    suspendCancellableCoroutine { cont ->
                        function.invoke(cont).also {
                            if (it != SUSPENDED){
                                cont.resume(it)
                            }
                        }.jsAnyOrUndefined()
                    }
                }.js()
                else -> notEnoughArgs()
            }
        }

        is Function2<*, *, *> -> withInvalidArgsCheck {
            function as Function2<Any?, Any?, Any?>
            when (args.size){
                2 -> function.invoke(args[0], args[1]).jsAnyOrUndefined()
                1 -> runtime.async {
                    suspendCancellableCoroutine { cont ->
                        function.invoke(cont, args[0]).also {
                            if (it != SUSPENDED){
                                cont.resume(it)
                            }
                        }.jsAnyOrUndefined()
                    }
                }.js()
                else -> notEnoughArgs()
            }
        }

        is Function3<*, *, *, *> -> withInvalidArgsCheck {
            function as Function3<Any?, Any?, Any?, Any?>
            when (args.size){
                3 -> function .invoke(args[0], args[1], args[2]).jsAnyOrUndefined()
                2 -> runtime.async {
                    suspendCancellableCoroutine { cont ->
                        function.invoke(cont, args[0], args[1]).also {
                            if (it != SUSPENDED){
                                cont.resume(it)
                            }
                        }
                    }.jsAnyOrUndefined()
                }.js()
                else -> notEnoughArgs()
            }
        }

        is Function4<*, *, *, *, *> -> withInvalidArgsCheck {
            function as Function4<Any?, Any?, Any?, Any?, Any?>
            when (args.size) {
                4 -> function.invoke(args[0], args[1], args[2], args[3]).jsAnyOrUndefined()
                3 -> runtime.async {
                    suspendCancellableCoroutine { cont ->
                        function.invoke(cont, args[0], args[1], args[2]).also {
                            if (it != SUSPENDED){
                                cont.resume(it)
                            }
                        }.jsAnyOrUndefined()
                    }
                }.js()

                else -> notEnoughArgs()
            }
        }

        is Function5<*, *, *, *, *, *> -> withInvalidArgsCheck {
            function as Function5<Any?, Any?, Any?, Any?, Any?, Any?>
            when (args.size) {
                5 -> function.invoke(args[0], args[1], args[2], args[3], args[4])
                    .jsAnyOrUndefined()
                4 -> runtime.async {
                    suspendCancellableCoroutine { cont ->
                        function.invoke(cont, args[0], args[1], args[2], args[3]).also {
                            if (it != SUSPENDED){
                                cont.resume(it)
                            }
                        }.jsAnyOrUndefined()
                    }
                }.js()

                else -> notEnoughArgs()
            }
        }

        is Function6<*, *, *, *, *, *, *> -> withInvalidArgsCheck {
            function as Function6<Any?, Any?, Any?, Any?, Any?, Any?, Any?>
            when (args.size) {
                6 -> function.invoke(args[0], args[1], args[2], args[3], args[4], args[6])
                    .jsAnyOrUndefined()
                5 -> runtime.async<Any?> {
                    suspendCancellableCoroutine { cont ->
                        function.invoke(cont, args[0], args[1], args[2], args[3], args[4]).also {
                            if (it != SUSPENDED){
                                cont.resume(it)
                            }
                        }.jsAnyOrUndefined()
                    }
                }.js()

                else -> notEnoughArgs()
            }
        }
        is Function7<*, *, *, *, *, *, *, *> -> withInvalidArgsCheck {
            function as Function7<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?>
            when (args.size) {
                7 -> function.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6])
                    .jsAnyOrUndefined()
                6 -> runtime.async {
                    suspendCancellableCoroutine { cont ->
                        function.invoke(cont, args[0], args[1], args[2], args[3], args[4], args[5]).also {
                            if (it != SUSPENDED){
                                cont.resume(it)
                            }
                        }.jsAnyOrUndefined()
                    }
                }.js()

                else -> notEnoughArgs()
            }
        }

        is Function8<*, *, *, *, *, *, *, *, *> -> withInvalidArgsCheck {
            function as Function8<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?>
            when (args.size) {
                8 -> function.invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7])
                    .jsAnyOrUndefined()
                7 -> runtime.async {
                    suspendCancellableCoroutine { cont ->
                        function.invoke(cont, args[0], args[1], args[2], args[3], args[4], args[5], args[6]).also {
                            if (it != SUSPENDED){
                                cont.resume(it)
                            }
                        }.jsAnyOrUndefined()
                    }
                }.js()

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

