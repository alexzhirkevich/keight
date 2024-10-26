package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Constructor
import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.Named
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.invoke
import io.github.alexzhirkevich.keight.expressions.OpConstant
import io.github.alexzhirkevich.keight.fastForEach

internal interface JSClass : JSObject, Named, Constructor {

    val functions : List<JSFunction>

    val construct: JSFunction?

    val extends : Expression?

    val constructorClass : Expression?

    val isInitialized : Boolean get() = true

    val static : List<StaticClassMember> get() = emptyList()

    override val type: String
        get() = "object"
}

internal fun JSClass.superFunctions(runtime: ScriptRuntime) : List<JSFunction> {
    val extendsClass = extends?.invoke(runtime) as? JSClass
        ?: return emptyList()

    return (extendsClass.superFunctions(runtime) + extendsClass.functions).associateBy { it.name }.values.toList()
}

internal tailrec fun JSClass.instanceOf(
    any: Any?,
    runtime: ScriptRuntime,
    extends: Expression? = this.extends
) : Boolean {

    if (constructorClass?.invoke(runtime) == any || any is JSObjectFunction) {
        return true
    }

    val e = extends?.invoke(runtime)?.let { it as? JSClass? } ?: return false

    if (e == any) {
        return true
    }

    return instanceOf(any, runtime, e.extends)
}

internal sealed interface StaticClassMember {

    fun assignTo(clazz : JSClass, runtime: ScriptRuntime)

    class Variable(val name : String, val init : Expression) : StaticClassMember {
        override fun assignTo(clazz: JSClass, runtime: ScriptRuntime) {
            clazz[name] = init(runtime)
        }
    }

    class Method(val function: JSFunction) : StaticClassMember {
        override fun assignTo(clazz: JSClass, runtime: ScriptRuntime) {
            clazz[function.name] = function
        }
    }
}

internal open class ESClassBase(
    override val name : String,
    final override val functions : List<JSFunction>,
    final override val construct: JSFunction?,
    final override val extends: Expression?,
    final override val static: List<StaticClassMember>
) : JSFunction(
    name = name,
    parameters = construct?.parameters.orEmpty(),
    body = construct?.body ?: OpConstant(Unit)
), JSClass {

    private var isSuperInitialized = false

    override val isInitialized: Boolean get() =
        extends == null || isSuperInitialized

    final override var constructorClass: Expression = OpConstant(this)

    override fun construct(args: List<Expression>, runtime: ScriptRuntime): Any {

        val parent = (extends?.invoke(runtime) as? JSClass)?.construct

        if (parent == null && extends != null) {
            isSuperInitialized = true
        }

        val clazz = ESClassBase(
            name = name,
            functions = (superFunctions(runtime) + functions).map(JSFunction::copy),
            construct = construct?.copy(
                extraVariables = if (parent != null){
                    mapOf("super" to (VariableType.Const to parent))
                } else emptyMap()
            ),
            extends = extends,
            static = static,
        )
        parent?.thisRef = clazz

        clazz.constructorClass = constructorClass
        clazz.functions.fastForEach {
            clazz[it.name] = it.apply { thisRef=clazz }
        }
        clazz.construct?.thisRef = clazz

        clazz.construct?.invoke(args, runtime)

        return clazz
    }
    override val type: String
        get() = "object"

    override fun toString(): String {
        val properties = keys.joinToString(
            prefix = " ",
            postfix = " ",
            separator = ", "
        ) {
            "$it: ${get(it)}"
        }
        return "$name {$properties}"
    }
}
