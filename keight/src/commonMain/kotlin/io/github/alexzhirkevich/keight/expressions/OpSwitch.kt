package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.fastForEachIndexed

internal fun OpSwitch(
    value : Expression,
    cases : List<Expression>,
): Expression =  Expression { r ->
    val v = value(r)

    var defaultIndex = -1;
    var run = false
    try {
        cases.fastForEachIndexed { i, it ->
            when {
                run -> it(r)
                it is OpCase -> {
                    when {
                        it.value === OpCase.Default -> defaultIndex = i
                        OpEqualsImpl(it.value(r), v, true, r) -> run = true
                    }
                }
            }
        }
        if (defaultIndex >= 0) {
            for (i in defaultIndex until cases.size) {
                cases[i].invoke(r)
            }
        }
    } catch (_: BlockBreak) {

    }
}

internal class OpCase(
    val value : Expression,
) : Expression() {
    override suspend fun execute(runtime: ScriptRuntime): Any = Unit

    companion object {
        val Default = Expression {  }
    }
}

