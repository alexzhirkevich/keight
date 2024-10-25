package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.Named
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.argAt
import io.github.alexzhirkevich.keight.argForNameOrIndex
import io.github.alexzhirkevich.keight.es.ESObject
import io.github.alexzhirkevich.keight.es.ESObjectBase
import io.github.alexzhirkevich.keight.es.SyntaxError
import io.github.alexzhirkevich.keight.es.TypeError
import io.github.alexzhirkevich.keight.fastForEachIndexed
import io.github.alexzhirkevich.keight.fastMap
import io.github.alexzhirkevich.keight.invoke

public class FunctionParam(
    public val name : String,
    public val isVararg : Boolean = false,
    public val default : Expression? = null
)

internal infix fun String.defaults(default: Expression?) : FunctionParam {
    return FunctionParam(this, false, default)
}

public fun interface Callable {
    public operator fun invoke(args: List<Expression>, runtime: ScriptRuntime) : Any?
}

public fun callable(
    block : () -> Any?
) : Callable = Callable { _, _ ->
    block()
}

@Suppress("unchecked_cast")
public fun <T1> callable(
    block : (t1 : T1) -> Any?
) : Callable = Callable { args, runtime ->
    block(
        runtime.fromKotlin(args.argAt(0)) as T1
    )
}

@Suppress("unchecked_cast")
public fun <T1,T2> callable(
    block : (t1 : T1, t2 : T2) -> Any?
) : Callable = Callable { args, runtime ->
    block(
        runtime.fromKotlin(args.argAt(0)) as T1,
        runtime.fromKotlin(args.argAt(1)) as T2,
    )
}

@Suppress("unchecked_cast")
public fun <T1,T2,T3> callable(
    block : (t1 : T1, t2 : T2, t3: T3) -> Any?
) : Callable = Callable { args, runtime ->
    block(
        runtime.fromKotlin(args.argAt(0)) as T1,
        runtime.fromKotlin(args.argAt(1)) as T2,
        runtime.fromKotlin(args.argAt(2)) as T3,
    )
}

@Suppress("unchecked_cast")
public fun <T1,T2,T3,T4> callable(
    block : (t1 : T1, t2 : T2, t3: T3, t4: T4) -> Any?
) : Callable = Callable { args, runtime ->
    block(
        runtime.fromKotlin(args.argAt(0)) as T1,
        runtime.fromKotlin(args.argAt(1)) as T2,
        runtime.fromKotlin(args.argAt(2)) as T3,
        runtime.fromKotlin(args.argAt(3)) as T4,
    )
}

@Suppress("unchecked_cast")
public fun <T1,T2,T3,T4, T5> callable(
    block : (t1 : T1, t2 : T2, t3: T3, t4: T4, t5 : T5) -> Any?
) : Callable = Callable { args, runtime ->
    block(
        runtime.fromKotlin(args.argAt(0)) as T1,
        runtime.fromKotlin(args.argAt(1)) as T2,
        runtime.fromKotlin(args.argAt(2)) as T3,
        runtime.fromKotlin(args.argAt(3)) as T4,
        runtime.fromKotlin(args.argAt(3)) as T5,
    )
}

internal class Function(
    override val name : String,
    val parameters : List<FunctionParam>,
    val body : Expression,
    var thisRef : Any? = null,
    val isClassMember : Boolean = false,
    var isStatic : Boolean = false,
    val extraVariables: Map<String, Pair<VariableType, Any?>> = emptyMap(),
) : ESObject by ESObjectBase(name), Callable, Named {
    override val type: String
        get() = "function"

    fun copy(
        body: Expression = this.body,
        extraVariables: Map<String, Pair<VariableType, Any?>> = emptyMap(),
    ): Function {
        return Function(
            name = name,
            parameters = parameters,
            body = body,
            thisRef = thisRef,
            isClassMember = isClassMember,
            extraVariables = this.extraVariables + extraVariables
        )
    }

    init {
        val varargs = parameters.count { it.isVararg }

        if (varargs > 1 || varargs == 1 && !parameters.last().isVararg) {
            throw SyntaxError("Rest parameter must be last formal parameter")
        }
    }

    override fun invoke(
        args: List<Expression>,
        runtime: ScriptRuntime,
    ): Any? {
        val arguments = buildMap {
            parameters.fastForEachIndexed { i, p ->
                val value = if (p.isVararg) {
                    args.drop(i).fastMap { it(runtime) }
                } else {
                    (args.argForNameOrIndex(i, p.name) ?: p.default)
                        ?.invoke(runtime)
                        ?: Unit
                }
                this[p.name] = VariableType.Local to value
            }
            thisRef?.let {
                this["this"] = VariableType.Const to it
            }
        }
        return try {
            runtime.withScope(arguments + extraVariables, body::invoke)
        } catch (ret: BlockReturn) {
            ret.value
        }
    }
}

internal fun OpExec(
    callable : Expression,
    arguments : List<Expression>
) = Expression { r->
    OpExecImpl(r, callable, arguments)
}

private fun OpExecImpl(
    runtime: ScriptRuntime,
    callable: Any?,
    arguments: List<Expression>
) : Any? {
    return when (callable) {
        is Expression -> OpExecImpl(runtime, callable(runtime), arguments)
        is Callable -> callable.invoke(arguments, runtime)
        is kotlin.Function<*> -> execKotlinFunction(callable, arguments.fastMap { runtime.toKotlin(it(runtime)) })
        else -> throw TypeError("$callable is not a function")
    }
}


@Suppress("unchecked_cast")
private fun execKotlinFunction(
    function: kotlin.Function<*>,
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

        else -> error("${function::class.simpleName} has too many arguments to be called from JS")
    }
}

