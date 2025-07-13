package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime

public object Undefined : JsAny {
    override val type: String
        get() = "undefined"

    override fun toString(): String {
        return "undefined"
    }

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {
        runtime.referenceError(runtime.fromKotlin("Can't read properties of undefined"))
    }
}