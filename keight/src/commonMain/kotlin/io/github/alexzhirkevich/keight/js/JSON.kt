package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.fastForEach
import io.github.alexzhirkevich.keight.js.interpreter.escape
import io.github.alexzhirkevich.keight.js.interpreter.number
import io.github.alexzhirkevich.keight.js.interpreter.string
import io.github.alexzhirkevich.keight.js.interpreter.syntaxCheck

internal fun JSON() = Object("JSON") {
    "parse".func("string") {
        JSStringFunction.toString(it[0], this)
            .toList().listIterator().parseJSON()
    }

    "stringify".func("object") {
        it[0].stringify(this)
    }
}

private fun ListIterator<Char>.nextSignificant() : Char {
    var n = next()
    while (n == '\n'){
        n =  next()
    }
    return n
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

private fun ListIterator<Char>.parseJSON() : Any? {
    syntaxCheck(hasNext()){
        "Invalid JSON"
    }
    return when (nextSignificant()) {
        '{' -> parseObject()
        '[' -> parseArray()
        else -> parsePrimitive()
    }
}

private fun ListIterator<Char>.parseObject() : JSObject {
    return Object {
        while (!eat('}')) {
            syntaxCheck(eat('"')) { "Invalid JSON" }
            val key = buildString {
                while (!eat('"')) {
                    append(next())
                }
            }
            syntaxCheck(eat(':')) { "Invalid JSON" }
            key eq parseJSON()
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
            val n = next()
            syntaxCheck(n.isDigit()) {
                "Invalid JSON"
            }
            return JsNumberWrapper(number(n).value)
        }
    }
}

private tailrec suspend fun Any?.stringify(runtime: ScriptRuntime) : String {
    return when (this) {
        is JSObject -> stringify(runtime)
        is List<*> -> stringify(runtime)
        is Number -> toString()
        is JsWrapper<*> -> value.stringify(runtime)
        else -> "\"${JSStringFunction.toString(this, runtime).escape()}\""
    }
}

private suspend fun JSObject.stringify(runtime: ScriptRuntime) : String {
    return buildString {
        append('{')
        for (k in keys) {
            append('"')
            append(k)
            append('"')
            append(':')
            append(runtime.fromKotlin(get(k, runtime)).stringify(runtime))
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



