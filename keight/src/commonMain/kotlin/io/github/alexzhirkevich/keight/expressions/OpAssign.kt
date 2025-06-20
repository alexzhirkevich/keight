package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.js.JSObject
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.interpreter.typeError
import io.github.alexzhirkevich.keight.js.js


internal class OpAssign(
    val type: VariableType? = null,
    val variableName: String,
    val receiver: Expression? = null,
    val assignableValue: Expression,
    private val merge: (suspend ScriptRuntime.(JsAny?, JsAny?) -> JsAny?)?
) : Expression() {

    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
        return invoke(
            type = type,
            variableName = variableName,
            receiver = receiver,
            value = assignableValue.invoke(runtime),
            merge = merge,
            runtime = runtime
        )
    }

    companion object {
        suspend fun invoke(
            type: VariableType?,
            variableName: String,
            receiver: Expression?,
            value: JsAny?,
            merge: (suspend ScriptRuntime.(JsAny?, JsAny?) -> JsAny?)?,
            runtime : ScriptRuntime
        ) : JsAny? {

            val r = receiver?.invoke(runtime)

            val current = if (receiver == null) {
                runtime.get(variableName.js)
            } else {
                when (r){
                    is JsAny -> r.get(variableName.js, runtime)
                    else -> null
                }
            }

            check(merge == null || current != null) {
                "Cant modify $variableName as it is undefined"
            }

            val v = if (current != null && merge != null) {
                merge.invoke(runtime, current, value)
            } else value

            if (receiver == null) {
                runtime.set(
                    property = variableName.js,
                    value = v,
                    type = type
                )
            } else {
                when (r) {
                    is JSObject -> r.set(variableName.js, v, runtime)
                    else -> runtime.typeError { "Cannot set properties of $r".js }
                }
            }

            return v
        }
    }
}

