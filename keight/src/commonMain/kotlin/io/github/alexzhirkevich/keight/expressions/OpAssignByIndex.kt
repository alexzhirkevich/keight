package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JSObject
import io.github.alexzhirkevich.keight.js.JsAny

internal class OpAssignByIndex(
    private val receiver : Expression,
    private val index : Expression,
    private val assignableValue : Expression,
    private val merge : (suspend ScriptRuntime.(Any?, Any?) -> Any?)?
) : Expression() {

    override suspend fun execute(runtime: ScriptRuntime): Any? {
        return invoke(
            receiver = receiver,
            index = index,
            value = assignableValue.invoke(runtime),
            merge = merge,
            runtime = runtime
        )
    }

    companion object {
        suspend fun invoke(
            receiver: Expression,
            index: Expression,
            value : Any?,
            merge: (suspend ScriptRuntime.(Any?, Any?) -> Any?)?,
            runtime: ScriptRuntime
        ) : Any? {
            val rec = receiver.invoke(runtime) as JsAny
            val idx = index(runtime)

            return when (rec) {
                is MutableList<*> -> {
                    rec as MutableList<Any?>
                    val i = runtime.toNumber(idx)

                    check(!i.toDouble().isNaN()) {
                        "Unexpected index: $i"
                    }
                    val index = i.toInt()

                    while (rec.lastIndex < index) {
                        rec.add(Unit)
                    }

                    val c = rec[index]

                    rec[index] = if (c !is Unit && merge != null) {
                        merge.invoke(runtime, c, value)
                    } else {
                        value
                    }
                    rec[index]
                }
                is JSObject -> {
                    if (rec.contains(idx, runtime) && merge != null){
                        rec.set(
                            property = idx,
                            value = merge.invoke(runtime, rec.get(idx, runtime), value),
                            runtime = runtime,
                        )
                    } else {
                        rec.set(idx, value, runtime)
                    }
                    rec.get(idx, runtime)
                }
                else -> error("Can't assign '$value' by index ($idx)")
            }
        }
    }
}