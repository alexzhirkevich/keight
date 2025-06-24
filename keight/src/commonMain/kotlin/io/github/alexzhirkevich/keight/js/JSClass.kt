package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Constructor
import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.Named
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck

internal sealed interface StaticClassMember : Named {

    class Variable(override val name: String, val init: Expression) : StaticClassMember

    class Method(val function: JSFunction) : StaticClassMember {
        override val name: String get() = function.name
    }
}

internal class OpClassInit(
    val name : String,
    val properties : Map<JsAny?, Expression>,
    val static : Map<JsAny?, StaticClassMember>,
    val construct: JSFunction?,
    val extends : Expression?
) : Expression(), JsAny {

    override suspend fun execute(runtime: ScriptRuntime): JsAny? {

        val extendsConstructor = extends?.invoke(runtime)

        runtime.typeCheck(extendsConstructor is Constructor?) {
            "$extendsConstructor is not a constructor".js
        }

        return JSClass(
            name = name,
            properties = properties.mapValues { it.value.invoke(runtime) },
            static = static.mapValues {
                when (val v = it.value) {
                    is StaticClassMember.Method -> v.function
                    is StaticClassMember.Variable -> v.init.invoke(runtime)
                }
            },
            construct = construct,
            extends = extendsConstructor
        ).apply {
            if (extendsConstructor != null) {
                setProto(runtime, extendsConstructor)
                val prototype = get(PROTOTYPE, runtime)
                if (prototype is JsObject) {
                    prototype.setProto(runtime, extendsConstructor.get(PROTOTYPE, runtime))
                }
            }
        }
    }
}

internal class JSClass(
    name : String,
    static: Map<JsAny?, JsAny?>,
    val construct: JSFunction?,
    extends: Constructor?,
    properties : Map<JsAny?, JsAny?>,
) : JSFunction(
    name = name,
    parameters = construct?.parameters ?: emptyList(),
    body = construct?.body ?: Expression { Undefined },
    prototype = JsObjectImpl(properties = properties.toMutableMap()),
    properties = static.toMutableMap(),
    superConstructor = extends
) {
    override fun toString(): String {
        return "[class $name]"
    }
}


