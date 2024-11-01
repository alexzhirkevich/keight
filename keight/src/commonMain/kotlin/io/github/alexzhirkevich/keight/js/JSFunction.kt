package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.Named
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.Constructor
import io.github.alexzhirkevich.keight.expressions.BlockReturn
import io.github.alexzhirkevich.keight.expressions.OpConstant
import io.github.alexzhirkevich.keight.fastForEachIndexed
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

public open class JSFunction(
    override val name : String = "",
    internal val parameters : List<FunctionParam> = emptyList(),
    internal val body : Expression = OpConstant(Unit),
    internal val isAsync : Boolean = false,
    private val isArrow : Boolean = false,
    private val superConstructor : Constructor? = null,
    private val prototype : Any? = JSObjectImpl(),
    properties : MutableMap<Any?, Any?> = mutableMapOf(),
) : JSObjectImpl(name, properties), Callable, Named, Constructor {

    override val type: String get() = "function"

    private val superConstructorPropertyMap =
        if (superConstructor != null) {
            mapOf("super" to Pair(VariableType.Const, superConstructor))
        } else {
            emptyMap()
        }

    final override var thisRef : Any? = null
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
        return super<JSObjectImpl>.get(property, runtime)
//        return when {
//            property in properties -> super<JSObjectImpl>.get(property, runtime)
//            else -> super<Callable>.get(property, runtime)
//        }
    }

    internal fun copy(
        isAsync: Boolean = this.isAsync,
        body: Expression = this.body,
    ): JSFunction {
        return JSFunction(
            name = name,
            parameters = parameters,
            body = body,
            isArrow = isArrow,
            isAsync = isAsync,
            prototype = prototype,
            superConstructor = superConstructor
        )
    }

    override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
        return copy().apply {
            if (!isArrow) {
                thisRef = thisArg
            }
        }
    }


    override suspend fun construct(args: List<Any?>, runtime: ScriptRuntime): Any {
        syntaxCheck(!isArrow) {
            "Can't use 'new' keyword with arrow functions"
        }

        val obj = JSObjectImpl().apply {
            setProto(this@JSFunction.get(PROTOTYPE, runtime))
        }
        with(copy()) {
            thisRef = obj
            invoke(args, runtime, superConstructorPropertyMap)
        }
        return obj
    }

    private suspend fun invoke(
        args: List<Any?>,
        runtime: ScriptRuntime,
        extraArgs : Map<String, Pair<VariableType, Any?>>
    ): Any? {
        val arguments = buildMap {
            parameters.fastForEachIndexed { i, p ->
                val value = if (p.isVararg) {
                    args.drop(i)
                } else {
                    (args.getOrElse(i) { p.default?.invoke(runtime) }) ?: Unit
                }
                this[p.name] = VariableType.Local to value
            }
            thisRef?.let {
                this["this"] = VariableType.Const to it
            }
            extraArgs.forEach {
                this[it.key] = it.value
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

    override suspend fun invoke(
        args: List<Any?>,
        runtime: ScriptRuntime,
    ): Any? = invoke(args, runtime, emptyMap())

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

    override fun toString(): String {
        return "[function $name]"
    }
}


