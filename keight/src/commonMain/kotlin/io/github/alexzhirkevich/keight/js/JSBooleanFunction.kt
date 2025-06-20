package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime

internal class JSBooleanFunction : JSFunction(name = "Boolean") {

    override suspend fun construct(args: List<JsAny?>, runtime: ScriptRuntime): JsAny {
        return invoke(args, runtime)
    }

    override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny {
        return runtime.isFalse(args.getOrNull(0)).not().js
    }
}