package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JSFunction
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.SyntaxError


internal interface Labeled {
    var label: String?
}

internal class OpColonAssignment(
    val key : String,
    val value : Expression
) : Expression() {


    init {
        if (value is Labeled){
            value.label = key
        }
    }

    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
        return value.invoke(runtime)
    }
}

internal interface PropertyAccessorFactory {
    val value : JSFunction
}

internal class OpGetter(override val value : JSFunction) : Expression(), PropertyAccessorFactory {
    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
        return value
    }
}

internal class OpSetter(override val value : JSFunction) : Expression(), PropertyAccessorFactory {
    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
        return value
    }
}