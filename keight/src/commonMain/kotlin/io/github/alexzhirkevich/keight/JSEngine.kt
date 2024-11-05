package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.interpreter.parse
import io.github.alexzhirkevich.keight.js.interpreter.tokenize
import kotlinx.coroutines.Job

public class JavaScriptEngine(
    override val runtime: JSRuntime,
    vararg modules : Module
) : ScriptEngine() {

    init {
        modules.forEach {
            it.importInto(runtime)
        }
    }

    override fun compile(script: String): Script {
        return "{$script}"
            .tokenize()
            .parse()
            .asScript(runtime)
    }
}