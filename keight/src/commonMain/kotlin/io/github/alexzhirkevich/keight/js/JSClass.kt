package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Constructor
import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.Named
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.expressions.OpGetter
import io.github.alexzhirkevich.keight.expressions.OpSetter
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
            // Process properties: evaluate expressions and detect getters/setters
            properties = processClassProperties(properties, runtime),
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

/**
 * Process class properties, converting getters and setters to proper JsPropertyAccessor.
 * @param properties Map of property expressions from the parser
 * @param runtime The script runtime for evaluating expressions
 */
internal suspend fun processClassProperties(properties: Map<JsAny?, Expression>, runtime: ScriptRuntime): Map<JsAny?, JsAny?> {
    val result = mutableMapOf<JsAny?, JsAny?>()
    val getters = mutableMapOf<String, JSFunction>()
    val setters = mutableMapOf<String, JSFunction>()

    // First pass: check expression types and evaluate regular properties
    for ((key, value) in properties) {
        val propName = key?.toString() ?: continue

        when (value) {
            is OpGetter -> {
                // Key is "__getter__propertyName", extract actual property name
                val actualName = if (propName.startsWith("__getter__")) {
                    propName.removePrefix("__getter__")
                } else propName
                getters[actualName] = value.value
            }
            is OpSetter -> {
                // Key is "__setter__propertyName", extract actual property name
                val actualName = if (propName.startsWith("__setter__")) {
                    propName.removePrefix("__setter__")
                } else propName
                setters[actualName] = value.value
            }
            else -> {
                // Regular property: evaluate the expression
                result[key] = value.invoke(runtime)
            }
        }
    }

    // Second pass: create BackedField for properties with getter/setter
    for (propName in getters.keys + setters.keys) {
        val getter = getters[propName]
        val setter = setters[propName]
        result[propName.js] = JsPropertyAccessor.BackedField(getter, setter)
    }

    return result
}


