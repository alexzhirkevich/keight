package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.Named
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.argAt
import io.github.alexzhirkevich.keight.argForNameOrIndex
import io.github.alexzhirkevich.keight.Constructor
import io.github.alexzhirkevich.keight.expressions.BlockReturn
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

internal open class JSFunction(
    override val name : String,
    val parameters : List<FunctionParam>,
    val body : Expression,
    var thisRef : Any? = null,
    val isClassMember : Boolean = false,
    var isStatic : Boolean = false,
    val extraVariables: Map<String, Pair<VariableType, Any?>> = emptyMap(),
) : JSObjectImpl(name), Callable, Named, Constructor {

    override val type: String
        get() = "function"

    init {
        val varargs = parameters.count { it.isVararg }

        if (varargs > 1 || varargs == 1 && !parameters.last().isVararg) {
            throw SyntaxError("Rest parameter must be last formal parameter")
        }
    }

    fun copy(
        body: Expression = this.body,
        extraVariables: Map<String, Pair<VariableType, Any?>> = emptyMap(),
    ): JSFunction {
        return JSFunction(
            name = name,
            parameters = parameters,
            body = body,
            thisRef = thisRef,
            isClassMember = isClassMember,
            extraVariables = this.extraVariables + extraVariables
        )
    }

    override fun construct(args: List<Expression>, runtime: ScriptRuntime): Any {
        return copy().apply {
            thisRef = this
            setPrototype(this@JSFunction)
            invoke(args, runtime)
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


