package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JSFunction


internal interface Labeled {
    var label: String?
}


internal class OpKeyValuePair(
    val key : String,
    val value : Expression
) : Expression() {

    init {
        if (value is Labeled){
            value.label = key
        }
    }

    override suspend fun execute(runtime: ScriptRuntime): Any? {
        return value.invoke(runtime)
    }
}

internal interface PropertyAccessorFactory {
    val value : JSFunction
}

internal class OpGetter(override val value : JSFunction) : Expression(), PropertyAccessorFactory {
    override suspend fun execute(runtime: ScriptRuntime): Any? {
        return value
    }
}

internal class OpSetter(override val value : JSFunction) : Expression(), PropertyAccessorFactory {
    override suspend fun execute(runtime: ScriptRuntime): Any? {
        return value
    }
}