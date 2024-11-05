package io.github.alexzhirkevich.keight

public abstract class ScriptEngine {

    public abstract val runtime : ScriptRuntime

    /**
     * Compile [script] code to an executable [Script] instance.
     *
     * - The resulting [Script] instance is NOT thread safe. The same script instance cannot be
     * executed multiple times in parallel - it will wait for the previous execution to finish
     * - Script is always executed in the coroutine context of the engine runtime
     * */
    public abstract fun compile(script: String) : Script

    public open suspend fun evaluate(script: String) : Any? {
        return compile(script).invoke()
    }

    /**
     * Restore engine runtime to its initial state
     * - Cancels all running [Script]s compiled with this engine
     * - Cancels all [evaluate] tasks started with this engine
     * - Cancels all active async jobs launched by the engine [runtime]
     *
     * @see [ScriptRuntime.reset]
     * */
    public open fun reset() {
        runtime.reset()
    }
}
