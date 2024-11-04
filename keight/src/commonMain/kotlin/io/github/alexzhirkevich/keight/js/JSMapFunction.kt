package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.ScriptRuntime
import kotlin.jvm.JvmInline

internal class JSMapFunction : JSFunction(name = "Map",) {

    init {
        setPrototype(Object {
            "set".func("key", "value") { args ->
                val key = args[0]
                val v = args.getOrElse(1) { }

                (get("this") as JsMapWrapper).value.set(key, v)
            }

            "get".func("key") { args ->
                val key = args[0]
                (get("this") as JsMapWrapper).value.get(key)
            }
        })
    }

    override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any {
        return if (args.isEmpty()) {
            mutableMapOf()
        } else {
            val entries = args[0] as List<List<*>>
            entries.associate { it[0] to it[1] }
        }
    }

    override suspend fun construct(args: List<Any?>, runtime: ScriptRuntime): Any {
        return invoke(args, runtime)
    }
}