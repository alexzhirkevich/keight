package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.ScopedRuntime
import io.github.alexzhirkevich.keight.StrictRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.SyntaxError
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck
import io.github.alexzhirkevich.keight.js.js

/**
 * Implements the JavaScript `with (object) statement` construct.
 *
 * The object is evaluated once, and the body is executed with a
 * runtime whose scope chain is: objectScope → parentScope.
 */
internal class OpWith(
    private val objectExpression: Expression,
    private val body: Expression,
) : Expression() {

    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
        val objectValue = objectExpression.invoke(runtime)

        // `with` is forbidden in strict mode.
        if (runtime is StrictRuntime) {
            throw SyntaxError("Strict mode code may not include a with statement")
        }


        runtime.typeCheck(objectValue is JsObject) {
            "$objectValue is not an object".js
        }


        // Build a scope whose lookups fall through to `objectValue`
        // before reaching the surrounding runtime.
        val withScope = WithRuntime(runtime, objectValue)

        return body.invoke(withScope)
    }

    /**
     * Runtime used only inside `with`.
     * It first asks the object for a property, then delegates
     * to the outer runtime.
     */
    private class WithRuntime(
        parent: ScriptRuntime,
        private val target: JsAny,
    ) : ScopedRuntime(parent, false) {

        override suspend fun get(property: JsAny?): JsAny? {
            // If the object actually owns the property, return it.
            if (target is JsObject && target.contains(property, this)) {
                return target.get(property, this)
            }
            // Otherwise fall through to the surrounding scope.
            return super.get(property)
        }

        override suspend fun set(property: JsAny?, value: JsAny?, type: VariableType?) {
            if (target is JsObject && target.contains(property, this)) {
                target.set(property, value, this)
            } else {
                super.set(property, value, type)
            }
        }
    }
}