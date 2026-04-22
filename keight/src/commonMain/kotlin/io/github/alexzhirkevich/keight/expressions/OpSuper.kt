package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.PROTO
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.interpreter.referenceError
import io.github.alexzhirkevich.keight.js.js

/**
 * Represents a super property access expression.
 * This expression accesses a property from the parent prototype of the class
 * where the method containing this expression was defined.
 *
 * In JavaScript, super is lexically bound - it refers to the prototype of
 * the class where the method is defined, not the runtime type of 'this'.
 */
internal class OpSuperGetProperty(
    private val name: String
) : Expression() {

    private val nameJs = name.js

    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
        val thisRef = runtime.thisRef
        if (thisRef == null || thisRef is Undefined) {
            runtime.referenceError { "'super' used outside of class method".js }
        }

        // Get 'super' from the runtime context - it should be set by the method's closure
        val superProto = runtime.get("super".js)
        if (superProto == null || superProto is Undefined) {
            // Fallback: try to get from the prototype chain
            val proto = thisRef.get(PROTO, runtime)
                ?: runtime.referenceError { "'super' used outside of class method".js }

            val parentProto = proto.get(PROTO, runtime)
            if (parentProto == null || parentProto is Undefined) {
                return Undefined
            }
            return parentProto.get(nameJs, runtime)
        }

        // Get the property from the super prototype
        return superProto.get(nameJs, runtime)
    }
}

/**
 * Represents a super computed property access expression.
 * This expression accesses a computed property from the parent prototype.
 */
internal class OpSuperGetPropertyComputed(
    private val index: Expression
) : Expression() {

    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
        val thisRef = runtime.thisRef
        if (thisRef == null || thisRef is Undefined) {
            runtime.referenceError { "'super' used outside of class method".js }
        }

        val indexValue = index.invoke(runtime)

        // Get 'super' from the runtime context
        val superProto = runtime.get("super".js)
        if (superProto == null || superProto is Undefined) {
            // Fallback: try to get from the prototype chain
            val proto = thisRef.get(PROTO, runtime)
                ?: runtime.referenceError { "'super' used outside of class method".js }

            val parentProto = proto.get(PROTO, runtime)
            if (parentProto == null || parentProto is Undefined) {
                return Undefined
            }
            return parentProto.get(indexValue, runtime)
        }

        // Get the property from the super prototype
        return superProto.get(indexValue, runtime)
    }
}
