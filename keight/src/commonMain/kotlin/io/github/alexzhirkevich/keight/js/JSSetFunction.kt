package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.interpreter.typeError


internal class JSSetFunction : JSFunction(name = "Set") {

    override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Any {
        runtime.typeError { "Constructor Set requires 'new'" }
    }

    override suspend fun construct(args: List<Any?>, runtime: ScriptRuntime): Any {
        if (args.isEmpty()){
            return mutableSetOf<Any>()
        }
        val x = args[0]

        return when {
            x is Iterable<*> -> x.toSet()
//            x is JsWrapper<*> && x.value is Iterable<*> -> (x.value as Iterable<*>).toSet()
            else -> runtime.typeError { "$x is not iterable" }
        }
    }
}