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
import io.github.alexzhirkevich.keight.expressions.OpAssign
import io.github.alexzhirkevich.keight.expressions.OpBlock
import io.github.alexzhirkevich.keight.expressions.OpConstant
import io.github.alexzhirkevich.keight.expressions.OpDestructAssign
import io.github.alexzhirkevich.keight.expressions.OpGetProperty
import io.github.alexzhirkevich.keight.expressions.OpMake
import io.github.alexzhirkevich.keight.expressions.OpSpread
import io.github.alexzhirkevich.keight.expressions.asDestruction
import io.github.alexzhirkevich.keight.fastForEach
import io.github.alexzhirkevich.keight.fastForEachIndexed
import io.github.alexzhirkevich.keight.fastMapNotNull
import io.github.alexzhirkevich.keight.findRoot
import io.github.alexzhirkevich.keight.js.interpreter.makeReferenceError
import io.github.alexzhirkevich.keight.js.interpreter.referenceCheck
import io.github.alexzhirkevich.keight.js.interpreter.referenceError
import io.github.alexzhirkevich.keight.js.interpreter.syntaxCheck
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck
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

    public val default : Expression?

    public suspend fun set(
        index: Int,
        arguments: List<JsAny?>,
        runtime: ScriptRuntime
    )

    public suspend fun get(runtime: ScriptRuntime): JsAny?

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
    override val default : Expression? = null
) : FunctionParam {

    private suspend fun defaultOrUnit(runtime: ScriptRuntime) : JsAny? {
        return if (default != null) {
            runtime.withScope {
                it.set(
                    property = name.js(),
                    value = JSPropertyAccessor.BackedField(
                        getter = Callable {
                            runtime.referenceError { "Cannot access '$name' before initialization".js() }
                        }
                    ),
                    type = VariableType.Local
                )
                default.invoke(it)
            }
        } else {
            Undefined
        }
    }

    override suspend fun set(
        index: Int,
        arguments: List<JsAny?>,
        runtime: ScriptRuntime
    ) {
        val value = when {
            isVararg -> arguments.drop(index).js()
            index in arguments.indices -> arguments.argOrElse(index) { Undefined }.let {
                if (it == Undefined) {
                    defaultOrUnit(runtime)
                } else {
                    it
                }
            }
            else -> defaultOrUnit(runtime)
        }

        runtime.set(name.js(), value, VariableType.Local)
    }

    override suspend fun get(runtime: ScriptRuntime): JsAny? {
       return runtime.get(name.js())
    }
}

internal class DestructiveFunctionParam(
    val destruction: Destruction,
    override val default: Expression? = null,
) : FunctionParam {
    override suspend fun set(
        index: Int,
        arguments: List<JsAny?>,
        runtime: ScriptRuntime
    ) {
        val arg = arguments.getOrElse(index) {
            return destruction.destruct(default?.invoke(runtime), null, runtime, null)
        }
        destruction.destruct(
            obj = arg,
            variableType = null,
            runtime = runtime,
            default = default?.let { LazyGetter(it::invoke) }
        )
    }

    override suspend fun get(runtime: ScriptRuntime): JsAny? = Undefined
}

internal fun String.vararg() = FunctionParam(this, isVararg = true)

internal infix fun String.defaults(default: Expression?) : FunctionParam {
    return FunctionParam(this, false, default)
}

internal class OpFunctionInit(
    val function : JSFunction
) : Expression() {
    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
        function.closure = runtime
        return function
    }
}

