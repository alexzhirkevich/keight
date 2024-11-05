package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import kotlin.jvm.JvmInline


internal data class OpBlock(
    val expressions: List<Expression>,
    val isScoped : Boolean,
    val isExpressible : Boolean = true,
    val isSurroundedWithBraces : Boolean
) : Expression() {

    override suspend fun execute(runtime: ScriptRuntime): Any? {
        return if (isScoped) {
            runtime.withScope(block = ::invokeInternal)
        } else {
            invokeInternal(runtime)
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

        return invoke(expressions.last(), context).let {
            if (isExpressible) it else Unit
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

internal class OpReturn(
    val value : Expression
) : Expression() {
    override suspend fun execute(runtime: ScriptRuntime): Any? {
        throw BlockReturn(value(runtime))
    }
}

internal object OpContinue : Expression() {
    override suspend fun execute(runtime: ScriptRuntime): Any? {
        throw BlockContinue
    }
}
internal object OpBreak : Expression() {
    override suspend fun execute(runtime: ScriptRuntime): Any? {
        throw BlockBreak
    }
}

