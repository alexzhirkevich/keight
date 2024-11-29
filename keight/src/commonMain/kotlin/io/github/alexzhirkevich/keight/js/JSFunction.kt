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
import io.github.alexzhirkevich.keight.expressions.Destruction
import io.github.alexzhirkevich.keight.expressions.OpBlock
import io.github.alexzhirkevich.keight.expressions.OpConstant
import io.github.alexzhirkevich.keight.fastForEachIndexed
import io.github.alexzhirkevich.keight.fastMapNotNull
import io.github.alexzhirkevich.keight.findRoot
import io.github.alexzhirkevich.keight.js.interpreter.referenceError
import io.github.alexzhirkevich.keight.js.interpreter.syntaxCheck
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck
import io.github.alexzhirkevich.keight.js.interpreter.typeError
import io.github.alexzhirkevich.keight.set
import kotlinx.coroutines.async
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableMap
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
import kotlin.collections.toSet
import kotlin.js.JsName


public sealed interface FunctionParam {
    public suspend fun set(
        index: Int,
        arguments: List<Any?>,
        runtime: ScriptRuntime
    )

    public suspend fun get(runtime: ScriptRuntime) : Any?

}

@JsName("MakeFunctionParam")
public fun FunctionParam(
    name : String,
    isVararg : Boolean = false,
    default : Expression? = null
) : FunctionParam = SimpleFunctionParam(name, isVararg, default)

internal class SimpleFunctionParam(
    val name : String,
    val isVararg : Boolean = false,
    val default : Expression? = null
) : FunctionParam {

    private suspend fun defaultOrUnit(runtime: ScriptRuntime) : Any? {
        return if (default != null) {
            runtime.withScope {
                it.set(
                    property = name,
                    value = Getter { it.referenceError { "Cannot access '$name' before initialization" } },
                    type = VariableType.Local
                )
                default.invoke(it)
            }
        } else {
            Unit
        }
    }

    override suspend fun set(
        index: Int,
        arguments: List<Any?>,
        runtime: ScriptRuntime
    ) {
        val value = when {
            isVararg -> arguments.drop(index)
            index in arguments.indices -> arguments.argOrElse(index) { Unit }.let {
                if (it == Unit) {
                    defaultOrUnit(runtime)
                } else {
                    it
                }
            }
            else -> defaultOrUnit(runtime)
        }

        runtime.set(name, value, VariableType.Local)
    }

    override suspend fun get(runtime: ScriptRuntime): Any? {
       return runtime.get(name)
    }
}

internal class DestructiveFunctionParam(
    private val destruction: Destruction,
    private val default: Expression? = null
) : FunctionParam {
    override suspend fun set(
        index: Int,
        arguments: List<Any?>,
        runtime: ScriptRuntime
    ) {
        val arg = arguments.getOrElse(index) { Unit }
        destruction.destruct(arg, null, runtime, default?.invoke(runtime))
    }

    override suspend fun get(runtime: ScriptRuntime): Any? {
        return Unit
    }
}

internal infix fun String.defaults(default: Expression?) : FunctionParam {
    return FunctionParam(this, false, default)
}

public open class JSFunction(
    final override val name : String = "",
    internal val parameters : List<FunctionParam> = emptyList(),
    internal val body : Expression = OpConstant(Unit),
    internal val isAsync : Boolean = false,
    internal val isArrow : Boolean = false,
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

        val varargs = parameters.count { it is SimpleFunctionParam && it.isVararg }

        if (varargs > 1 || varargs == 1 && !parameters.last().let { it is SimpleFunctionParam && it.isVararg }) {
            throw SyntaxError("Rest parameter must be last formal parameter")
        }

        if (parameters.lastOrNull().let { it is SimpleFunctionParam && it.isVararg && it.default != null }){
            throw SyntaxError("Rest parameter may not have a default initializer")
        }

        val namedList = parameters.fastMapNotNull { (it as? SimpleFunctionParam?)?.name }
        syntaxCheck(namedList.toSet().size == namedList.size){
            "Duplicate parameter name not allowed in this context"
        }

        syntaxCheck(parameters.all { it is SimpleFunctionParam } || body !is OpBlock || !body.isStrict){
            "Illegal 'use strict' directive in function with non-simple parameter list"
        }

        setPrototype(prototype)

        defineOwnProperty(
            property = "length",
            value = parameters.count { it !is SimpleFunctionParam || it.default != null && !it.isVararg },
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

        suspend fun doInvoke() : Any? {
            return invokeImpl(runtime, thisRef) { r ->
                parameters.fastForEachIndexed { i, p->
                    p.set(i, allArgs, r)
                }
                extraArgs.forEach {
                    r.set(it.key,it.value.second, it.value.first)
                }
                if (!isArrow) {
                    r.set(
                        "arguments",
                        Getter {
                            r.typeCheck(!isArrow && !isAsync && !it.isStrict) {
                                "'arguments' is not available in this context"
                            }
                            allArgs
                        },
                        VariableType.Local
                    )
                }
            }
        }

        return if (isAsync){
            runtime.async {
                doInvoke()
            }
        } else {
            doInvoke()
        }
    }

    override suspend fun invoke(
        args: List<Any?>,
        runtime: ScriptRuntime,
    ): Any? = invoke(args, runtime, emptyMap())

    private suspend fun invokeImpl(
        runtime: ScriptRuntime,
        thisRef: Any?,
        initArguments : suspend (ScriptRuntime) -> Unit,
    ) : Any? {
        return try {
            runtime.withScope(
                isFunction = true,
                thisRef = thisRef ?: runtime.thisRef,
                isSuspendAllowed = isAsync,
            ){
                initArguments(it)
                body.invoke(it)
            }
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
