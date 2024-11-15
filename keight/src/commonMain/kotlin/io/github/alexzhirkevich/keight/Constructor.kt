package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.PROTOTYPE
import io.github.alexzhirkevich.keight.js.isPrototypeOf

public interface Constructor : Callable {

    public suspend fun construct(args: List<Any?>, runtime: ScriptRuntime): Any

    override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any? {
        return construct(args, runtime)
    }

    public suspend fun isInstance(obj: Any?, runtime: ScriptRuntime): Boolean {
        return (get(PROTOTYPE, runtime) as? JsAny)?.isPrototypeOf(obj, runtime) == true
    }
}