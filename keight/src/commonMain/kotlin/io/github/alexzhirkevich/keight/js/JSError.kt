package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime

public open class JSError(
    msg : Any?,
    name : String = "Error",
    cause : Throwable? = null
) : Exception("Uncaught $name: $msg", cause),
    JSObject by JSObjectImpl(properties = mapOf("message" to msg, "name" to name)) {
    override suspend fun proto(runtime: ScriptRuntime): Any? {
        return (runtime as JSRuntime).Error.get(PROTOTYPE, runtime)
    }
}

public class SyntaxError(message : String? = null, cause : Throwable? = null)
    : JSError(message, "SyntaxError", cause)

public class TypeError(message : String? = null, cause : Throwable? = null)
    : JSError(message, "TypeError", cause)

public class RangeError(message : String? = null, cause : Throwable? = null)
    : JSError(message, "RangeError", cause)

public class ReferenceError(message : String? = null, cause : Throwable? = null)
    : JSError(message, "ReferenceError", cause)
