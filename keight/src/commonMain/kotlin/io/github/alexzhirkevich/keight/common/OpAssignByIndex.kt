package io.github.alexzhirkevich.keight.common

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.ecmascript.ESObject
import io.github.alexzhirkevich.keight.invoke
import io.github.alexzhirkevich.keight.javascript.JsArray

internal class OpAssignByIndex(
    private val variableName : String,
    private val scope : VariableType?,
    private val index : Expression,
    private val assignableValue : Expression,
    private val merge : ((Any?, Any?) -> Any?)?
) : Expression {

    override tailrec fun invokeRaw(context: ScriptRuntime): Any? {
        val v = assignableValue.invoke(context)
        val current = context.get(variableName)

        check(merge == null || current != null) {
            "Cant modify $variableName as it is undefined"
        }

        if (current == null) {
            context.set(variableName, mutableListOf<Any>(), scope)
            return invoke(context)
        } else {


            val idx = index(context)

            return when (current) {

                is JsArray-> {
                    val i = context.toNumber(idx)

                    check(!i.toDouble().isNaN()) {
                        "Unexpected index: $i"
                    }
                    val index = i.toInt()

                    while (current.value.lastIndex < index) {
                        current.value.add(Unit)
                    }

                    val c = current.value[index]

                    current.value[index] = if (current.value[index] !is Unit && merge != null){
                        merge.invoke(c,v)
                    } else {
                        v
                    }
                    current.value[index]
                }
                is ESObject -> {
                    
                    if (idx in current && merge != null){
                        current[idx] = merge.invoke(current[idx], v)
                    } else {
                        current[idx] = v
                    }
                }
                else -> error("Can't assign '$current' by index ($index)")
            }
        }
    }
}