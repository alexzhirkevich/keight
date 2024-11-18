package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.js.SyntaxError
import io.github.alexzhirkevich.keight.js.JSError
import io.github.alexzhirkevich.keight.js.PROTOTYPE
import io.github.alexzhirkevich.keight.js.ReferenceError
import io.github.alexzhirkevich.keight.js.TypeError
import io.github.alexzhirkevich.keight.js.interpreter.makeReferenceError
import io.github.alexzhirkevich.keight.js.interpreter.makeTypeError
import io.github.alexzhirkevich.keight.js.interpreter.referenceError

internal class ThrowableValue(val value : Any?) : JSError(value) {
    override fun toString(): String {
        return value.toString() + " (thrown)"
    }
}

internal fun OpTryCatch(
    tryBlock : Expression,
    catchVariableName : String?,
    catchBlock : Expression?,
    finallyBlock : Expression?,
) = when {
    catchBlock != null ->
        TryCatchFinally(
            tryBlock = tryBlock,
            catchVariableName = catchVariableName,
            catchBlock = catchBlock,
            finallyBlock = finallyBlock
        )

    finallyBlock != null -> TryFinally(
        tryBlock = tryBlock,
        finallyBlock = finallyBlock
    )
    else -> throw SyntaxError("Missing catch or finally after try")
}

private fun TryCatchFinally(
    tryBlock : Expression,
    catchVariableName : String?,
    catchBlock : Expression,
    finallyBlock : Expression? = null,
) = Expression {
    try {
        tryBlock(it)
    } catch (x: ScopeException) {
        throw x
    } catch (t: Throwable) {
        val t = when  {
            t is ReferenceError && t.get("constructor", it) == Unit ->
                it.makeReferenceError { t.message.orEmpty()  }
            t is TypeError && t.get("constructor", it) == Unit ->
                it.makeTypeError { t.message.orEmpty()  }
            else -> t
        }
        if (catchVariableName != null) {
            val throwable = if (t is ThrowableValue) t.value else t
            it.withScope(
                extraProperties = mapOf(catchVariableName to (VariableType.Local to throwable)),
                block = catchBlock::invoke
            )
        } else {
            catchBlock(it)
        }
    } finally {
        finallyBlock?.invoke(it)
    }
}


private fun TryFinally(
    tryBlock : Expression,
    finallyBlock : Expression,
) = Expression {
    try {
        tryBlock(it)
    } finally {
        finallyBlock(it)
    }
}