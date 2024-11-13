package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JSError
import io.github.alexzhirkevich.keight.js.JSFunction
import io.github.alexzhirkevich.keight.js.Object

internal class JSErrorFunction : JSFunction(
    name = "Error",
    prototype = Object {
        "message" eq ""
        "name" eq "Error"
    }
) {
    override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): JSError {
        if (args.isEmpty()) {
            return JSError("Uncaught Error")
        }

        return when {
            args.isEmpty() -> JSError("Uncaught Error")
            else -> JSError(args[0])
        }
    }
}