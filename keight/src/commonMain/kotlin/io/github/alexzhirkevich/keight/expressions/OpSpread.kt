package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import kotlin.jvm.JvmInline

internal class OpSpread(val value : Expression) : Expression() {
    override suspend fun execute(runtime: ScriptRuntime): Any? {
        return value.invoke(runtime)
    }
}