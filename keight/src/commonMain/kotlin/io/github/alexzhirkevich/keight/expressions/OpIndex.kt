package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.checkNotEmpty
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.valueAtIndexOrUnit

internal class OpIndex(
    val receiver : Expression,
    val index : Expression,
    val isOptional : Boolean = false
) : Expression() {

    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
        val prop = receiver(runtime)

        val v = when {
            isOptional && (prop == null || prop == Undefined) -> return Undefined
            else -> checkNotEmpty(prop)
        }

        val idx = index(runtime)

        return OpGetProperty.invoke(v, isOptional, idx, runtime)

//        return v.valueAtIndexOrUnit(idx, runtime.toNumber(idx).toInt(), runtime)
    }
}