package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.js.JSObject
import io.github.alexzhirkevich.keight.invoke
import io.github.alexzhirkevich.keight.js.JsArrayWrapper

internal class OpAssignByIndex(
    private val variableName : String,
    private val scope : VariableType?,
    private val index : Expression,
    private val assignableValue : Expression,
    private val merge : (ScriptRuntime.(Any?, Any?) -> Any?)?
) : Expression {

    override tailrec suspend fun invokeRaw(runtime: ScriptRuntime): Any? {
        val v = assignableValue.invoke(runtime)
        val current = runtime.get(variableName)

        check(merge == null || current != null) {
            "Cant modify $variableName as it is undefined"
        }

        if (current == null) {
            runtime.set(variableName, mutableListOf<Any>(), scope)
            return invoke(runtime)
        } else {


            val idx = index(runtime)

            return when (current) {

                is JsArrayWrapper-> {
                    val i = runtime.toNumber(idx)

                    check(!i.toDouble().isNaN()) {
                        "Unexpected index: $i"
                    }
                    val index = i.toInt()

                    while (current.value.lastIndex < index) {
                        current.value.add(Unit)
                    }

                    val c = current.value[index]

                    current.value[index] = if (current.value[index] !is Unit && merge != null){
                        merge.invoke(runtime, c,v)
                    } else {
                        v
                    }
                    current.value[index]
                }
                is JSObject -> {
                    
                    if (current.contains(idx, runtime) && merge != null){
                        current[idx] = merge.invoke(runtime, current.get(idx, runtime), v)
                    } else {
                        current[idx] = v
                    }
                }
                else -> error("Can't assign '$current' by index ($index)")
            }
        }
    }
}