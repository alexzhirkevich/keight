package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.Callable
import io.github.alexzhirkevich.keight.js.TypeError
import io.github.alexzhirkevich.keight.fastMap
import io.github.alexzhirkevich.keight.invoke

internal fun OpCall(
    callable : Expression,
    arguments : List<Expression>
) = Expression {
    OpCallImpl(it, callable, arguments)
}

private tailrec fun OpCallImpl(
    runtime: ScriptRuntime,
    callable: Any?,
    arguments: List<Expression>
) : Any? {
    return when (callable) {
        is Expression -> OpCallImpl(runtime, callable(runtime), arguments)
        is Callable -> callable.invoke(arguments, runtime)
        is Function<*> -> execKotlinFunction(callable, arguments.fastMap { runtime.toKotlin(it(runtime)) })
        else -> throw TypeError("$callable is not a function")
    }
}

@Suppress("unchecked_cast")
private fun execKotlinFunction(
    function: Function<*>,
    args: List<Any?>,
) : Any? {
    return when (function) {

        is Function0<*> -> (function as Function0<Any?>)
            .invoke()

        is Function1<*, *> -> (function as Function1<Any?, Any?>)
            .invoke(args[0])

        is Function2<*, *, *> -> (function as Function2<Any?, Any?, Any?>)
            .invoke(args[0], args[1])

        is Function3<*, *, *, *> -> (function as Function3<Any?, Any?, Any?, Any?>)
            .invoke(args[0], args[1], args[2])

        is Function4<*, *, *, *, *> -> (function as Function4<Any?, Any?, Any?, Any?, Any?>)
            .invoke(args[0], args[1], args[2], args[3])

        is Function5<*, *, *, *, *, *> -> (function as Function5<Any?, Any?, Any?, Any?, Any?, Any?>)
            .invoke(args[0], args[1], args[2], args[3], args[4])

        is Function6<*, *, *, *, *, *, *> -> (function as Function6<Any?, Any?, Any?, Any?, Any?, Any?, Any?>)
            .invoke(args[0], args[1], args[2], args[3], args[4], args[5])

        is Function7<*, *, *, *, *, *, *, *> -> (function as Function7<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?>)
            .invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6])

        is Function8<*, *, *, *, *, *, *, *, *> -> (function as Function8<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?>)
            .invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7])

        is Function9<*, *, *, *, *, *, *, *, *, *> -> (function as Function9<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?>)
            .invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8])

        is Function10<*, *, *, *, *, *, *, *, *, *, *> -> (function as Function10<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?>)
            .invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9])

        else -> error("${function::class.simpleName} has too many arguments to be called from JS")
    }
}


