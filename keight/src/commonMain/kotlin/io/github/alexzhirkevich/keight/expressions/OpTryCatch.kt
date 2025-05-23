package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.findRoot
import io.github.alexzhirkevich.keight.js.CONSTRUCTOR
import io.github.alexzhirkevich.keight.js.SyntaxError
import io.github.alexzhirkevich.keight.js.JSError
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.ReferenceError
import io.github.alexzhirkevich.keight.js.TypeError
import io.github.alexzhirkevich.keight.js.interpreter.makeReferenceError
import io.github.alexzhirkevich.keight.js.interpreter.makeTypeError
import io.github.alexzhirkevich.keight.js.js

internal class ThrowableValue(val value : JsAny?) : JSError(value) {
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
) = Expression { r ->
    try {
        tryBlock(r)
    } catch (x: ScopeException) {
        throw x
    } catch (t: Throwable) {
        val t = when  {
            t is ReferenceError && t.get(CONSTRUCTOR, r) !== (r.findRoot() as JSRuntime).ReferenceError ->
                r.makeReferenceError { t.message.orEmpty().js()  }
            t is TypeError && t.get(CONSTRUCTOR, r) !== (r.findRoot() as JSRuntime).TypeError ->
                r.makeTypeError { t.message.orEmpty().js()  }
            else -> t
        }
        if (catchVariableName != null) {
            val throwable = if (t is ThrowableValue) t.value else t.js()
            r.withScope {
                it.set(catchVariableName.js(), throwable, VariableType.Local)
                catchBlock.invoke(it)
            }
        } else {
            catchBlock(r)
        }
    } finally {
        finallyBlock?.invoke(r)
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