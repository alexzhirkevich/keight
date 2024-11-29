package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.Wrapper
import io.github.alexzhirkevich.keight.findRoot
import kotlin.jvm.JvmInline

@JvmInline
internal value class JsMapWrapper(
    override val value: MutableMap<Any?, Any?>
) : JsAny, Wrapper<MutableMap<Any?, Any?>>, Iterable<List<*>> {

    override suspend fun keys(
        runtime: ScriptRuntime,
        excludeSymbols: Boolean,
        excludeNonEnumerables: Boolean
    ): List<Any?> {
        return value.keys.toList()
    }

    override suspend fun proto(runtime: ScriptRuntime): Any? {
        return (runtime.findRoot() as JSRuntime).Map.get(PROTOTYPE, runtime)
    }

    override fun iterator(): Iterator<List<*>> {
        return value.entries.asSequence().map { listOf(it.key, it.value) }.iterator()
    }

    override fun toString(): String {
        return value.toString()
    }
}