package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.findJsRoot
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

// Issue #23: the completion value of a try statement is the value of the try block, or of the
// catch block when the try block threw. `parseBlock` marks both as non-expressible, so they are
// made expressible at construction time below. The finally block never contributes a value
// unless it completes abruptly - which Kotlin's `finally` already models.
private fun TryCatchFinally(
    tryBlock : Expression,
    catchVariableName : String?,
    catchBlock : Expression,
    finallyBlock : Expression? = null,
) : Expression {
    val tryBlockE = tryBlock.asExpressible()
    val catchBlockE = catchBlock.asExpressible()
    return Expression { r ->
        try {
            tryBlockE.invoke(r)
        } catch (x: ScopeException) {
            throw x
        } catch (t: Throwable) {
            val t = when  {
                t is ReferenceError && t.get(CONSTRUCTOR, r) !== (r.findJsRoot()).ReferenceError ->
                    r.makeReferenceError { t.message.orEmpty().js  }
                t is TypeError && t.get(CONSTRUCTOR, r) !== (r.findJsRoot()).TypeError ->
                    r.makeTypeError { t.message.orEmpty().js  }
                else -> t
            }
            if (catchVariableName != null) {
                val throwable = if (t is ThrowableValue) t.value else t.js
                r.withScope {
                    it.set(catchVariableName.js, throwable, VariableType.Local)
                    catchBlockE.invoke(it)
                }
            } else {
                catchBlockE.invoke(r)
            }
        } finally {
            finallyBlock?.invoke(r)
        }
    }
}


// Issue #23: see TryCatchFinally - the finally block's value is discarded.
private fun TryFinally(
    tryBlock : Expression,
    finallyBlock : Expression,
) : Expression {
    val tryBlockE = tryBlock.asExpressible()
    return Expression {
        try {
            tryBlockE.invoke(it)
        } finally {
            finallyBlock(it)
        }
    }
}