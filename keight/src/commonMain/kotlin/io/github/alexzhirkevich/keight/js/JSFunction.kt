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
import io.github.alexzhirkevich.keight.js.interpreter.syntaxCheck
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

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

    private var thisRef : Any? = null
    private var isMutableThisRef = !isArrow
    private var bindedArgs = emptyList<Any?>()

    private val superConstructorPropertyMap =
        if (superConstructor != null) {
            mapOf("super" to Pair(VariableType.Const, superConstructor))
        } else {
            emptyMap()
        }

    init {
        properties.forEach {
            val v = it.value
            if (v is JSFunction && isMutableThisRef) {
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
            property in properties -> super<JSObjectImpl>.get(property, runtime)
            else -> super<Callable>.get(property, runtime)
        }
    }

    internal fun copy(
        isAsync: Boolean = this.isAsync,
    ): JSFunction = JSFunction(
        name = name,
        parameters = parameters,
        body = body,
        isArrow = isArrow,
        isAsync = isAsync,
        superConstructor = superConstructor,
        prototype = prototype,
    )

    override suspend fun call(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Any? {
        return invoke(
            args = args,
            runtime = runtime,
            extraArgs = emptyMap(),
            thisRef = if (isMutableThisRef) thisArg else thisRef
        )
    }

    override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
        return copy().also {
            it.thisRef = if (isMutableThisRef) thisArg else thisRef
            it.isMutableThisRef = false
            it.bindedArgs = bindedArgs + args
        }
    }

    override suspend fun construct(args: List<Any?>, runtime: ScriptRuntime): Any {
        syntaxCheck(isMutableThisRef) {
            "Illegal usage of 'new' operator"
        }

        return JSObjectImpl().also {
            it.setProto(get(PROTOTYPE, runtime), runtime)
            invoke(args, runtime, superConstructorPropertyMap, it)
        }
    }

    private suspend fun invoke(
        args: List<Any?>,
        runtime: ScriptRuntime,
        extraArgs : Map<String, Pair<VariableType, Any?>>,
        thisRef: Any? = this.thisRef
    ): Any? {
        val allArgs = if (bindedArgs.isEmpty()) args else bindedArgs + args

        val arguments = buildMap {
            parameters.fastForEachIndexed { i, p ->
                this[p.name] = VariableType.Local to when {
                    p.isVararg -> allArgs.drop(i)
                    i in allArgs.indices -> allArgs[i]
                    p.default != null -> p.default.invoke(runtime)
                    else -> Unit
                }
            }

            extraArgs.forEach {
                this[it.key] = it.value
            }
            this["arguments"] = VariableType.Local to args
        }

        return if (isAsync){
            runtime.async {
                invokeImpl(runtime, arguments, thisRef)
            }
        } else {
            invokeImpl(runtime, arguments, thisRef)
        }
    }

    override suspend fun invoke(
        args: List<Any?>,
        runtime: ScriptRuntime,
    ): Any? = invoke(args, runtime, emptyMap())

    private suspend fun invokeImpl(
        runtime: ScriptRuntime,
        arguments: Map<String, Pair<VariableType,Any?>>,
        thisRef: Any?,
    ) : Any? {
        return try {
            runtime.withScope(
                extraProperties = arguments,
                thisRef = thisRef ?: runtime.thisRef,
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
