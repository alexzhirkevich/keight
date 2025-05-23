package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.Undefined
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Part of script that represents single operation
 * */
public abstract class Expression {

    protected abstract suspend fun execute(runtime: ScriptRuntime): JsAny?

    public suspend operator fun invoke(runtime: ScriptRuntime): JsAny? {
        currentCoroutineContext().ensureActive()
        return when (val res = execute(runtime)){
            is Expression -> res.invoke(runtime)
            is Getter<*> -> res.get(runtime)
            else -> res
        }
    }
}

public fun Expression(execute : suspend (ScriptRuntime) -> JsAny?) : Expression {
    return object : Expression() {
        override suspend fun execute(runtime: ScriptRuntime): JsAny? {
            return execute.invoke(runtime)
        }
    }
}

