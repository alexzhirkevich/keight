package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.invoke
import io.github.alexzhirkevich.keight.js.TypeError
import io.github.alexzhirkevich.keight.js.unresolvedReference
import kotlin.jvm.JvmInline

@JvmInline
internal value class OpConstant(val value: Any?) : Expression {
    override suspend fun invokeRaw(context: ScriptRuntime): Any? {
        return value
    }
}

private object UNINITIALIZED

internal class OpLazy(
    private val init : suspend (ScriptRuntime) -> Any?
) : Expression {

    private var value : Any? = UNINITIALIZED

    override suspend fun invokeRaw(context: ScriptRuntime): Any? {

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

    override suspend fun invokeRaw(context: ScriptRuntime, ): Any? {
        return if (assignmentType != null) {
            context.set(name, OpConstant(Unit), assignmentType)
        } else {
            getImpl(receiver, context)
        }
    }

    private tailrec suspend fun getImpl(res: Any?, runtime: ScriptRuntime): Any? {
        return when (res) {
            is Expression -> getImpl(res.invoke(runtime), runtime)
            is JsAny -> res.get(name, runtime)
            null -> if (name in runtime) {
                runtime.get(name)
            } else {
                unresolvedReference(name)
            }

            else -> throw TypeError("Cannot get properties of $res")
        }
    }
}