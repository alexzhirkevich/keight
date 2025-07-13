package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.interpreter.referenceError
import io.github.alexzhirkevich.keight.js.interpreter.typeError
import io.github.alexzhirkevich.keight.js.js

internal class OpConstant(val value: JsAny?) : Expression() {
    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
        return value
    }
}


internal class OpGetProperty(
    val name : String,
    val receiver : Expression?,
    val isOptional : Boolean = false
) : Expression() {

    private val nameJs = name.js

    override suspend fun execute(runtime: ScriptRuntime, ): JsAny? {
        return when {
            receiver == null -> if (runtime.contains(nameJs)) {
                runtime.get(nameJs)
            } else {
                runtime.referenceError("$nameJs is not defined".js)
            }
            else -> invoke(receiver.invoke(runtime), isOptional, nameJs, runtime)
        }
    }

    companion object {
        suspend fun invoke(receiver: JsAny?, isOptional: Boolean, property : JsAny?, runtime: ScriptRuntime): JsAny? {
            return when {
                isOptional && (receiver == null || receiver == Undefined) -> Undefined
                receiver != null -> receiver.get(property, runtime)
                else -> runtime.typeError { "Cannot get properties of $receiver".js }
            }
        }
    }
}