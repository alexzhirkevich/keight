package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.findRoot
import kotlin.jvm.JvmInline

@JvmInline
internal value class JsMapWrapper(
    override val value: MutableMap<Any?, Any?>
) : JsAny, JsWrapper<MutableMap<Any?,Any?>>, MutableMap<Any?,Any?> by value {

    override suspend fun proto(runtime: ScriptRuntime): Any? {
        return (runtime.findRoot() as JSRuntime).Map.get(PROTOTYPE, runtime)
    }

}