package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.fastMap
import io.github.alexzhirkevich.keight.js.JSStringFunction
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.interpreter.syntaxCheck



internal class OpForLoop(
    private val assignment : Expression?,
    private val increment: Expression?,
    private val comparison : Expression?,
    private val body: Expression,
    override var label: String? = null
) : Expression(), Labeled {

    private val condition: suspend (ScriptRuntime) -> Boolean = if (comparison == null) {
        { true }
    } else {
        { !it.isFalse(comparison.invoke(it)) }
    }

    override suspend fun execute(runtime: ScriptRuntime): Any {
        runtime.withScope {
            assignment?.invoke(it)
            block(it)
        }
        return Unit
    }

    private suspend fun block(ctx: ScriptRuntime) {
        while (condition(ctx)) {
            try {
                body(ctx)
            } catch (t: BlockContinue) {
                if (t.label == label) {
                    continue
                } else {
                    throw t
                }
            } catch (t: BlockBreak) {
                if (t.label == label) {
                    break
                } else {
                    throw t
                }
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
    private val body: Expression,
    override var label: String? = null
) : Expression(), Labeled {

    override suspend fun execute(runtime: ScriptRuntime): Any {
        runtime.withScope {
            val o = inObject(it)

            syntaxCheck(o is JsAny){
                "$o is not iterable"
            }

            val keys = o.keys(runtime).fastMap { JSStringFunction.toString(it, runtime) }

            if (keys.isNotEmpty()){
                prepare(it)
            }

            for (k in keys) {
                try {
                    assign(it, k)
                    body(it)
                } catch (t: BlockContinue) {
                    if (t.label == label) {
                        continue
                    } else {
                        throw t
                    }
                } catch (t: BlockBreak) {
                    if (t.label == label) {
                        break
                    } else {
                        throw t
                    }
                }
            }
        }
        return Unit
    }
}

internal class OpDoWhileLoop(
    val condition : Expression,
    val body : OpBlock,
    override var label: String? = null
) : Expression(), Labeled {
    override suspend fun execute(runtime: ScriptRuntime): Any? {
        do {
            val cond = runtime.withScope {
                try {
                    body.invoke(it)
                    !it.isFalse(condition.invoke(it))
                } catch (t: BlockContinue) {
                    if (t.label == label) {
                        !it.isFalse(condition.invoke(it))
                    } else {
                        throw t
                    }
                } catch (t: BlockBreak) {
                    if (t.label == label) {
                        false
                    } else {
                        throw t
                    }
                }
            }
        } while (cond)
        return Unit
    }
}


internal class OpWhileLoop(
    val condition : Expression,
    val body : Expression,
    override var label: String? = null
) : Expression(), Labeled {
    override suspend fun execute(runtime: ScriptRuntime): Any? {
        while (!runtime.isFalse(condition.invoke(runtime))) {
            try {
                body.invoke(runtime)
            } catch (t: BlockContinue) {
                if (t.label == label) {
                    continue
                } else {
                    throw t
                }
            } catch (t: BlockBreak) {
                if (t.label == label) {
                    break
                } else {
                    throw t
                }
            }
        }
        return Unit
    }
}
