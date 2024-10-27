package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.findRoot
import kotlin.jvm.JvmInline

@JvmInline
internal value class JsSetWrapper(
    override val value: MutableSet<Any?>
) : JsAny, JsWrapper<MutableSet<Any?>>, MutableSet<Any?> by value {

    override suspend fun proto(runtime: ScriptRuntime): Any? {
        return (runtime.findRoot() as JSRuntime).Set.get(PROTOTYPE, runtime)
    }

}