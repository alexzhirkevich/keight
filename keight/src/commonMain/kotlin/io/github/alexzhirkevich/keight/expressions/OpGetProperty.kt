package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.ReferenceError
import io.github.alexzhirkevich.keight.js.TypeError

internal class OpConstant(val value: Any?) : Expression() {
    override suspend fun execute(runtime: ScriptRuntime): Any? {
        return value
    }
}


internal class OpGetProperty(
    val name : String,
    val receiver : Expression?,
    val isOptional : Boolean = false
) : Expression() {
    
    override suspend fun execute(runtime: ScriptRuntime, ): Any? {
        return getImpl(receiver, runtime)
    }

    private tailrec suspend fun getImpl(res: Any?, runtime: ScriptRuntime): Any? {
        return when {
            receiver == null -> if (name in runtime) {
                runtime.get(name)
            } else {
                throw ReferenceError("$name is not defined")
            }
            res is Expression -> getImpl(res(runtime), runtime)
            res is JsAny -> res.get(name, runtime)
            isOptional && (res == null || res == Unit) -> Unit
            else -> throw TypeError("Cannot get properties of $res")
        }
    }
}