package io.github.alexzhirkevich.keight

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Part of script that represents single operation
 * */
public abstract class Expression {

    protected abstract suspend fun execute(runtime: ScriptRuntime): Any?

    public suspend operator fun invoke(runtime: ScriptRuntime): Any? {
        currentCoroutineContext().ensureActive()
        return when (val res = execute(runtime)){
            is Expression -> res.invoke(runtime)
            is Getter<*> -> runtime.fromKotlin(res.get())
            else -> runtime.fromKotlin(res)
        }
    }
}

public fun Expression(execute : suspend (ScriptRuntime) -> Any?) : Expression {
    return object : Expression() {
        override suspend fun execute(runtime: ScriptRuntime): Any? {
            return execute.invoke(runtime)
        }
    }
}
