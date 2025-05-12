package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.Wrapper
import io.github.alexzhirkevich.keight.findRoot
import kotlinx.coroutines.Job
import kotlin.jvm.JvmInline

@JvmInline
internal value class JSPromiseWrapper(
    override val value: Job
) : JsAny, Wrapper<Job>, Job by value {

    override suspend fun proto(runtime: ScriptRuntime): JsAny? {
        return (runtime.findRoot() as JSRuntime).Promise.get(PROTOTYPE, runtime)
    }

    override fun toString(): String {
        return "Promise"
    }
}