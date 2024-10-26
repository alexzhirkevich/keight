package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.Callable

internal interface Constructor : Callable {
    fun construct(args: List<Expression>, runtime: ScriptRuntime): Any

    override fun invoke(args: List<Expression>, runtime: ScriptRuntime): Any? {
        return construct(args, runtime)
    }
}