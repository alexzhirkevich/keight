package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.es.ESAny
import io.github.alexzhirkevich.keight.es.ESObject
import io.github.alexzhirkevich.keight.es.TypeError
import io.github.alexzhirkevich.keight.invoke

internal class OpAssign(
    val type : VariableType? = null,
    val variableName : String,
    val receiver : Expression?=null,
    var isStatic : Boolean = false,
    val assignableValue : Expression,
    private val merge : ((Any?, Any?) -> Any?)?
) : Expression {

    override fun invokeRaw(context: ScriptRuntime): Any? {
        val v = assignableValue.invoke(context)
        val r = receiver?.invoke(context)

        val current = if (receiver == null) {
            context[variableName]
        } else {
            when (r){
                is ESAny -> r[variableName]
                else -> null
            }
        }

        check(merge == null || current != null) {
            "Cant modify $variableName as it is undefined"
        }

        val value = if (current != null && merge != null) {
            merge.invoke(current, v)
        } else v

        if (receiver == null) {
            context.set(
                variable = variableName,
                value = value,
                type = type
            )
        } else {
            when (r) {
                is ESObject -> r[variableName] = value
                else -> throw TypeError("Cannot set properties of ${if (r == Unit) "undefined" else r} (setting '$variableName')")
            }
        }

        return value
    }
}