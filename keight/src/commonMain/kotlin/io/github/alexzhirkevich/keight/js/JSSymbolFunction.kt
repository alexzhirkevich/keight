package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.interpreter.typeError

internal class JSSymbolFunction : JSFunction(
    name = "Symbol",
    properties = JSSymbol.defaults.mapKeys { it.key.js }.toMutableMap()
) {
    override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
        return JSSymbol(args.getOrElse(0) { "" }.toString())
    }

    override suspend fun construct(args: List<JsAny?>, runtime: ScriptRuntime): JsAny {
        runtime.typeError { "Symbol is not a constructor".js }
    }
}