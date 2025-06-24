package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.Wrapper
import io.github.alexzhirkevich.keight.fastForEach
import io.github.alexzhirkevich.keight.js.interpreter.escape
import io.github.alexzhirkevich.keight.js.interpreter.number
import io.github.alexzhirkevich.keight.js.interpreter.string
import io.github.alexzhirkevich.keight.js.interpreter.syntaxCheck

internal fun JSON() = Object("JSON") {
    "parse".js.func("string") {
        toString(it[0]).toList().listIterator().parseJSON()
    }

    "stringify".js.func("object") {
        it[0].stringify(this).js
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
        else -> {
            val n = nextSignificant()
            syntaxCheck(n.isDigit()) {
                "Invalid JSON: number expected but got $n at ${previousIndex()}"
            }
            return JsNumberWrapper(number(n).value)
        }
    }
}

private tailrec suspend fun Any?.stringify(runtime: ScriptRuntime) : String {
    return when (this) {
        is List<*> -> stringify(runtime)
        is JsObject -> stringify(runtime)
        is Number -> toString()
        is Wrapper<*> -> value.stringify(runtime)
        is JsAny -> "\"${runtime.toString(this).escape()}\""
        else -> "\"${toString().escape()}\""
    }
}

private suspend fun JsObject.stringify(runtime: ScriptRuntime) : String {
    return buildString {
        append('{')
        for (k in keys(runtime)) {
            append('"')
            append(k)
            append('"')
            append(':')
            append(get(k, runtime).stringify(runtime))
            append(',')
        }
    }.removeSuffix(",") + "}"
}

private suspend fun List<*>.stringify(runtime: ScriptRuntime) : String {
    return buildString {
        append('[')
        fastForEach {
            append(it.stringify(runtime))
            append(',')
        }
    }.removeSuffix(",") + "]"
}
