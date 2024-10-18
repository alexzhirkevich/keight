package io.github.alexzhirkevich.keight.es

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.InterpretationContext

public open class ESInterpretationContext(
    public val namedArgumentsEnabled : Boolean = false
) : InterpretationContext {
    override fun interpret(callable: String?, args: List<Expression>?): Expression? {
        return null
    }
}