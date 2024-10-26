package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.invoke


internal class OpForLoop(
    private val assignment : Expression?,
    private val increment: Expression?,
    private val comparison : Expression?,
    private val body: Expression
) : Expression {


    private val condition: (ScriptRuntime) -> Boolean = if (comparison == null) {
        { true }
    } else {
        { !it.isFalse(comparison.invoke(it)) }
    }

    override fun invokeRaw(context: ScriptRuntime): Any {
        context.withScope {
            assignment?.invoke(it)
            block(it)
        }
        return Unit
    }

    private fun block(ctx: ScriptRuntime) {
        while (condition(ctx)) {
            try {
                body(ctx)
            } catch (_: BlockContinue) {
                continue
            } catch (_: BlockBreak) {
                break
            } finally {
                increment?.invoke(ctx)
            }
        }
    }
}


internal fun  OpDoWhileLoop(
    condition : Expression,
    body : OpBlock
) = Expression {
    do {
        try {
            body.invoke(it)
        } catch (_: BlockContinue) {
            continue
        } catch (_: BlockBreak) {
            break
        }
    } while (!it.isFalse(condition.invoke(it)))
}


internal fun OpWhileLoop(
    condition : Expression,
    body : Expression,
) = Expression {
    while (!it.isFalse(condition.invoke(it))) {
        try {
            body.invoke(it)
        } catch (_: BlockContinue) {
            continue
        } catch (_: BlockBreak) {
            break
        }
    }
}
