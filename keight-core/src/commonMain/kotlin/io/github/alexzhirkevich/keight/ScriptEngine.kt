package io.github.alexzhirkevich.keight

import kotlinx.coroutines.sync.Mutex

public interface ScriptEngine<out R : ScriptRuntime> {

    public val runtime : R

    public val mutex : Mutex

    /**
     * Compile [script] code to an executable [Script] instance.
     *
     * - The resulting [Script] instance is NOT thread safe. The same script instance cannot be
     * executed multiple times in parallel - it will wait for the previous execution to finish
     * - Script is always executed in the coroutine context of the engine runtime
     * */
    public fun compile(script: String) : Script

    public suspend fun evaluate(script: String) : Any? {
        return compile(script).invoke(runtime)?.toKotlin(runtime)
    }

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
