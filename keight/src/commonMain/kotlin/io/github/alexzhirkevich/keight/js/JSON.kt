package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.Wrapper
import io.github.alexzhirkevich.keight.fastForEach
import io.github.alexzhirkevich.keight.js.interpreter.NumberFormat
import io.github.alexzhirkevich.keight.js.interpreter.escapeForJson
import io.github.alexzhirkevich.keight.js.interpreter.number
import io.github.alexzhirkevich.keight.js.interpreter.string
import io.github.alexzhirkevich.keight.js.interpreter.syntaxCheck

internal fun JSON() = Object("JSON") {
    "parse".js.func(
        FunctionParam("text"),
        "reviver" defaults OpArgOmitted
    ) {
        toString(it[0]).toList().listIterator().parseJSON()
    }

    "stringify".js.func(
        FunctionParam("value"),
        "replacer" defaults OpArgOmitted,
        "space"    defaults OpArgOmitted
    ) { args ->
        val indent = args.argOrNull(2).resolveIndent(this)
        args[0].stringify(this, indent).js
    }
}

private tailrec fun ListIterator<Char>.nextSignificant() : Char {
    return next().takeUnless(Char::isWhitespace) ?: nextSignificant()
}

private fun ListIterator<Char>.eat(seq : String) : Boolean {
    return seq.all(::eat)
}

private fun ListIterator<Char>.eat(char: Char) : Boolean {

    if (nextSignificant() == char){
        return true
    }
    previous()
    return false
}

private fun ListIterator<Char>.parseJSON() : JsAny? {
    syntaxCheck(hasNext()){
        "Invalid JSON (empty value)"
    }
    return when (nextSignificant()) {
        '{' -> parseObject()
        '[' -> parseArray()
        else -> {
            previous()
            parsePrimitive()
        }
    }
}

private fun ListIterator<Char>.parseObject() : JsObject {
    return Object {
        while (!eat('}')) {
            syntaxCheck(eat('"')) { "Invalid JSON - '\"' was expected at ${nextIndex()}" }
            val key = buildString {
                while (!eat('"')) {
                    append(next())
                }
            }
            syntaxCheck(eat(':')) { "Invalid JSON - ':' was expected at ${nextIndex()}" }
            key.js eq parseJSON()
            eat(',')
        }
    }
}


private fun ListIterator<Char>.parseArray() : JsArrayWrapper {
    return JsArrayWrapper(
        buildList {
            while (!eat(']')) {
                add(parseJSON())
                eat(',')
            }
        }.toMutableList()
    )
}

private fun ListIterator<Char>.parsePrimitive() : JsAny? {
    return when {
        eat('"') -> JsStringWrapper(string('"').value)
        eat("null") -> null
        eat("true") -> JSBooleanWrapper(true)
        eat("false") -> JSBooleanWrapper(false)
        eat("undefined") -> Undefined
        else -> {
            val n = nextSignificant()
            syntaxCheck(n.isDigit() || n in listOf('-','+','e','E','.') || NumberFormat.entries.any { it.prefix == n }) {
                "Invalid JSON: number expected but got $n at ${previousIndex()}"
            }
            JsNumberWrapper(number(n).value)
        }
    }
}

// ---------------------------------------------------------------------------
// indent resolver
// ---------------------------------------------------------------------------

/**
 * Converts the raw `space` argument to a concrete indent string:
 * - Number  → up to 10 spaces
 * - String  → first 10 characters
 * - Omitted → "" (compact)
 */
private suspend fun JsAny?.resolveIndent(runtime: ScriptRuntime): String = when {
    this == null || this === Undefined || this === ArgOmitted -> ""
    this is JsNumberWrapper -> " ".repeat(value.toInt().coerceIn(0, 10))
    this is JsStringWrapper -> value.take(10)
    else -> {
        val s = runtime.toString(this)
        s.toIntOrNull()?.let { " ".repeat(it.coerceIn(0, 10)) } ?: s.take(10)
    }
}

// ---------------------------------------------------------------------------
// stringify
// ---------------------------------------------------------------------------

private tailrec suspend fun Any?.stringify(runtime: ScriptRuntime, indent: String = "", depth: Int = 0): String {
    return when (this) {
        null          -> "null"
        Undefined     -> "undefined"
        is List<*>    -> stringify(runtime, indent, depth)
        is JsObject   -> stringify(runtime, indent, depth)
        is Number     -> toString()
        is Boolean    -> toString()
        is Wrapper<*> -> value.stringify(runtime, indent, depth)
        is JsAny      -> "\"${runtime.toString(this).escapeForJson()}\""
        else          -> "\"${toString().escapeForJson()}\""
    }
}

private suspend fun JsObject.stringify(runtime: ScriptRuntime, indent: String, depth: Int): String {
    val keys = keys(runtime)
    if (keys.isEmpty()) return "{}"

    if (indent.isEmpty()) {
        return buildString {
            append('{')
            for (k in keys) {
                append('"'); append(k); append('"'); append(':')
                append(get(k, runtime).stringify(runtime, indent, depth))
                append(',')
            }
        }.removeSuffix(",") + "}"
    }

    val childPad  = indent.repeat(depth + 1)
    val closePad  = indent.repeat(depth)
    return buildString {
        append("{\n")
        for (k in keys) {
            append(childPad)
            append('"'); append(k); append('"'); append(": ")
            append(get(k, runtime).stringify(runtime, indent, depth + 1))
            append(",\n")
        }
    }.removeSuffix(",\n") + "\n${closePad}}"
}

private suspend fun List<*>.stringify(runtime: ScriptRuntime, indent: String, depth: Int): String {
    if (isEmpty()) return "[]"

    if (indent.isEmpty()) {
        return buildString {
            append('[')
            fastForEach { append(it.stringify(runtime, indent, depth)); append(',') }
        }.removeSuffix(",") + "]"
    }

    val childPad = indent.repeat(depth + 1)
    val closePad = indent.repeat(depth)
    return buildString {
        append("[\n")
        fastForEach {
            append(childPad)
            append(it.stringify(runtime, indent, depth + 1))
            append(",\n")
        }
    }.removeSuffix(",\n") + "\n${closePad}]"
}
