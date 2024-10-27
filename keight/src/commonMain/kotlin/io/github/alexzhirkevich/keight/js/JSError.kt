package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime

public open class JSError(message : String?, cause : Throwable?)
    : Exception(message, cause), JsAny {

    override suspend fun get(property: Any?, runtime: ScriptRuntime): Any? {
        return when(property){
            "message" -> message
            "stack" -> stackTraceToString()
            "name" -> this::class.simpleName
            else -> super.get(property, runtime)
        }
    }
}

public class SyntaxError(message : String? = null, cause : Throwable? = null) : JSError(message, cause)

public class TypeError(message : String? = null, cause : Throwable? = null) : JSError(message, cause)

public class RangeError(message : String? = null, cause : Throwable? = null) : JSError(message, cause)

public class ReferenceError(message : String? = null, cause : Throwable? = null) : JSError(message, cause)

internal fun unresolvedReference(ref : String, obj : String? = null) : Nothing =
    if (obj != null)
        throw ReferenceError("Unresolved reference '$ref' for $obj")
    else throw ReferenceError("$ref is not defined")