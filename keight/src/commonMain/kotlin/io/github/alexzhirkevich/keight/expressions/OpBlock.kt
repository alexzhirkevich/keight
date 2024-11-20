package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import kotlin.jvm.JvmInline


internal data class OpBlock(
    val expressions: List<Expression>,
    val isScoped : Boolean,
    val isExpressible : Boolean = true,
    val isStrict : Boolean = false,
    override var label: String? = null,
    val isSurroundedWithBraces : Boolean,
) : Expression(), Labeled {

    override suspend fun execute(runtime: ScriptRuntime): Any? {
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
                return Unit
            } else {
                throw t
            }
        }
    }

    private suspend fun invokeInternal(context: ScriptRuntime): Any? {
        if (expressions.isEmpty()) {
            return Unit
        }

        if (expressions.size > 1) {
            repeat(expressions.size - 1) {
                expressions[it].invoke(context)
            }
        }

        return expressions.last().invoke(context).let {
            if (isExpressible) it else Unit
        }
    }
}


internal sealed class ScopeException : Throwable()
internal class BlockContinue(val label : String? = null) : ScopeException()
internal class BlockBreak(val label : String? = null) : ScopeException()
internal class BlockReturn(val value: Any?) : ScopeException()

internal class OpReturn(
    val value : Expression
) : Expression() {
    override suspend fun execute(runtime: ScriptRuntime): Any? {
        throw BlockReturn(value(runtime))
    }
}

internal class OpContinue(private val label: String? = null) : Expression() {
    override suspend fun execute(runtime: ScriptRuntime): Any? {
        throw BlockContinue(label)
    }
}
internal class OpBreak(private val label: String? = null) : Expression() {
    override suspend fun execute(runtime: ScriptRuntime): Any? {
        throw BlockBreak(label)
    }
}

