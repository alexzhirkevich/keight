package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.thisRef

internal class JSMapFunction : JSFunction(
    name = "Map",
    prototype = Object {
        "set".func("key", "value") { args ->
            val key = args[0]
            val v = args.getOrElse(1) { }

            thisRef<JsMapWrapper>().value.set(key, v)
        }
        "get".func("key") { args ->
            val key = args[0]
            thisRef<JsMapWrapper>().value.get(key)
        }
    }
) {
    override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any {
        return if (args.isEmpty()) {
            emptyMap()
        } else {
            val entries = args[0] as List<List<*>>
            entries.associate { it[0] to it[1] }
        }
    }

    override suspend fun construct(args: List<Any?>, runtime: ScriptRuntime): Any {
        return invoke(args, runtime)
    }
}