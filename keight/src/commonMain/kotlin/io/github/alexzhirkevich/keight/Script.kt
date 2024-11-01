package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.JSError
import io.github.alexzhirkevich.keight.js.JsAny

public interface Script {
    public suspend operator fun invoke(): Any?
}

public fun Expression.asScript(runtime: ScriptRuntime): Script = object : Script {
    override suspend fun invoke(): Any? {
        return try {
            runtime.toKotlin(invoke(runtime))
        } catch (t: Throwable) {
            if (t is JsAny) {
                throw t
            } else {
                throw JSError(t.message, cause = t)
            }
        }
    }
}

