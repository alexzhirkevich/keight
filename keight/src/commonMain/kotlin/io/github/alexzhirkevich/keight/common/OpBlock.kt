package io.github.alexzhirkevich.keight.common

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.invoke
import kotlin.jvm.JvmInline


internal class OpBlock(
    val expressions: List<Expression>,
    private val scoped : Boolean,
) : Expression {

    override fun invokeRaw(context: ScriptRuntime): Any? {
        return if (scoped) {
            context.withScope(block = ::invokeInternal)
        } else {
            invokeInternal(context)
        }
    }

    private fun invokeInternal(context: ScriptRuntime): Any? {
        if (expressions.isEmpty()) {
            return Unit
        }

        if (expressions.size > 1) {
            repeat(expressions.size - 1) {
                invoke(expressions[it], context)
            }
        }

        return invoke(expressions.last(), context)
    }

    private fun invoke(expression: Expression, context: ScriptRuntime) : Any? {
        return expression.invoke(context)
    }
}


internal sealed class BlockException : Throwable()
internal data object BlockContinue : BlockException()
internal data object BlockBreak : BlockException()
internal class BlockReturn(val value: Any?) : BlockException()

@JvmInline
internal value class OpReturn(
    val value : Expression
) : Expression {
    override fun invokeRaw(context: ScriptRuntime): Any? {
        throw BlockReturn(value(context))
    }
}

internal object OpContinue : Expression {
    override fun invokeRaw(context: ScriptRuntime): Any? {
        throw BlockContinue
    }
}
internal object OpBreak : Expression {
    override fun invokeRaw(context: ScriptRuntime): Any? {
        throw BlockBreak
    }
}

