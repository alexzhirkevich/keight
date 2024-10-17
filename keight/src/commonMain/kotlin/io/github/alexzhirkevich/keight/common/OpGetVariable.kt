package io.github.alexzhirkevich.keight.common

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.ecmascript.ESAny
import io.github.alexzhirkevich.keight.ecmascript.unresolvedReference
import io.github.alexzhirkevich.keight.invoke
import kotlin.jvm.JvmInline

@JvmInline
internal value class OpConstant(val value: Any?) : Expression {
    override fun invokeRaw(context: ScriptRuntime): Any? {
        return value
    }
}

private object UNINITIALIZED

internal class OpLazy(
    private val init : (ScriptRuntime) -> Any?
) : Expression {

    private var value : Any? = UNINITIALIZED

    override fun invokeRaw(context: ScriptRuntime): Any? {

        if (value is UNINITIALIZED){
            value = init(context)
        }

        return value
    }
}

internal class OpGetVariable(
    val name : String,
    val receiver : Expression?,
    val assignmentType : VariableType? = null
) : Expression {

    override fun invokeRaw(context: ScriptRuntime, ): Any? {
        return if (assignmentType != null) {
            context.set(name, 0f, assignmentType)
        } else {
            when (val res = receiver?.invoke(context)) {
                is ESAny -> res[name]
                else ->
                    if (name in context) {
                        context[name]
                    } else {
                        unresolvedReference(name)
                    }
            }
        }
    }
}