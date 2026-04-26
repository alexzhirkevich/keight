package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.CallFrame
import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime

public open class JSError(
    private val msg : Any?,
    private val name : String = "Error",
    cause : Throwable? = null,
    delegate : JsObjectImpl = JsObjectImpl(
        name = "Error",
        properties = mutableMapOf(
            "message".js to msg.toString().js,
            "name".js to name.js,
        )
    )
) : Exception("Uncaught $name: $msg", cause),
    JsObject by delegate,
    Expression.LocationAttachable
{
    /**
     * File name where the error occurred.
     */
    public var fileName: String? = null

    /**
     * Line number (1-based) where the error occurred.
     */
    public var lineNumber: Int? = null

    /**
     * Column number (1-based) where the error occurred.
     */
    public var columnNumber: Int? = null

    /**
     * Snapshot of the call stack at the time this error was constructed/attached.
     * Set by parseNew or Expression.invoke() when a JSError is caught.
     */
    internal var callStackFrames: List<CallFrame>? = null

    /**
     * Stack trace string, similar to JavaScript's Error.stack.
     * Generated lazily on first access.
     */
    public var stack: String? = null
        get() {
            if (field == null) {
                field = buildStackString()
            }
            return field
        }

    init {
        // Register dynamic properties on the delegate so JS code can access them.
        // Note: Inside Callable lambdas, 'this' is ScriptRuntime, so we use
        // errorRef to access JSError's own properties.
        delegate.defineOwnProperty(
            property = "stack".js,
            value = JsPropertyAccessor.BackedField(
                getter = Callable { stack?.js ?: Undefined },
                setter = Callable { value ->
                    stack = value.firstOrNull()?.toString()
                    Undefined
                }
            )
        )
        delegate.defineOwnProperty(
            property = "fileName".js,
            value = JsPropertyAccessor.BackedField(
                getter = Callable { fileName?.js ?: Undefined },
                setter = Callable { value ->
                    fileName = value.firstOrNull()?.toString()
                    stack = null
                    Undefined
                }
            )
        )
        delegate.defineOwnProperty(
            property = "lineNumber".js,
            value = JsPropertyAccessor.BackedField(
                getter = Callable { lineNumber?.js ?: Undefined },
                setter = Callable { value ->
                    lineNumber = value.firstOrNull()?.let {
                        toNumber(it).toInt()
                    }
                    stack = null
                    Undefined
                }
            )
        )
        delegate.defineOwnProperty(
            property = "columnNumber".js,
            value = JsPropertyAccessor.BackedField(
                getter = Callable { columnNumber?.js ?: Undefined },
                setter = Callable { value ->
                    columnNumber = value.firstOrNull()?.let {
                        toNumber(it).toInt()
                    }
                    stack = null
                    Undefined
                }
            )
        )
    }

    private fun buildStackString(): String {
        val sb = StringBuilder()
        sb.append("$name: $msg")

        if (callStackFrames != null && callStackFrames!!.isNotEmpty()) {
            val frames = callStackFrames!!
            // callStack stores frames with most recent call LAST, but standard JS
            // stack traces print the most recent (deepest) frame FIRST.
            // So we reverse the order.
            val reversed = frames.reversed()

            // Last frame (deepest call) uses JSError's own fileName/lineNumber/columnNumber
            // (which may have been manually modified after construction).
            // Function name and isConstructor come from the captured frame.
            val firstFrame = reversed.first()
            val firstLine = CallFrame(
                functionName = firstFrame.functionName,
                fileName = fileName,
                lineNumber = lineNumber,
                columnNumber = columnNumber,
                isConstructor = firstFrame.isConstructor,
                isNative = firstFrame.isNative
            )

            sb.append("\n")
            sb.append(firstLine.toStackString())
            for (i in 1 until reversed.size) {
                sb.append("\n")
                sb.append(reversed[i].toStackString())
            }
        } else if (lineNumber != null || columnNumber != null || fileName != null) {
            // Fallback: single-frame stack trace from location fields
            if (fileName != null) {
                sb.append("\n    at <anonymous> ($fileName")
                if (lineNumber != null) sb.append(":$lineNumber")
                if (columnNumber != null) sb.append(":$columnNumber")
                sb.append(")")
            } else {
                sb.append("\n    at <anonymous>")
                if (lineNumber != null) sb.append(":$lineNumber")
                if (columnNumber != null) sb.append(":$columnNumber")
            }
        }
        return sb.toString()
    }

    /**
     * Attach source location information to this error.
     */
    public fun attachSourceLocation(
        fileName: String? = null,
        lineNumber: Int? = null,
        columnNumber: Int? = null
    ): JSError {
        if (this.fileName == null && fileName != null) this.fileName = fileName
        if (this.lineNumber == null && lineNumber != null) this.lineNumber = lineNumber
        if (this.columnNumber == null && columnNumber != null) this.columnNumber = columnNumber
        // Reset stack so it gets regenerated with new location info
        this.stack = null
        return this
    }

    override fun attachLocation(line: Int, column: Int, fileName: String?) {
        if (this.fileName == null && fileName != null) this.fileName = fileName
        if (this.lineNumber == null) this.lineNumber = line
        if (this.columnNumber == null) this.columnNumber = column
        this.stack = null
    }

    override fun attachCallStack(frames: List<CallFrame>) {
        if (this.callStackFrames == null) {
            this.callStackFrames = frames
            this.stack = null
        }
    }

    override fun toString(): String = stack ?: if (msg == null) name else "$name: $msg"
}

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
