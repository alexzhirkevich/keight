package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.interpreter.referenceError

public object Undefined : JsAny {
    override val type: String
        get() = "undefined"

    override fun toString(): String {
        return "undefined"
    }

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {
        runtime.referenceError { "Can't read properties of undefined" }
    }
}