package io.github.alexzhirkevich.keight

public interface ScriptEngine {

    public val runtime : ScriptRuntime

    public val interpreter : ScriptInterpreter

    /**
     * Restore engine runtime to its initial state
     *
     * @see [ScriptRuntime.reset]
     * */
    public fun reset() {
        runtime.reset()
    }
}


public fun ScriptEngine.compile(script: String) : Script {
    return interpreter.interpret(script).asScript(runtime)
}

public suspend fun ScriptEngine.evaluate(script: String) : Any? {
    return interpreter.interpret(script).asScript(runtime).invoke()
}

public fun ScriptEngine(
    runtime: ScriptRuntime,
    interpreter: ScriptInterpreter,
    vararg modules: Module
): ScriptEngine = object : ScriptEngine{

    override val runtime: ScriptRuntime get() = runtime

    override val interpreter: ScriptInterpreter
        get() = interpreter

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