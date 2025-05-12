package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.interpreter.typeError


internal class JSSetFunction : JSFunction(name = "Set") {

    override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
        runtime.typeError { "Constructor Set requires 'new'" }
    }

    override suspend fun construct(args: List<JsAny?>, runtime: ScriptRuntime): JsAny {
        if (args.isEmpty()){
            return mutableSetOf<JsAny>().js()
        }
        val x = args[0]

        return when {
            x is Iterable<*> -> (x as Iterable<JsAny?>).toSet().js()
//            x is JsWrapper<*> && x.value is Iterable<*> -> (x.value as Iterable<*>).toSet()
            else -> runtime.typeError { "$x is not iterable" }
        }
    }
}