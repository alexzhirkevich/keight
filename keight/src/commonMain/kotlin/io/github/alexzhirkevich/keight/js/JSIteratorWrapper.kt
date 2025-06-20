package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.Wrapper
import io.github.alexzhirkevich.keight.findRoot
import kotlin.jvm.JvmInline

internal const val NEXT = "next"
internal const val VALUE = "value"
internal const val DONE = "done"

@JvmInline
internal value class JSIteratorWrapper(
    override val value: Iterator<JsAny?>
) : JsAny, Wrapper<Iterator<JsAny?>>, Iterator<JsAny?> by value, Iterable<JsAny?> {

    override suspend fun proto(runtime: ScriptRuntime): JsAny? {
        return (runtime.findRoot() as JSRuntime).Iterator.get(PROTOTYPE, runtime)
    }

    override fun iterator(): Iterator<JsAny?> = value

    override fun toKotlin(runtime: ScriptRuntime): Any = value
}
