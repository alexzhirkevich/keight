package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.expressions.OpConstant
import io.github.alexzhirkevich.keight.findRoot
import kotlin.jvm.JvmInline

@JvmInline
public value class JSSymbol(public val value : String) : JsAny {

    override val type: String get() = "symbol"

    override suspend fun proto(runtime: ScriptRuntime): Any? {
        return (runtime.findRoot() as JSRuntime).Symbol.get(PROTOTYPE, runtime)
    }

    override fun toString(): String {
        return "Symbol($value)"
    }

    public companion object {
        public val toPrimitive: JSSymbol = JSSymbol("Symbol.toPrimitive",)
        public val toStringTag: JSSymbol = JSSymbol("Symbol.toStringTag")

        internal val defaults = mapOf(
            "toPrimitive" to toPrimitive,
            "toStringTag" to toStringTag
        )
    }
}