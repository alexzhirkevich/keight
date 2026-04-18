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

        // Get the parent prototype for super binding
        val parentPrototype = if (extendsConstructor != null) {
            extendsConstructor.get(PROTOTYPE, runtime)
        } else null

        return JSClass(
            name = name,
            // Process properties: evaluate expressions and detect getters/setters
            properties = processClassProperties(properties, runtime, parentPrototype),
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
 * Also sets the superProto on methods for super binding.
 * @param properties Map of property expressions from the parser
 * @param runtime The script runtime for evaluating expressions
 * @param superProto The parent prototype for super binding (null for base classes)
 */
internal suspend fun processClassProperties(
    properties: Map<JsAny?, Expression>,
    runtime: ScriptRuntime,
    superProto: JsAny? = null
): Map<JsAny?, JsAny?> {
    val result = mutableMapOf<JsAny?, JsAny?>()
    val getters = mutableMapOf<String, JSFunction>()
    val setters = mutableMapOf<String, JSFunction>()

    // First pass: check expression types and evaluate regular properties
    for ((key, value) in properties) {
        val propName = key?.toString() ?: continue

        when (value) {
            is OpGetter -> {
                // Key is "__getter__propertyName", extract actual property name
                val actualName = if (propName.startsWith(Constants.getterPrefix)) {
                    propName.removePrefix(Constants.getterPrefix)
                } else propName
                // Set superProto for the getter method
                val getter = value.value
                if (superProto != null) {
                    // Create a copy with superProto set
                    getters[actualName] = JSFunction(
                        name = getter.name,
                        parameters = getter.parameters,
                        body = getter.body,
                        isAsync = getter.isAsync,
                        isArrow = getter.isArrow,
                        prototype = getter.prototype,
                        superProto = superProto
                    )
                } else {
                    getters[actualName] = getter
                }
            }
            is OpSetter -> {
                // Key is "__setter__propertyName", extract actual property name
                val actualName = if (propName.startsWith(Constants.setterPrefix)) {
                    propName.removePrefix(Constants.setterPrefix)
                } else propName
                // Set superProto for the setter method
                val setter = value.value
                if (superProto != null) {
                    setters[actualName] = JSFunction(
                        name = setter.name,
                        parameters = setter.parameters,
                        body = setter.body,
                        isAsync = setter.isAsync,
                        isArrow = setter.isArrow,
                        prototype = setter.prototype,
                        superProto = superProto
                    )
                } else {
                    setters[actualName] = setter
                }
            }
            is OpFunctionInit -> {
                // Regular method: evaluate and set superProto
                val func = value.function
                if (superProto != null) {
                    // Check if this OpFunctionInit has already been processed
                    // (i.e., func.closure is already set). If so, don't re-wrap
                    // because the func.body is shared and re-wrapping would cause
                    // incorrect super binding in multi-level inheritance.
                    if (func.closure != null) {
                        // Already processed by a parent class. We need to create a new wrapper
                        // with the correct superProto, but use the already-set closure.
                        // The body should be the function's body (not OpFunctionInit) to avoid
                        // re-executing OpFunctionInit which would overwrite closure.
                        val funcWithSuper = JSFunction(
                            name = func.name,
                            parameters = func.parameters,
                            body = func.body,  // This is the already-created function's body
                            isAsync = func.isAsync,
                            isArrow = func.isArrow,
                            prototype = func.prototype,
                            superProto = superProto
                        )
                        result[key] = funcWithSuper
                    } else {
                        // First time processing, create a copy with superProto set
                        val funcWithSuper = JSFunction(
                            name = func.name,
                            parameters = func.parameters,
                            body = func.body,
                            isAsync = func.isAsync,
                            isArrow = func.isArrow,
                            prototype = func.prototype,
                            superProto = superProto
                        )
                        result[key] = funcWithSuper
                    }
                } else {
                    result[key] = value.invoke(runtime)
                }
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


