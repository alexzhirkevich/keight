package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.Wrapper
import io.github.alexzhirkevich.keight.findRoot
import kotlin.jvm.JvmInline

@JvmInline
internal value class JSBooleanWrapper(
    override val value: Boolean
) : JsAny, Wrapper<Boolean> {

    override val type: String
        get() = "boolean"

    override suspend fun proto(runtime: ScriptRuntime): Any? {
        return (runtime.findRoot() as JSRuntime).Boolean.get(PROTOTYPE, runtime)
    }

    override fun toString(): String {
        return value.toString()
    }
}