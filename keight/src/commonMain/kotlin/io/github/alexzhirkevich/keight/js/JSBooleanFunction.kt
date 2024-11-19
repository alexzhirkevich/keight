package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.interpreter.typeError

internal class JSBooleanFunction : JSFunction(name = "Boolean") {

    override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any {
        return !runtime.isFalse(args.getOrNull(0))
    }
}