package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.invoke
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.interpreter.syntaxCheck
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck

internal class OpIn(
    val property : Expression,
    val inObject : Expression
)  : Expression {

    // for (let x in y)
    var variableType : VariableType ?= null

    override suspend fun invokeRaw(runtime: ScriptRuntime): Any? {
        syntaxCheck(variableType == null){
            "Unexpected token 'in'"
        }
        val o = inObject(runtime)
        typeCheck(o is JsAny) {
            "$o is not an object (can't use with 'in' operator)"
        }
        return o.contains(property(runtime), runtime)
    }
}

