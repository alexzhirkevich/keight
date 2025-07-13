package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.isPrototypeOf

public interface Constructor : Callable {

    public suspend fun construct(args: List<JsAny?>, runtime: ScriptRuntime): JsAny

    override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
        return construct(args, runtime)
    }

    public suspend fun isInstance(obj: JsAny?, runtime: ScriptRuntime): Boolean {
        return get(runtime.fromKotlin("prototype"), runtime)
            ?.isPrototypeOf(obj, runtime) == true
    }
}