package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.invoke
import kotlin.jvm.JvmInline


internal class OpBlock(
    val expressions: List<Expression>,
    private val isScoped : Boolean,
    private val isExpressible : Boolean = true
) : Expression {

    override suspend fun invokeRaw(context: ScriptRuntime): Any? {
        return if (isScoped) {
            context.withScope(block = ::invokeInternal)
        } else {
            invokeInternal(context)
        }
    }

    private suspend fun invokeInternal(context: ScriptRuntime): Any? {
        if (expressions.isEmpty()) {
            return Unit
        }

        if (expressions.size > 1) {
            repeat(expressions.size - 1) {
                invoke(expressions[it], context)
            }
        }

        invoke(expressions.last(), context).let {
            return if (isExpressible) it else Unit
        }
    }

    private suspend fun invoke(expression: Expression, context: ScriptRuntime) : Any? {
        return expression.invoke(context)
    }
}


internal sealed class ScopeException : Throwable()
internal data object BlockContinue : ScopeException()
internal data object BlockBreak : ScopeException()
internal class BlockReturn(val value: Any?) : ScopeException()

@JvmInline
internal value class OpReturn(
    val value : Expression
) : Expression {
    override suspend fun invokeRaw(context: ScriptRuntime): Any? {
        throw BlockReturn(value(context))
    }
}

internal object OpContinue : Expression {
    override suspend fun invokeRaw(context: ScriptRuntime): Any? {
        throw BlockContinue
    }
}
internal object OpBreak : Expression {
    override suspend fun invokeRaw(context: ScriptRuntime): Any? {
        throw BlockBreak
    }
}

