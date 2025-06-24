package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsSymbol
import io.github.alexzhirkevich.keight.js.JsAny

internal class OpSpread(val value : Expression) : Expression() {

    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
        val obj = value.invoke(runtime)
        return obj?.get(JsSymbol.iterator, runtime)?.let {
            if (it is Callable) {
                it.call(obj, emptyList(), runtime)
            } else it
        }
    }
}