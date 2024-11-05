package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime

internal class OpKeyValuePair(
    val key : String,
    val value : Expression
) : Expression() {
    override suspend fun execute(runtime: ScriptRuntime): Any? {
        return value.invoke(runtime)
    }
}