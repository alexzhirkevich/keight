package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Constructor
import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.Named
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.interpreter.syntaxCheck

internal sealed interface StaticClassMember : Named {

    class Variable(override val name: String, val init: Expression) : StaticClassMember

    class Method(val function: JSFunction) : StaticClassMember {
        override val name: String get() = function.name
    }
}

internal class OpClassInit(
    val name : String,
    val properties : Map<String, Expression>,
    val static : Map<String, StaticClassMember>,
    val construct: JSFunction?,
    val extends : Expression?
) : Expression() {

    override suspend fun execute(runtime: ScriptRuntime): JSClass {

        val extendsConstructor = extends?.invoke(runtime)

        syntaxCheck(extendsConstructor is Constructor?) {
            "$extendsConstructor is not a constructor"
        }

        val construct = construct ?: JSFunction("")

        return JSClass(
            name = name,
            properties = properties.mapValues { it.value.invoke(runtime) },
            static = static.mapValues {
                when (val v = it.value) {
                    is StaticClassMember.Method -> v.function
                    is StaticClassMember.Variable -> v.init(runtime)
                }
            },
            construct = construct,
            extends = extendsConstructor
        ).apply {
            if (extendsConstructor != null) {
                setProto(extendsConstructor, runtime)
                val prototype = get(PROTOTYPE, runtime)
                if (prototype is JSObject) {
                    prototype.setProto(extendsConstructor.get(PROTOTYPE, runtime), runtime)
                }
            }
        }
    }
}

internal class JSClass(
    name : String,
    static: Map<String, Any?>,
    construct: JSFunction,
    extends: Constructor?,
    properties : Map<String, Any?>,
) : JSFunction(
    name = name,
    parameters = construct.parameters,
    body = construct.body,
    prototype = JSObjectImpl(properties = properties.toMutableMap()),
    properties = static.toMutableMap(),
    superConstructor = extends
) {
    override fun toString(): String {
        return "[class $name]"
    }
}


