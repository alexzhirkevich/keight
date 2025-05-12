package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.Wrapper
import io.github.alexzhirkevich.keight.findRoot
import kotlin.jvm.JvmInline

@JvmInline
internal value class JsSetWrapper(
    override val value: MutableSet<JsAny?>
) : JsAny, Wrapper<MutableSet<JsAny?>>, MutableSet<JsAny?> by value {

    override suspend fun proto(runtime: ScriptRuntime): JsAny? {
        return (runtime.findRoot() as JSRuntime).Set.get(PROTOTYPE, runtime)
    }

}