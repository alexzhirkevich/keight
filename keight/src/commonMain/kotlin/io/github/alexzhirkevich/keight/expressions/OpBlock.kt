package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.Undefined
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive


internal data class OpBlock(
    val expressions: List<Expression>,
    val isScoped : Boolean,
    val isExpressible : Boolean = true,
    val isStrict : Boolean = false,
    override var label: String? = null,
    val isSurroundedWithBraces : Boolean,
) : Expression(), Labeled {

    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
        currentCoroutineContext().ensureActive()
        return try {
            when {
                isScoped -> runtime.withScope(
                    block = ::invokeInternal,
                    isStrict = isStrict
                )

                isStrict -> runtime.useStrict(::invokeInternal)
                else -> invokeInternal(runtime)
            }
        } catch (t: BlockBreak) {
            if (label != null && t.label == label) {
                return Undefined
            } else {
                throw t
            }
        }
    }

    private suspend fun invokeInternal(context: ScriptRuntime): JsAny? {
        if (expressions.isEmpty()) {
            return Undefined
        }

        // Track the completion value of the last normally-completed statement so that a
        // `break`/`continue` (which aborts the block) can still propagate it. This makes
        // e.g. `switch(x){ case 1: 'a'; break }` complete with `'a'` (ECMAScript semantics).
        var last: JsAny? = Undefined
        try {
            for (i in expressions.indices) {
                last = expressions[i].invoke(context)
            }
        } catch (t: BlockBreak) {
            if (label != null && t.label == label) {
                // Targeted labelled break: the enclosing loop/switch handles the value.
                throw t
            }
            t.value = if (isExpressible) last else Undefined
            throw t
        } catch (t: BlockContinue) {
            if (label != null && t.label == label) {
                throw t
            }
            t.value = if (isExpressible) last else Undefined
            throw t
        }

        return if (isExpressible) last else Undefined
    }
}


/**
 * Return an `Expression` whose completion value is the value of its last normally-completed
 * statement. `parseBlock` wraps control-structure bodies/branches in an `OpBlock` whose
 * `isExpressible` defaults to `false`, which discards the inner completion value. For
 * ECMAScript completion-value semantics (Issue #23) we must mark those blocks expressible so
 * the enclosing `if`/`for`/`while`/`try`/... can propagate the body's last statement value.
 */
internal fun Expression.asExpressible(): Expression {
    val block = this as? OpBlock ?: return this
    return if (block.isExpressible) this else block.copy(isExpressible = true)
}


internal sealed class ScopeException : Throwable()
internal class BlockContinue(val label : String? = null, var value: JsAny? = null) : ScopeException()
internal class BlockBreak(val label : String? = null, var value: JsAny? = null) : ScopeException()
internal class BlockReturn(val value: JsAny?) : ScopeException()

internal class OpReturn(
    val value : Expression
) : Expression() {
    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
        throw BlockReturn(value(runtime))
    }
}

internal class OpContinue(private val label: String? = null) : Expression() {
    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
        throw BlockContinue(label)
    }
}
internal class OpBreak(private val label: String? = null) : Expression() {
    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
        throw BlockBreak(label)
    }
}

