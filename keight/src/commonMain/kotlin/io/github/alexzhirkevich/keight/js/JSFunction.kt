package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.Constructor
import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.Getter
import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.LazyGetter
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.expressions.BlockReturn
import io.github.alexzhirkevich.keight.expressions.OpConstant
import io.github.alexzhirkevich.keight.fastForEachIndexed
import io.github.alexzhirkevich.keight.fastMap
import io.github.alexzhirkevich.keight.findRoot
import io.github.alexzhirkevich.keight.get
import io.github.alexzhirkevich.keight.js.interpreter.syntaxCheck
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck
import kotlinx.coroutines.async
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableMap
import kotlin.collections.buildMap
import kotlin.collections.contains
import kotlin.collections.count
import kotlin.collections.drop
import kotlin.collections.emptyList
import kotlin.collections.emptyMap
import kotlin.collections.forEach
import kotlin.collections.indices
import kotlin.collections.last
import kotlin.collections.lastOrNull
import kotlin.collections.mapOf
import kotlin.collections.mutableMapOf
import kotlin.collections.plus
import kotlin.collections.set
import kotlin.collections.toSet

public class FunctionParam(
    public val name : String,
    public val isVararg : Boolean = false,
    public val default : Expression? = null
)

internal infix fun String.defaults(default: Expression?) : FunctionParam {
    return FunctionParam(this, false, default)
}

public open class ExtendableFunction(
    runtime : JSRuntime,
    name : String = "",
    prototype : JSObject = JSObjectImpl(),
    parameters : List<FunctionParam> = emptyList(),
    body : (suspend ScriptRuntime.(List<Any?>) -> Any?)? = null,
    properties : MutableMap<Any?, Any?> = mutableMapOf(),
) : JSFunction(
    name = name,
    parameters = parameters,
    prototype = prototype,
    properties = properties,
    body = if (body != null) {
        Expression { r ->
            body.invoke(r, parameters.fastMap { r.get(it.name) })
        }
    } else OpConstant(Unit)
)

public open class JSFunction(
    final override val name : String = "",
    internal val parameters : List<FunctionParam> = emptyList(),
    internal val body : Expression = OpConstant(Unit),
    internal val isAsync : Boolean = false,
    private val isArrow : Boolean = false,
    private val superConstructor : Constructor? = null,
    internal val prototype : JSObject? = JSObjectImpl(),
    properties : MutableMap<Any?, Any?> = mutableMapOf(),
) : JSObjectImpl(name, properties), Callable, JSObject, Constructor {

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

        if (parameters.lastOrNull().let { it?.isVararg == true && it.default != null }){
            throw SyntaxError("Rest parameter may not have a default initializer")
        }

        if (parameters.fastMap { it.name }.toSet().count() != parameters.size){
            throw SyntaxError("Duplicate parameter name not allowed in this context")
        }

        setPrototype(prototype)

        defineOwnProperty(
            property = "length",
            value = parameters.count { it.default != null && !it.isVararg },
            writable = false,
            enumerable = false,
            configurable = true
        )
        defineOwnProperty(
            property = "name",
            value = name,
            configurable = false,
            writable = false,
            enumerable = false
        )
    }

    override suspend fun fallbackProto(runtime: ScriptRuntime): Any? {
       return (runtime.findRoot() as JSRuntime).Function.get(PROTOTYPE, runtime)
    }

    private fun thisRef() = this

    override suspend fun get(property: Any?, runtime: ScriptRuntime): Any? {
        return super<JSObjectImpl>.get(property, runtime)
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

    public open suspend fun constructObject(
        args: List<Any?>,
        runtime: ScriptRuntime
    ) : JSObject = JSObjectImpl()

    override suspend fun construct(args: List<Any?>, runtime: ScriptRuntime): Any {
        syntaxCheck(isMutableThisRef) {
            "Illegal usage of 'new' operator"
        }
        return constructObject(args, runtime).also { o ->
            o.setProto(runtime, get(PROTOTYPE, runtime))
            invoke(args, runtime, superConstructorPropertyMap, o)
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
                    p.default != null -> LazyGetter {  p.default.invoke(runtime) }
                    else -> Unit
                }
            }

            extraArgs.forEach {
                this[it.key] = it.value
            }

            this["arguments"] = VariableType.Local to Getter {
                runtime.typeCheck(!isArrow && !isAsync && !it.isStrict){
                    "'arguments' is not available in this context"
                }
                allArgs
            }
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
                isFunction = true,
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

internal fun mapThisArg(arg : Any, strict : Boolean) : Any {
    return if (strict) {
        arg
    } else {
        when (arg) {
            is JsNumberWrapper -> JsNumberObject(arg)
            is Number -> JsNumberObject(JsNumberWrapper(arg))
            else -> arg
        }
    }
}
