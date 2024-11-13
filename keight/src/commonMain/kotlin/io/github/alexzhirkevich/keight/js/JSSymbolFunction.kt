package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime

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
        throw TypeError("Symbol is not a constructor")
    }
}