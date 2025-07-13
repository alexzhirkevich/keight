package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.findJsRoot
import kotlin.jvm.JvmInline

@JvmInline
public value class JsSymbol(public val value : String) : JsAny {

    override val type: String get() = "symbol"

    override suspend fun proto(runtime: ScriptRuntime): JsAny? {
        return runtime.findJsRoot().Symbol.get(PROTOTYPE, runtime)
    }

    override fun toString(): String {
        return "Symbol($value)"
    }

    public companion object {
        public val toPrimitive: JsSymbol = JsSymbol("Symbol.toPrimitive",)
        public val toStringTag: JsSymbol = JsSymbol("Symbol.toStringTag")
        public val iterator: JsSymbol = JsSymbol("Symbol.iterator")
        public val match: JsSymbol = JsSymbol("Symbol.match")

        internal val WellKnown = mapOf(
            "toPrimitive" to toPrimitive,
            "toStringTag" to toStringTag,
            "iterator" to iterator,
            "match" to match,
        )
    }
}