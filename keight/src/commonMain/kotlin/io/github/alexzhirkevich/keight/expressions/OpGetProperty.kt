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

    private val nameJs = name.js()

    override suspend fun execute(runtime: ScriptRuntime, ): JsAny? {
        return getImpl(receiver, nameJs, runtime)
    }

    private suspend fun getImpl(res: Any?, property : JsAny?, runtime: ScriptRuntime): JsAny? {
        return when {
            receiver == null -> if (runtime.contains(property)) {
                runtime.get(property)
            } else {
                runtime.referenceError { "$property is not defined".js() }
            }
            else -> invoke(receiver, isOptional, property, runtime)
        }
    }

    companion object {
        tailrec suspend fun invoke(receiver: Any?, isOptional: Boolean, property : JsAny?, runtime: ScriptRuntime): JsAny? {
            return when {
                receiver is Expression -> invoke(receiver(runtime), isOptional, property, runtime)
                isOptional && (receiver == null || receiver == Undefined || receiver == Unit) -> Undefined
                receiver is JsAny -> receiver.get(property, runtime)
                else -> runtime.typeError { "Cannot get properties of $receiver".js() }
            }
        }
    }
}