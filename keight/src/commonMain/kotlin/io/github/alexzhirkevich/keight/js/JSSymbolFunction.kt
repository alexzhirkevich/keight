package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.interpreter.typeError

internal class JSSymbolFunction : JSFunction(
    name = "Symbol",
    properties = JSSymbol.defaults
        .associateBy { it.value }
        .toMutableMap()
) {
    override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): JSSymbol {
        return JSSymbol(args.getOrElse(0) { "" }.toString())
    }

    override suspend fun construct(args: List<Any?>, runtime: ScriptRuntime): Any {
        runtime.typeError { "Symbol is not a constructor" }
    }
}