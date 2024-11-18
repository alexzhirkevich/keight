package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.interpreter.parse
import io.github.alexzhirkevich.keight.js.interpreter.tokenize

public class JavaScriptEngine(
    override val runtime: ScriptRuntime,
    vararg modules : Module
) : ScriptEngine() {

    init {
        modules.forEach {
            it.importInto(runtime.findRoot())
        }
    }

    override fun compile(script: String): Script {
        return "{$script}"
            .tokenize()
            .parse()
            .asScript(runtime)
    }
}