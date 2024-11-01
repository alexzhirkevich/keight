package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.invoke
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.interpreter.syntaxCheck
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive


internal class OpForLoop(
    private val assignment : Expression?,
    private val increment: Expression?,
    private val comparison : Expression?,
    private val body: Expression
) : Expression {


    private val condition: suspend (ScriptRuntime) -> Boolean = if (comparison == null) {
        { true }
    } else {
        { !it.isFalse(comparison.invoke(it)) }
    }

    override suspend fun invokeRaw(runtime: ScriptRuntime): Any {
        runtime.withScope {
            assignment?.invoke(it)
            block(it)
        }
        return Unit
    }

    private suspend fun block(ctx: ScriptRuntime) {
        while (condition(ctx)) {
            currentCoroutineContext().ensureActive()
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

internal class OpForInLoop(
    private val prepare : Expression,
    private val assign : suspend (ScriptRuntime, Any?) -> Unit,
    private val inObject : Expression,
    private val body: Expression
) : Expression {

    override suspend fun invokeRaw(runtime: ScriptRuntime): Any {
        runtime.withScope {
            val o = inObject(it)

            syntaxCheck(o is JsAny){
                "$o is not iterable"
            }

            val keys = o.keys

            if (keys.isNotEmpty()){
                prepare(it)
            }

            for (k in keys) {
                currentCoroutineContext().ensureActive()
                try {
                    assign(it, k)
                    body(it)
                } catch (_: BlockContinue) {
                    continue
                } catch (_: BlockBreak) {
                    break
                }
            }
        }
        return Unit
    }
}


internal fun  OpDoWhileLoop(
    condition : Expression,
    body : OpBlock
) = Expression {
    do {
        currentCoroutineContext().ensureActive()
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
        currentCoroutineContext().ensureActive()
        try {
            body.invoke(it)
        } catch (_: BlockContinue) {
            continue
        } catch (_: BlockBreak) {
            break
        }
    }
}
