package io.github.alexzhirkevich.keight

/**
 * Pluggable module for script runtimes
 * */
public interface Module {

    public fun importInto(runtime: ScriptRuntime)

    public companion object
}


