package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime

internal class JSMapFunction : JSFunction(name = "Map",) {

    override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any {
        return if (args.isEmpty()) {
            mutableMapOf<Any?, Any?>()
        } else {
            TODO()
        }
    }

    override suspend fun construct(args: List<Any?>, runtime: ScriptRuntime): Any {
        return invoke(args, runtime)
    }
}