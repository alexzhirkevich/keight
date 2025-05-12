package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsAny

internal class OpSpread(val value : Expression) : Expression() {

    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
        return value.invoke(runtime)
    }
}