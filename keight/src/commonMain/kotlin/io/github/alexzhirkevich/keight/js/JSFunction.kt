package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.Named
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.argAt
import io.github.alexzhirkevich.keight.argForNameOrIndex
import io.github.alexzhirkevich.keight.Constructor
import io.github.alexzhirkevich.keight.expressions.BlockReturn
import io.github.alexzhirkevich.keight.expressions.asCallable
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

internal open class JSFunction(
    override val name : String,
    val parameters : List<FunctionParam>,
    val body : Expression,
    var thisRef : Any? = null,
    val isClassMember : Boolean = false,
    var isStatic : Boolean = false,
    val isArrow : Boolean = false,
    val extraVariables: Map<String, Pair<VariableType, Any?>> = emptyMap(),
    prototype : Any? = JSObjectImpl()
) : JSObjectImpl(name), Callable, Named, Constructor {

    override suspend fun get(property: Any?, runtime: ScriptRuntime): Any? {
        return when {
            super<JSObjectImpl>.contains(property, runtime) ->
                super<JSObjectImpl>.get(property, runtime)

            else -> super<Callable>.get(property, runtime)
        }
    }

    override val type: String
        get() = "function"

    init {
        val varargs = parameters.count { it.isVararg }

        if (varargs > 1 || varargs == 1 && !parameters.last().isVararg) {
            throw SyntaxError("Rest parameter must be last formal parameter")
        }
        setPrototype(prototype)
    }

    override suspend fun bind(args: List<Expression>, runtime: ScriptRuntime): Callable {
        return copy().apply {
            if (!isArrow) {
                thisRef = args[0](runtime)
            }
        }
    }

    fun copy(
        body: Expression = this.body,
        extraVariables: Map<String, Pair<VariableType, Any?>> = emptyMap(),
        prototype: Any? = Unit
    ): JSFunction {
        return JSFunction(
            name = name,
            parameters = parameters,
            body = body,
            thisRef = thisRef,
            isClassMember = isClassMember,
            isArrow = isArrow,
            extraVariables = this.extraVariables + extraVariables,
            prototype = prototype
        )
    }

    override suspend fun isInstance(obj : Any?, runtime: ScriptRuntime) : Boolean {
        return (obj as? JsAny)?.get(PROTO,runtime) === get(PROTOTYPE, runtime)
    }

    override suspend fun construct(args: List<Expression>, runtime: ScriptRuntime): Any {
        return copy().apply {
            thisRef = this
            setProto(this@JSFunction.get(PROTOTYPE, runtime))
            invoke(args, runtime)
        }
    }

    override suspend fun invoke(
        args: List<Expression>,
        runtime: ScriptRuntime,
    ): Any? {
        val arguments = buildMap {
            parameters.fastForEachIndexed { i, p ->
                val value = if (p.isVararg) {
                    args.drop(i).fastMap { it.invoke(runtime) }
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


