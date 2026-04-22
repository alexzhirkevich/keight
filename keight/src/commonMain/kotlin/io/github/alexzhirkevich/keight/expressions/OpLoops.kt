package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.callableOrNull
import io.github.alexzhirkevich.keight.fastMap
import io.github.alexzhirkevich.keight.js.Constants
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsSymbol
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.interpreter.syntaxCheck
import io.github.alexzhirkevich.keight.js.js


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

    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
        runtime.withScope {
            assignment?.invoke(it)
            block(it)
            Undefined
        }
        return Undefined
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
    private val assign : suspend (ScriptRuntime, JsAny?) -> Unit,
    private val inObject : Expression,
    private val body: Expression,
    override var label: String? = null
) : Expression(), Labeled {

    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
        runtime.withScope {
            val o = inObject(it)

            syntaxCheck(o is JsAny){
                "$o is not iterable"
            }

            val keys = o.keys(runtime).fastMap { runtime.toString(it) }

            if (keys.isNotEmpty()){
                prepare(it)
            }

            for (k in keys) {
                try {
                    assign(it, k.js)
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
            Undefined
        }
        return Undefined
    }
}

/**
 * for...of loop - iterates over iterable values (not keys)
 */
internal class OpForOfLoop(
    private val assign : suspend (ScriptRuntime, JsAny?) -> Unit,
    private val iterable: Expression,
    private val body: Expression,
    override var label: String? = null
) : Expression(), Labeled {

    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
        runtime.withScope {
            val iterableObj = iterable(it)

            syntaxCheck(iterableObj is JsAny){
                "$iterableObj is not iterable"
            }

            // Get iterator by calling Symbol.iterator on the iterable object
            val iteratorFn = iterableObj.get(JsSymbol.iterator, it)
                ?: return@withScope Undefined

            val callable = iteratorFn.callableOrNull()
                ?: return@withScope Undefined

            val iterator = callable
                .call(iterableObj, emptyList(), it)
                ?: return@withScope Undefined

            while (true) {

                val result = iterator.call(
                    func = Constants.next.js,
                    thisRef = iterator,
                    args = emptyList(),
                    isOptional = false,
                    runtime = it
                ) ?: break

                val done = !it.isFalse(result.get(Constants.done.js, it))

                if (done) {
                    break
                }

                val value = result.get(Constants.value.js, it)

                try {
                    assign(it, value)
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
        return Undefined
    }
}

internal class OpDoWhileLoop(
    val condition : Expression,
    val body : OpBlock,
    override var label: String? = null
) : Expression(), Labeled {
    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
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

        return Undefined
    }
}


internal class OpWhileLoop(
    val condition : Expression,
    val body : Expression,
    override var label: String? = null
) : Expression(), Labeled {
    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
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
        return Undefined
    }
}
