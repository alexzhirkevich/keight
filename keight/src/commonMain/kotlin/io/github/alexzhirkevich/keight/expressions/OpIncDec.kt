package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.invoke

internal fun OpIncDecAssign(
    variable: Expression,
    isPrefix: Boolean,
    isInc: Boolean
) : Expression {

    val op = if (isInc) ScriptRuntime::inc else ScriptRuntime::dec
    val value = Expression { op(it, variable(it)) }

    val assignment = when {
        variable is OpGetVariable && variable.assignmentType == null ->
            OpAssign(
                variableName = variable.name,
                assignableValue = value,
                merge = null
            )

        variable is OpIndex && variable.variable is OpGetVariable ->
            OpAssignByIndex(
                variableName = variable.variable.name,
                index = variable.index,
                assignableValue = value,
                scope = null,
                merge = null
            )

        else -> error("$variable is not assignable")
    }

    if (isPrefix) {
        return assignment
    }

    return Expression { ctx ->
        variable(ctx).also { assignment(ctx) }
    }
}
