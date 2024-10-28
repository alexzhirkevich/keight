package io.github.alexzhirkevich.keight

public interface ScriptInterpreter {

    public fun interpret(script : String) : Expression
}