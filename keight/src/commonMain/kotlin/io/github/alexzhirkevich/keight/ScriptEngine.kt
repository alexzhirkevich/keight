package io.github.alexzhirkevich.keight

public interface ScriptEngine : ScriptInterpreter {

    public val runtime : ScriptRuntime

    public fun reset() {
        runtime.reset()
    }
}

public suspend fun ScriptEngine.evaluate(script: String) : Any? {
    return invoke(interpret(script)) 
}

public suspend fun ScriptEngine.invoke(script: Script) : Any? {
    return script.invoke(runtime)
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