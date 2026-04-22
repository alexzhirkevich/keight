package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JSFunction
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsArrayWrapper
import io.github.alexzhirkevich.keight.js.js

/**
 * Represents a computed property name in an object literal.
 * For example: { [key]: value } where key is evaluated at runtime.
 */
internal class OpComputedProperty(
    val keyExpression: Expression,
    val valueExpression: Expression
) : Expression() {
    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
        // Execute both key and value expressions and return as an array [key, value]
        val key = keyExpression(runtime)
        val value = valueExpression(runtime)
        // If key is a JsArrayWrapper (computed array expression like ['test']), 
        // convert it to a string for use as property name
        val keyForProperty = when (key) {
            is JsArrayWrapper -> {
                // Convert JS array to string (JavaScript semantics: ['test'].toString() === 'test')
                val str = key.joinToString(",") { it?.toString() ?: "" }
                str.js
            }
            else -> key
        }
        return JsArrayWrapper(mutableListOf(keyForProperty, value))
    }
}

/**
 * Marker expression for computed property names in object literals.
 * This is a wrapper that signals to the parser that the enclosed expression
 * should be treated as a computed property name.
 */
internal class OpComputedPropertyName(
    val keyExpression: Expression
) : Expression() {
    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
        // This should not be called directly - OpComputedPropertyName is handled specially
        return keyExpression(runtime)
    }
}

/**
 * Represents a method with a computed property name.
 * For example: { [methodName]() { ... } }
 * This creates a method at runtime with a dynamically computed name.
 */
internal class OpComputedPropertyMethod(
    val keyExpression: Expression,
    val function: JSFunction
) : Expression() {
    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
        // Evaluate the key expression to get the method name
        val key = keyExpression(runtime)
        // Create a new function with the computed name
        // Note: We can't access private superConstructor, so we create a simplified version
        val namedFunction = JSFunction(
            name = key.toString(),
            parameters = function.parameters,
            body = function.body,
            isAsync = function.isAsync,
            isArrow = function.isArrow,
            prototype = function.prototype,
            properties = mutableMapOf()
        )
        // Return a structure that can be used to set the method on an object
        return JsArrayWrapper(mutableListOf(key, namedFunction))
    }
}
