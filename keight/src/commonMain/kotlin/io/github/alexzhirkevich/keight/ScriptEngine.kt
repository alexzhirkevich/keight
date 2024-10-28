package io.github.alexzhirkevich.keight

public interface ScriptEngine : ScriptInterpreter {

    public val runtime : ScriptRuntime

    /**
     * Restore engine runtime to its initial state
     *
     * @see [ScriptRuntime.reset]
     * */
    public fun reset() {
        runtime.reset()
    }
}

public suspend fun ScriptEngine.evaluate(script: String) : Any? {
    return interpret(script).asScript(runtime).invoke()
}

public fun ScriptEngine(
    runtime: ScriptRuntime,
    interpreter: ScriptInterpreter,
    vararg modules: Module
): ScriptEngine = object : ScriptEngine, ScriptInterpreter by interpreter {

    override val runtime: ScriptRuntime get() = runtime

    init {
        importModules()
    }

    override fun reset() {
        super.reset()
        importModules()
    }

    private fun importModules(){
        modules.forEach {
            it.importInto(runtime)
        }
    }
}