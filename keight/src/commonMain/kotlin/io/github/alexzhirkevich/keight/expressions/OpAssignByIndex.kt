package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JSObject
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsArrayWrapper
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck

internal class OpAssignByIndex(
    private val receiver : Expression,
    private val index : Expression,
    private val assignableValue : Expression,
    private val merge : (ScriptRuntime.(Any?, Any?) -> Any?)?
) : Expression() {

    override suspend fun execute(runtime: ScriptRuntime): Any? {
        val rec = receiver.invoke(runtime) as JsAny
        val idx = index(runtime)
        val v = assignableValue.invoke(runtime)

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

                rec[index] = if (c !is Unit && merge != null){
                    merge.invoke(runtime, c, v)
                } else {
                    v
                }
                rec[index]
            }
            is JSObject -> {
                if (rec.contains(idx, runtime) && merge != null){
                    rec.set(
                        property = idx,
                        value = merge.invoke(runtime, rec.get(idx, runtime), v),
                        runtime = runtime,
                    )
                } else {
                    rec.set(idx, v, runtime)
                }
                rec.get(idx, runtime)
            }
            else -> error("Can't assign '$v' by index ($idx)")
        }
    }
}