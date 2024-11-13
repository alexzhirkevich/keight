package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JSObject
import io.github.alexzhirkevich.keight.js.TypeError


internal class OpAssign(
    val type: VariableType? = null,
    val variableName: String,
    val receiver: Expression? = null,
    val assignableValue: Expression,
    private val merge: (ScriptRuntime.(Any?, Any?) -> Any?)?
) : Expression() {

    override suspend fun execute(runtime: ScriptRuntime): Any? {
        val v = assignableValue.invoke(runtime)
        val r = receiver?.invoke(runtime)

        val current = if (receiver == null) {
            runtime.get(variableName)
        } else {
            when (r){
                is JsAny -> r.get(variableName, runtime)
                else -> null
            }
        }

        check(merge == null || current != null) {
            "Cant modify $variableName as it is undefined"
        }

        val value = if (current != null && merge != null) {
            merge.invoke(runtime, current, v)
        } else v

        if (receiver == null) {
            runtime.set(
                property = variableName,
                value = value,
                type = type
            )
        } else {
            when (r) {
                is JSObject -> r.set(variableName, value, runtime)
                else -> throw TypeError("Cannot set properties of ${if (r == Unit) "undefined" else r}")
            }
        }

        return value
    }
}