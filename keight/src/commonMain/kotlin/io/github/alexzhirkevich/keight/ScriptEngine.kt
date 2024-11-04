package io.github.alexzhirkevich.keight

public interface ScriptEngine {

    public val runtime : ScriptRuntime

    public val interpreter : ScriptInterpreter

    /**
     * Restore engine runtime to its initial state
     * - Cancels all running [Script]s compiled with this engine
     * - Cancels all [evaluate] tasks started with this engine
     * - Cancels all active async jobs launched by the engine [runtime]
     *
     * @see [ScriptRuntime.reset]
     * */
    public fun reset() {
        runtime.reset()
    }
}


/**
 * Compile [script] code to an executable [Script] instance.
 *
 * - The resulting [Script] instance is not thread safe. It will be executed in the coroutine
 * context of the [ScriptEngine.runtime]
 * */
public fun ScriptEngine.compile(script: String) : Script {
    return interpreter.interpret(script).asScript(runtime)
}

public suspend fun ScriptEngine.evaluate(script: String) : Any? {
    return compile(script).invoke()
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