public open class JSFunction(
    final override var name : String = "",
    internal val parameters : List<FunctionParam> = emptyList(),
    internal val body : Expression = OpConstant(Undefined),
    internal val isAsync : Boolean = false,
    internal val isArrow : Boolean = false,
    private val superConstructor : Constructor? = null,
    internal val prototype : JSObject? = JSObjectImpl(),
    properties : MutableMap<JsAny?, JsAny?> = mutableMapOf(),
) : JSObjectImpl(name, properties), Callable, JSObject, Constructor {

    override val type: String get() = "function"

    private var thisRef : JsAny? = null
    private var isMutableThisRef = !isArrow
    private var bindedArgs = emptyList<JsAny?>()
    internal var closure : ScriptRuntime? = null

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

        var length = 0
        for (p in parameters){
            if (p.default != null || (p is SimpleFunctionParam && p.isVararg)){
                break
            }
            length++
        }

        defineOwnProperty(
            property = "length".js(),
            value = length.js(),
            writable = false,
            enumerable = false,
            configurable = true
        )

        if ("name".js() !in this.properties) {
            defineName(name)
        }
    }

    internal fun defineName(name: String){
        this.name = name
        defineOwnProperty(
            property = "name".js(),
            value = name.js(),
            configurable = true,
            writable = false,
            enumerable = false
        )
    }

    override suspend fun fallbackProto(runtime: ScriptRuntime): JsAny? {
       return (runtime.findRoot() as JSRuntime).Function.get(PROTOTYPE, runtime)
    }

    private fun thisRef() = this

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {
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
    ).apply {
        closure = this@JSFunction.closure
    }

    override suspend fun call(thisArg: JsAny?, args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
        return invoke(
            args = if (bindedArgs.isEmpty()) args else bindedArgs + args,
            runtime = runtime,
            extraArgs = emptyMap(),
            thisRef = if (isMutableThisRef) thisArg else thisRef
        )
    }

    override suspend fun bind(thisArg: JsAny?, args: List<JsAny?>, runtime: ScriptRuntime): Callable {
        return copy().also {
            it.thisRef = if (isMutableThisRef) thisArg else thisRef
            it.isMutableThisRef = false
            it.bindedArgs = if (bindedArgs.isEmpty()) args else bindedArgs + args
        }
    }

    public open suspend fun constructObject(
        args: List<JsAny?>,
        runtime: ScriptRuntime
    ) : JSObject = JSObjectImpl()

    override suspend fun construct(args: List<JsAny?>, runtime: ScriptRuntime): JsAny {
        syntaxCheck(isMutableThisRef) {
            "Illegal usage of 'new' operator"
        }
        return constructObject(
            args = args,
            runtime = runtime
        ).also { o ->
            o.setProto(runtime, get(PROTOTYPE, runtime))
            var superInitialized = false

            val mustHaveSuperInitialized = superConstructor != null && this is JSClass && construct != null

            suspend fun assertSuperInitialized() {
                if (mustHaveSuperInitialized && !superInitialized) {
                    runtime.referenceError {
                        "Must call super constructor in derived class before accessing 'this' or returning from derived constructor".js()
                    }
                }
            }

            invoke(
                args = args,
                runtime = runtime,
                extraArgs = if (superConstructor == null ) {
                    emptyMap()
                } else {
                    mapOf(
                        "super" to Pair(
                            VariableType.Const,
                            Callable {
                                superConstructor.call(o, it, runtime).also {
                                    runtime.referenceCheck(!superInitialized) {
                                        "Super constructor may only be called once".js()
                                    }
                                    superInitialized = true
                                }
                            }
                        )
                    )
                },
                thisRef = o
//                thisRef = Getter {
//                    assertSuperInitialized()
//                    o
//                }
            ).also {
                assertSuperInitialized()
            }
        }
    }

    private suspend fun invoke(
        args: List<JsAny?>,
        runtime: ScriptRuntime,
        extraArgs : Map<String, Pair<VariableType, JsAny?>>,
        thisRef: JsAny? = this.thisRef
    ): JsAny? {
        val allArgs = if (bindedArgs.isEmpty()) args else bindedArgs + args

        val invokeRuntime = closure ?: runtime

        suspend fun doInvoke() : JsAny? {
            return invokeImpl(invokeRuntime, thisRef) { r ->
                parameters.fastForEachIndexed { i, p ->
                    p.set(i, allArgs, r)
                }
                extraArgs.forEach {
                    r.set(it.key.js(), it.value.second, it.value.first)
                }
                if (!isArrow) {
                    r.set(
                        "arguments".js(),
                        JsArrayWrapper(allArgs.toMutableList()),
//                        LazyGetter {
//                            r.typeCheck(!isArrow && !isAsync && !it.isStrict) {
//                                "'arguments' is not available in this context"
//                            }
//                            JsArrayWrapper(allArgs.toMutableList())
//                        },
                        VariableType.Local
                    )
                }

            }
        }

        return if (isAsync){
            invokeRuntime.async { doInvoke() }.js()
        } else {
            doInvoke()
        }
    }

    override suspend fun invoke(
        args: List<JsAny?>,
        runtime: ScriptRuntime,
    ): JsAny? = invoke(args, runtime, emptyMap())

    private suspend fun invokeImpl(
        runtime: ScriptRuntime,
        thisRef: JsAny?,
        initArguments : suspend (ScriptRuntime) -> Unit,
    ) : JsAny? {
        return try {
            runtime.withScope {

                initArguments(it)

                it.withScope(
                    isFunction = true,
                    thisRef = thisRef ?: it.thisRef,
                    isSuspendAllowed = isAsync,
                ){
                    body.invoke(it)
                }
            }

        } catch (ret: BlockReturn) {
            ret.value
        }
    }

    override fun toString(): String {
        return "[function $name]"
    }
}

internal fun validateFunctionParams(
    params : List<FunctionParam>,
    isArrow: Boolean,
){
    val names = mutableSetOf<String>()

    params.fastForEach {
        when(it){
            is DestructiveFunctionParam -> {
                if (
                    it.destruction.variables.any { it in names } ||
                    it.destruction.variables.toSet().size != it.destruction.variables.size
                ){
                    throw SyntaxError("Duplicate parameter name not allowed in this context")
                }
                names.addAll(it.destruction.variables)
            }
            is SimpleFunctionParam -> {
                if (isArrow &&  it.name in names){
                    throw SyntaxError("Duplicate parameter name not allowed in this context")
                }
                names += it.name
            }
        }
    }
}

internal fun Expression.toFunctionParam() : FunctionParam {
    return when (this) {
        is OpGetProperty -> FunctionParam(name = name)
        is OpSpread -> value.toFunctionParam().let {
            syntaxCheck(it is SimpleFunctionParam) {
                "Invalid function declaration"
            }
            FunctionParam(
                name = it.name,
                isVararg = true,
                default = it.default
            )
        }

        is OpAssign -> FunctionParam(
            name = variableName,
            default = assignableValue
        )

        is OpDestructAssign -> DestructiveFunctionParam(destruction, value)
        is OpBlock,
        is OpMake -> DestructiveFunctionParam(asDestruction())

        else -> throw SyntaxError("Invalid function declaration")
    }
}

internal fun mapThisArg(arg : JsAny, strict : Boolean) : JsAny {
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
