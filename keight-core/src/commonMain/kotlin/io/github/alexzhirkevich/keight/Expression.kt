package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.Undefined
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Part of script that represents single operation
 * */
public abstract class Expression {

    /**
     * Optional source location for error reporting.
     * Set by the parser when source location information is available.
     */
    public var sourceLocation: SourceLocation? = null

    protected abstract suspend fun execute(runtime: ScriptRuntime): JsAny?

    public suspend operator fun invoke(runtime: ScriptRuntime): JsAny? {
        currentCoroutineContext().ensureActive()
        return try {
            when (val res = execute(runtime)){
                is Getter<*> -> res.get(runtime)
                else -> res
            }
        } catch (e: Throwable) {
            // Try to attach source location to the error if it's a JS error
            // with our location attachment interface
            attachErrorInfo(e, runtime)
            throw e
        }
    }

    /**
     * Attach source location and call stack to an error if it supports it.
     */
    private fun attachErrorInfo(error: Throwable, runtime: ScriptRuntime) {
        val loc = sourceLocation
        val hasLocation = loc != null
        val hasCallStack = runtime.callStack.isNotEmpty()
        if (!hasLocation && !hasCallStack) return
        try {
            val attachable = error as? LocationAttachable ?: return
            if (hasLocation) {
                attachable.attachLocation(loc!!.line, loc!!.column, loc!!.fileName)
            }
            if (hasCallStack) {
                attachable.attachCallStack(runtime.captureCallStack())
            }
        } catch (_: ClassCastException) {
            // Not a JS error, ignore
        }
    }

    /**
     * Interface for errors that can receive source location.
     * JSError implements this in the keight module.
     */
    public interface LocationAttachable {
        public fun attachLocation(line: Int, column: Int, fileName: String? = null)
        public fun attachCallStack(frames: List<CallFrame>) {}
    }
}

/**
 * Represents a source location in JavaScript code.
 * Cross-platform compatible (no JVM-only APIs).
 */
public data class SourceLocation(
    public val line: Int,
    public val column: Int,
    public val fileName: String? = null
) {
    override fun toString(): String {
        val base = "$line:$column"
        return if (fileName != null) "$fileName:$base" else base
    }
}

/**
 * Represents a single frame in the JavaScript call stack.
 * Captured when a function is invoked and used for Error.stack generation.
 */
public data class CallFrame(
    public val functionName: String?,
    public val fileName: String?,
    public val lineNumber: Int?,
    public val columnNumber: Int?,
    public val isConstructor: Boolean = false,
    public val isNative: Boolean = false,
) {
    public fun toStackString(): String {
        if (isNative) {
            val name = functionName ?: "unknown"
            return "    at $name (<native>)"
        }
        val name = when {
            functionName.isNullOrEmpty() -> "<anonymous>"
            isConstructor -> "new $functionName"
            else -> functionName
        }
        return if (fileName != null || lineNumber != null) {
            val location = buildString {
                append(" (")
                if (fileName != null) append(fileName)
                if (lineNumber != null) append(":$lineNumber")
                if (columnNumber != null) append(":$columnNumber")
                append(")")
            }
            "    at $name$location"
        } else {
            "    at $name"
        }
    }
}

public fun Expression(execute : suspend (ScriptRuntime) -> JsAny?) : Expression {
    return object : Expression() {
        override suspend fun execute(runtime: ScriptRuntime): JsAny? {
            return execute.invoke(runtime)
        }
    }
}

