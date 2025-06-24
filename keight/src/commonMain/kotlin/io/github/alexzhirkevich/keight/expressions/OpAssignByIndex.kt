package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.Undefined

internal class OpAssignByIndex(
    private val receiver : Expression,
    private val index : Expression,
    private val assignableValue : Expression,
    private val merge : (suspend ScriptRuntime.(JsAny?, JsAny?) -> JsAny?)?
) : Expression() {

    override suspend fun execute(runtime: ScriptRuntime): JsAny? {
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
            value : JsAny?,
            merge: (suspend ScriptRuntime.(JsAny?, JsAny?) -> JsAny?)?,
            runtime: ScriptRuntime
        ) : JsAny? {
            val rec = receiver.invoke(runtime) as JsAny
            val idx = index(runtime)

            return when (rec) {
                is MutableList<*> -> {
                    rec as MutableList<JsAny?>
                    val i = runtime.toNumber(idx)

                    check(!i.toDouble().isNaN()) {
                        "Unexpected index: $i"
                    }
                    val index = i.toInt()

                    while (rec.lastIndex < index) {
                        rec.add(Undefined)
                    }

                    val c = rec[index]

                    rec[index] = if (c !is Undefined && merge != null) {
                        merge.invoke(runtime, c, value)
                    } else {
                        value
                    }
                    rec[index]
                }
                is JsObject -> {
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