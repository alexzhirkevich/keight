package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.Wrapper
import io.github.alexzhirkevich.keight.findJsRoot
import io.github.alexzhirkevich.keight.findRoot
import kotlinx.coroutines.Job
import kotlin.jvm.JvmInline

@JvmInline
internal value class JsPromiseWrapper(
    override val value: Job
) : JsAny, Wrapper<Job>, Job by value {

    override suspend fun proto(runtime: ScriptRuntime): JsAny? {
        return runtime.findJsRoot().Promise.get(PROTOTYPE, runtime)
    }

    override fun toString(): String {
        return "Promise"
    }

    override fun toKotlin(runtime: ScriptRuntime): Any = value
}