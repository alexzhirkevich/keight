package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.interpreter.parse
import io.github.alexzhirkevich.keight.js.interpreter.tokenize
import kotlinx.coroutines.Job

public fun JavaScriptEngine(
    runtime: JSRuntime = JSRuntime(Job()),
    vararg modules : Module
) : ScriptEngine = ScriptEngine(runtime, JSInterpreter, *modules)

private object JSInterpreter : ScriptInterpreter {
    override fun interpret(script: String): Script {
        return "{$script}"
            .tokenize()
//            .also { println(it.map { it::class.simpleName }) }
            .parse()
    }
}