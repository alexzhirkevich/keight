package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.thisRef

internal class JSMapFunction : JSFunction(
    name = "Map",
    prototype = Object {
        "set".js().func("key", "value") { args ->
            val key = args[0]
            val v = args.getOrElse(1) { Undefined }

            thisRef<JsMapWrapper>().value[key] = v
            Undefined
        }
        "get".js().func("key") { args ->
            val key = args[0]
            thisRef<JsMapWrapper>().value[key]
        }
    }
) {
    override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny {
        return if (args.isEmpty()) {
            emptyMap<JsAny?,JsAny?>().js()
        } else {
            val entries = args[0] as List<List<JsAny?>>
            entries.associate { it[0] to it[1] }.js()
        }
    }

    override suspend fun construct(args: List<JsAny?>, runtime: ScriptRuntime): JsAny {
        return invoke(args, runtime)
    }
}