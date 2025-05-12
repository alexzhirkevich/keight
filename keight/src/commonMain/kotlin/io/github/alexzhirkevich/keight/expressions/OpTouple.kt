package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.fastForEach
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.Undefined

internal class OpTouple(
    val expressions: List<Expression>
) : Expression() {

    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
        var res: JsAny? = Undefined
        expressions.fastForEach { res = it.invoke(runtime) }
        return res
    }
}

internal tailrec fun OpTouple.singleRecursiveOrNull() : Expression? {
    if (expressions.size != 1){
        return null
    }

    return when {
        expressions.size != 1 -> null
        expressions[0] is OpTouple -> (expressions[0] as OpTouple).singleRecursiveOrNull()
        else -> expressions[0]
    }
}