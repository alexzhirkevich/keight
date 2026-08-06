package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.fastForEachIndexed
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.Undefined

internal fun OpSwitch(
    value : Expression,
    cases : List<Expression>,
): Expression =  Expression { r ->
    val v = value(r)
    var defaultIndex = -1;
    var run = false
    // Issue #23: completion value is the value of the last evaluated statement in the
    // matched path (matching ECMAScript fall-through semantics).
    var last: JsAny? = Undefined
    var broke = false
    try {
        cases.fastForEachIndexed { i, it ->
            if (broke) return@fastForEachIndexed
            when {
                run -> last = it(r)
                it is OpCase -> {
                    when {
                        it.value === OpCase.Default -> defaultIndex = i
                        OpEqualsImpl(it.value(r), v, true, r) -> run = true
                    }
                }
            }
        }
        if (!broke && defaultIndex >= 0) {
            for (i in defaultIndex until cases.size) {
                last = cases[i].invoke(r)
            }
        }
    } catch (t: BlockBreak) {
        // A `break` inside a case carries the case-body completion value (set by OpBlock when
        // the case body is itself an `OpBlock`), so the switch still reports the last statement's
        // value. When the break is a plain `break` (value == null) — the common case where the
        // case body is a flat list of statements — `last` already holds the last evaluated
        // statement, so we keep it rather than overwriting with `null`. `broke` also prevents the
        // default block from running after a break.
        if (t.value != null) last = t.value
        broke = true
    } catch (e: BlockReturn) {
        throw e
    }
    last
}

internal class OpCase(
    val value : Expression,
) : Expression() {
    override suspend fun execute(runtime: ScriptRuntime): JsAny? = Undefined

    companion object {
        val Default = Expression { Undefined }
    }
}

