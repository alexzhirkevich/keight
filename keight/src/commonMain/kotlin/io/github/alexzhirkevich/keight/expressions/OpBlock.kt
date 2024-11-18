package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import kotlin.jvm.JvmInline


internal data class OpBlock(
    val expressions: List<Expression>,
    val isScoped : Boolean,
    val isExpressible : Boolean = true,
    val isStrict : Boolean = false,
    val isSurroundedWithBraces : Boolean
) : Expression() {

    override suspend fun execute(runtime: ScriptRuntime): Any? {
        return when {
            isScoped -> runtime.withScope(
                block = ::invokeInternal,
                isStrict = isStrict
            )
            isStrict -> runtime.useStrict(::invokeInternal)
            else -> invokeInternal(runtime)
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

