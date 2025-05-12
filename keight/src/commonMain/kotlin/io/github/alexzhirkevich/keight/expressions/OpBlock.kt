package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.Undefined


internal data class OpBlock(
    val expressions: List<Expression>,
    val isScoped : Boolean,
    val isExpressible : Boolean = true,
    val isStrict : Boolean = false,
    override var label: String? = null,
    val isSurroundedWithBraces : Boolean,
) : Expression(), Labeled {

    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
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

        if (expressions.size > 1) {
            repeat(expressions.size - 1) {
                expressions[it].invoke(context)
            }
        }

        return expressions.last().invoke(context).let {
            if (isExpressible) it else Undefined
        }
    }
}


internal sealed class ScopeException : Throwable()
internal class BlockContinue(val label : String? = null) : ScopeException()
internal class BlockBreak(val label : String? = null) : ScopeException()
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

