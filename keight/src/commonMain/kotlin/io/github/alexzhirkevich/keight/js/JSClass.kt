package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Constructor
import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.Named
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.expressions.OpConstant
import io.github.alexzhirkevich.keight.invoke
import io.github.alexzhirkevich.keight.js.interpreter.syntaxCheck

internal interface JSClass : JSObject, Named, Constructor {

    val construct: JSFunction?

    val extends : Constructor?

    val static : Map<String, Any?>
}

internal sealed interface StaticClassMember : Named {

    class Variable(override val name: String, val init: Expression) : StaticClassMember

    class Method(val function: JSFunction) : StaticClassMember {
        override val name: String get() = function.name
    }
}

internal class JSClassDeclaration(
    val name : String,
    val properties : Map<String, Expression>,
    val static : Map<String, StaticClassMember>,
    val construct: JSFunction?,
    val extends : Expression?
)

internal class OpClassRegistration(val clazz: JSClassDeclaration) : Expression {

    override suspend fun invokeRaw(runtime: ScriptRuntime): JSClass {

        val extendsConstructor = clazz.extends?.invoke(runtime)

        syntaxCheck(extendsConstructor is Constructor?) {
            "$extendsConstructor is not a constructor"
        }

        val construct = clazz.construct ?: JSFunction("")

        return JSClassImpl(
            name = clazz.name,
            properties = clazz.properties.mapValues { it.value.invoke(runtime) },
            static = clazz.static.mapValues {
                when (val v = it.value) {
                    is StaticClassMember.Method -> v.function
                    is StaticClassMember.Variable -> v.init(runtime)
                }
            },
            construct = clazz.construct ?: JSFunction(""),
            extends = extendsConstructor
        ).apply {
            if (extends != null) {
                setProto(extends)
                construct["super"] = extends.bind(listOf(OpConstant(this)), runtime)
                val prototype = get(PROTOTYPE, runtime)
                if (prototype is JSObject) {
                    prototype.setProto(extends.get(PROTOTYPE, runtime))
                }
            }
        }
    }
}

internal open class JSClassImpl(
    final override val name : String,
    final override val static: Map<String, Any?>,
    final override val construct: JSFunction,
    final override val extends: Constructor?,
    properties : Map<String, Any?>,
) : JSFunction(
    name = name,
    parameters = construct.parameters,
    body = construct.body,
    prototype = JSObjectImpl(map = properties.toMutableMap()),
    properties = static.toMutableMap()
), JSClass {

    override fun toString(): String {
        return "[class $name]"
    }
}


