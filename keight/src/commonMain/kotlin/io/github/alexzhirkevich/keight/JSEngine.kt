package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.interpreter.parse
import io.github.alexzhirkevich.keight.js.interpreter.tokenize

public fun JavaScriptEngine(
    runtime: JSRuntime = JSRuntime(),
    vararg modules : Module
) : ScriptEngine = ScriptEngine(runtime, JSInterpreter, *modules)

private object JSInterpreter : ScriptInterpreter {
    override fun interpret(script: String): Script {
        return "{$script}".tokenize().parse()
    }
}