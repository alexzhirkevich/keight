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
        return getImpl(receiver, runtime)
    }

    private tailrec suspend fun getImpl(res: Any?, runtime: ScriptRuntime): JsAny? {
        return when {
            receiver == null -> if (nameJs in runtime) {
                runtime.get(nameJs)
            } else {
                runtime.referenceError { "$nameJs is not defined" }
            }
            res is Expression -> getImpl(res(runtime), runtime)
            res is JsAny -> res.get(nameJs, runtime)
            isOptional && (res == null || res == Unit) -> Undefined
            else -> runtime.typeError { "Cannot get properties of $res" }
        }
    }
}