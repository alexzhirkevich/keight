package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.Named
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.argForNameOrIndex
import io.github.alexzhirkevich.keight.Constructor
import io.github.alexzhirkevich.keight.expressions.BlockReturn
import io.github.alexzhirkevich.keight.expressions.OpConstant
import io.github.alexzhirkevich.keight.fastForEachIndexed
import io.github.alexzhirkevich.keight.fastMap
import io.github.alexzhirkevich.keight.invoke
import io.github.alexzhirkevich.keight.js.interpreter.syntaxCheck
import kotlinx.coroutines.async

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
    val parameters : List<FunctionParam> = emptyList(),
    val body : Expression = OpConstant(Unit),
    val isAsync : Boolean = false,
    val isArrow : Boolean = false,
    properties : MutableMap<Any?, Any?> = mutableMapOf(),
    prototype : Any? = JSObjectImpl()
) : JSObjectImpl(name, properties), Callable, Named, Constructor {

    override val type: String get() = "function"

    var thisRef : Any? = null
        private set


    init {
        properties.forEach {
            val v = it.value
            if (v is JSFunction && !v.isArrow) {
                v.thisRef = this
            }
        }

        val varargs = parameters.count { it.isVararg }

        if (varargs > 1 || varargs == 1 && !parameters.last().isVararg) {
            throw SyntaxError("Rest parameter must be last formal parameter")
        }
        setPrototype(prototype)
    }

    override suspend fun get(property: Any?, runtime: ScriptRuntime): Any? {
        return when {
            super<JSObjectImpl>.contains(property, runtime) ->
                super<JSObjectImpl>.get(property, runtime)

            else -> super<Callable>.get(property, runtime)
        }
    }

    fun copy(
        body: Expression = this.body,
        isAsync: Boolean = this.isAsync,
        prototype: Any? = Unit
    ): JSFunction {
        return JSFunction(
            name = name,
            parameters = parameters,
            body = body,
            isArrow = isArrow,
            isAsync = isAsync,
            prototype = prototype
        )
    }

    override suspend fun isInstance(obj : Any?, runtime: ScriptRuntime) : Boolean {
        return (obj as? JsAny)?.get(PROTO,runtime) === get(PROTOTYPE, runtime)
    }

    override suspend fun bind(args: List<Expression>, runtime: ScriptRuntime): Callable {
        return copy().apply {
            if (!isArrow) {
                thisRef = args[0](runtime)
            }
        }
    }


    override suspend fun construct(args: List<Expression>, runtime: ScriptRuntime): Any {
        syntaxCheck(!isArrow){
            "Can't use 'new' keyword with arrow functions"
        }

        val obj = JSObjectImpl().apply {
            setProto(this@JSFunction.get(PROTOTYPE, runtime))
        }

        copy().apply {
            thisRef = obj
            invoke(args, runtime)
        }
        return obj
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

        return if (isAsync){
            runtime.async {
                invokeImpl(runtime, arguments)
            }
        } else {
            invokeImpl(runtime, arguments)
        }
    }

    private suspend fun invokeImpl(
        runtime: ScriptRuntime,
        arguments: Map<String, Pair<VariableType,Any?>>
    ) : Any? {
        return try {
            runtime.withScope(
                extraVariables = arguments,
                isSuspendAllowed = isAsync,
                block = body::invoke
            )
        } catch (ret: BlockReturn) {
            ret.value
        }
    }
}


