package io.github.alexzhirkevich.keight

public interface ScriptEngine<out R : ScriptRuntime> {

    /**
     * Engine runtime used to [evaluate] scripts.
     * Resets with [reset]
     * */
    public val runtime : R

    /**
     * Compile [script] code to an executable [Script] instance.
     *
     * [name]d scripts are treated as modules. After compilation they are stored in the [runtime]
     * and can be used with import statements. Module scripts should not be executed manually.
     *
     *
     * - The resulting [Script] instance is NOT thread safe. The same script instance cannot be
     * executed multiple times in parallel - it will wait for the previous execution to finish
     * - Script is always executed in the coroutine context of the engine runtime
     * */
    public fun compile(script: String, name : String? = null) : Script

    /**
     * Compile and invoke [script]. Result is converted to Kotlin type
     * */
    public suspend fun evaluate(script: String) : Any? {
        return compile(script).invoke()?.toKotlin(runtime)
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
