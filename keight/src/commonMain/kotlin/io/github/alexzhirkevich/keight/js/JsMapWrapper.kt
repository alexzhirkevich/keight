package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.findRoot
import kotlin.jvm.JvmInline

@JvmInline
internal value class JsMapWrapper(
    override val value: MutableMap<Any?, Any?>
) : JsAny, JsWrapper<MutableMap<Any?,Any?>>, Iterable<List<*>> {

    override val keys: List<String>
        get() = value.keys.map { it.toString() }

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