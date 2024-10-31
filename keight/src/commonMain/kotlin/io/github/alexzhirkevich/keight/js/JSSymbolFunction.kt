package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime

internal class JSSymbolFunction : JSFunction("Symbol") {

    init {
        JSSymbol.defaults.forEach {
            this[it.value] = it
        }
    }

    override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): Any? {
        return super.invoke(args, runtime)
    }

    override suspend fun construct(args: List<Expression>, runtime: ScriptRuntime): Any {
        throw TypeError("Symbol is not a constructor")
    }
}