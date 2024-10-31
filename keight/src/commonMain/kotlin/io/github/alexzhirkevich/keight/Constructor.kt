package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.PROTOTYPE

public interface Constructor : Callable {

    public suspend fun construct(args: List<Expression>, runtime: ScriptRuntime): Any

    public suspend fun isInstance(obj : Any?, runtime: ScriptRuntime) : Boolean {
        return when {
            obj === get(PROTOTYPE, runtime) -> true
            obj !is JsAny -> false
            else -> isInstance(obj.proto(runtime), runtime)
        }
    }

    override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): Any? {
        return construct(args, runtime)
    }
}