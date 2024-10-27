package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.expressions.OpConstant
import io.github.alexzhirkevich.keight.findRoot
import io.github.alexzhirkevich.keight.invoke

internal class JSSetFunction : JSFunction(name = "Set",) {

    override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): Any {
        throw TypeError("Constructor Set requires 'new'")
    }

    override suspend fun construct(args: List<Expression>, runtime: ScriptRuntime): Any {
        if (args.isEmpty()){
            return mutableSetOf<Any>()
        }
        val x = args[0](runtime)

        return when {
            x is Iterable<*> -> x.toSet()
//            x is JsWrapper<*> && x.value is Iterable<*> -> (x.value as Iterable<*>).toSet()
            else -> throw TypeError( "$x is not iterable")
        }
    }
}