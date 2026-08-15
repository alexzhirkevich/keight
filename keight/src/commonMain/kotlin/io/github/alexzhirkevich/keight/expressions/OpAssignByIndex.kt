package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsObject
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JSFunction
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

            // V8-style inferred frame name for computed-key property assignment,
            // e.g. `obj["key"] = fn` -> `at obj.key`, `obj[expr] = fn` ->
            // `at obj.<computed> [as <value>]`. Mirrors OpAssign: `.name` is left
            // empty (V8 only sets it for lexical bindings), the frame name is
            // inferred instead, and first assignment wins.
            if (merge == null && value is JSFunction && value.name.isEmpty() && value.inferredName == null) {
                val recv = receiver.referencedName()
                if (recv != null) {
                    value.setInferredName("$recv.${formatComputedKey(index, idx)}")
                }
            }

            return when (rec) {
                is MutableList<*> -> {
                    @Suppress("UNCHECKED_CAST")
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

/**
 * Builds the trailing part of an inferred frame name for a computed-key property
 * assignment (`obj[key] = fn`). A literal key (`obj["key"]`, `obj[7]`) is shown
 * in dotted form (`obj.key`, `obj.7`), while any computed expression
 * (`obj[expr]`, even if it evaluates to a string) is shown as V8 does:
 * `obj.<computed> [as <value>]`.
 */
private fun formatComputedKey(index: Expression, idx: JsAny?): String {
    return if (index is OpConstant) {
        idx?.toString() ?: "undefined"
    } else {
        "<computed> [as ${idx?.toString() ?: "undefined"}]"
    }
}