package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.interpreter.typeError
import io.github.alexzhirkevich.keight.js.js

internal class OpConstant(val value: JsAny?) : Expression() {
    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
        return value
    }
}


internal class OpGetProperty(
    val name : String,
    val receiver : Expression?,
    val isOptional : Boolean = false
) : Expression() {

    private val nameJs = name.js

    override suspend fun execute(runtime: ScriptRuntime, ): JsAny? {
        return when {
            receiver == null -> if (runtime.contains(nameJs)) {
                runtime.get(nameJs)
            } else {
                runtime.referenceError("$nameJs is not defined".js)
            }
            else -> invoke(receiver.invoke(runtime), isOptional, nameJs, runtime)
        }
    }

    companion object {
        suspend fun invoke(receiver: JsAny?, isOptional: Boolean, property : JsAny?, runtime: ScriptRuntime): JsAny? {
            return when {
                isOptional && (receiver == null || receiver == Undefined) -> Undefined
                receiver != null -> receiver.get(property, runtime)
                else -> runtime.typeError { "Cannot get properties of $receiver".js }
            }
        }
    }
}

/**
 * Best-effort textual source name of an expression, used to build V8-style
 * inferred frame names for property assignments. Returns the dotted identifier
 * path for simple identifiers and member chains (`a`, `a.b`, `a.b.c`), or `null`
 * for anything that is not a plain reference (calls, literals, complex
 * expressions) where a meaningful name cannot be derived.
 */
internal fun Expression.referencedName(): String? = when (this) {
    is OpGetProperty -> {
        val base = receiver?.referencedName()
        if (base == null) name else "$base.$name"
    }
    else -> null
}