package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.fastForEach
import io.github.alexzhirkevich.keight.fastForEachIndexed
import io.github.alexzhirkevich.keight.get
import io.github.alexzhirkevich.keight.js.JSObject
import io.github.alexzhirkevich.keight.js.JSStringFunction
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.SyntaxError
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck
import io.github.alexzhirkevich.keight.js.interpreter.typeError


internal class OpAssign(
    val type: VariableType? = null,
    val variableName: String,
    val receiver: Expression? = null,
    val assignableValue: Expression,
    private val merge: (suspend ScriptRuntime.(Any?, Any?) -> Any?)?
) : Expression() {

    override suspend fun execute(runtime: ScriptRuntime): Any? {
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
            value: Any?,
            merge: (suspend ScriptRuntime.(Any?, Any?) -> Any?)?,
            runtime : ScriptRuntime
        ) : Any? {

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

            val v = if (current != null && merge != null) {
                merge.invoke(runtime, current, value)
            } else value

            if (receiver == null) {
                runtime.set(
                    property = variableName,
                    value = v,
                    type = type
                )
            } else {
                when (r) {
                    is JSObject -> r.set(variableName, v, runtime)
                    else -> runtime.typeError { "Cannot set properties of ${if (r == Unit) "undefined" else r}" }
                }
            }

            return v
        }
    }
}

