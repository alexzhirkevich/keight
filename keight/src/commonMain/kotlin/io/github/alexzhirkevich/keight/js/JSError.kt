package io.github.alexzhirkevich.keight.js

public open class JSError(
    msg : Any?,
    name : String = "Error",
    cause : Throwable? = null
) : Exception("Uncaught $name: $msg", cause),
    JSObject by JSObjectImpl(
        name = name,
        properties = mapOf(
            "message" to msg.toString(),
            "name" to name
        )
    )

public class JSKotlinError(t : Throwable) : JSError(
    msg = t.message,
    name = t::class.simpleName ?: t::class.toString(),
    cause = t.cause
)

public class SyntaxError(msg : Any?, cause : Throwable? = null)
    : JSError(msg, "SyntaxError", cause) {}

public class TypeError(msg : Any?, cause : Throwable? = null)
    : JSError(msg, "TypeError", cause)

public class RangeError(msg : Any?, cause : Throwable? = null)
    : JSError(msg, "RangeError", cause)

public class ReferenceError(msg : Any?, cause : Throwable? = null)
    : JSError(msg, "ReferenceError", cause)
