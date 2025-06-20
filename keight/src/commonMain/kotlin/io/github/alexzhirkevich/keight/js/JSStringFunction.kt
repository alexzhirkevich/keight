package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.callableOrNull
import io.github.alexzhirkevich.keight.checkNotEmpty
import io.github.alexzhirkevich.keight.expressions.OpConstant
import io.github.alexzhirkevich.keight.fastMap
import io.github.alexzhirkevich.keight.get
import io.github.alexzhirkevich.keight.js.interpreter.LSEP
import io.github.alexzhirkevich.keight.thisRef
import io.github.alexzhirkevich.keight.valueAtIndexOrUnit
import kotlin.jvm.JvmInline

internal class JSStringFunction : JSFunction(
    name = "String",
    prototype = Object {
        "length".js eq JsNumberWrapper(0)
        "charAt".js.func("index") {
            valueAtIndexOrUnit(toNumber(it[0]).toInt())
        }
        "charAt".js.func("index") {
            valueAtIndexOrUnit(toNumber(it[0]).toInt())
        }
        "indexOf".js.func("char") {
            toString(thisRef, this)
                .indexOf(checkNotEmpty(toString(it[0], this)[0])).js
        }
        "last".js.func("char") {
            toString(thisRef, this)
                .lastIndexOf(checkNotEmpty(toString(it[0], this)[0])).js
        }
        "concat".js.func("strings".vararg()) {
            (toString(thisRef, this) + (it[0] as Iterable<*>).joinToString("")).js
        }
        "charCodeAt".js.func("index") {
            toString(thisRef, this)[toNumber(it[0]).toInt()].code.js
        }
        "endsWith".js.func(
            FunctionParam("search"),
            "pos" defaults OpArgOmitted
        ) {
            val searchString = toString(it[0], this)
            val position = it.argOrNull(1)?.let { toNumber(it) }?.toInt()
            val value = toString(thisRef, this)
            if (position == null) {
                value.endsWith(searchString)
            } else {
                value.take(position.toInt()).endsWith(searchString)
            }.js
        }
        "split".js.func("delim") {
            val delimiters = toString(it[0], this)
            val str = toString(thisRef, this).split(delimiters)
            if (delimiters.isEmpty()) {
                str.subList(1, str.size - 1)
            } else {
                str
            }.fastMap { it.js }.js
        }
        "startsWith".js.func(
            FunctionParam("search"),
            "pos" defaults OpArgOmitted
        ) {
            val searchString = toString(it[0], this)
            val position = it.argOrNull(1)?.let { toNumber(it) }?.toInt()

            val value = thisRef<CharSequence>()
            if (position == null) {
                value.startsWith(searchString)
            } else {
                value.drop(position).startsWith(searchString)
            }.js
        }
        "includes".js.func(
            FunctionParam("search"),
            "pos" defaults OpConstant(0.js)
        ) {
            val searchString = toString(it[0], this)
            val position = toNumber(it[1]).toInt()

            (thisRef<CharSequence>().indexOf(searchString, startIndex = position) >= 0).js
        }
        "padStart".js.func(
            FunctionParam("length"),
            "padString" defaults OpConstant(" ".js)
        ){
            val targetLength = toNumber(it[0]).toInt()
            val padString = toString(it[1], this)
            val value = thisRef<CharSequence>()
            val toAppend = targetLength - value.length

            buildString(targetLength) {
                while (length < toAppend) {
                    append(padString.take(toAppend - length))
                }
                append(value)
            }.js
        }
        "padEnd".js.func(
            FunctionParam("length"),
            "padString" defaults OpConstant(" ".js)
        ) {
            val targetLength = toNumber(it[0]).toInt()
            val padString = toString(it[1], this)
            val value = thisRef<CharSequence>()

            buildString(targetLength) {
                append(value)
                while (length < targetLength) {
                    append(padString.take(targetLength - length))
                }
            }.js
        }
        "replace".js.func("pattern", "replacement"){
            thisRef<CharSequence>().replace(false, this, it).js
        }
        "replaceAll".js.func("pattern", "replacement"){
            thisRef<CharSequence>().replace(true, this, it).js
        }
        "repeat".js.func("count") {
            thisRef<CharSequence>().repeat(toNumber(it[0]).toInt()).js
        }
        "substring".js.func(
            FunctionParam("start"),
            "end" defaults OpArgOmitted
        ) {
            thisRef<CharSequence>().substring(this, it).js
        }
        "substr".js.func(
            FunctionParam("start"),
            "end" defaults OpArgOmitted
        ) {
            thisRef<CharSequence>().substring(this, it).js
        }
        "trim".js.func {
            thisRef<CharSequence>().trim().js
        }
        "trimStart".js.func {
            thisRef<CharSequence>().trimStart().js
        }
        "trimEnd".js.func {
            thisRef<CharSequence>().trimEnd().js
        }
        "toUpperCase".js.func {
            thisRef<CharSequence>().toString().uppercase().js
        }
        "toLocaleUpperCase".js.func {
            thisRef<CharSequence>().toString().uppercase().js
        }
        "toLowerCase".js.func {
            thisRef<CharSequence>().toString().lowercase().js
        }
        "toLocaleLowerCase".js.func {
            thisRef<CharSequence>().toString().lowercase().js
        }
        //"match" TODO("regex")

    },
    properties = mutableMapOf(
        "fromCharCode".js to "fromCharCode".func(FunctionParam("codes", isVararg = true)) {
            (it[0] as Iterable<JsAny?>).map {
                val c = toNumber(it).toInt()
                if (c in LSEP) "\n" else Char(c).toString()
            }.joinToString("").js
        }
    ),
    parameters = listOf("__str" defaults OpConstant("".js)),
    body = Expression {
        toString(it.get("__str".js), it).js
    }
) {
    companion object {
        suspend fun toString(value: Any?, runtime: ScriptRuntime) : String {
            return (value as? JsAny)?.get("toString".js, runtime)
                ?.callableOrNull()
                ?.call(value, emptyList(), runtime)?.toString()
                ?: value.toString()
        }
    }

    override suspend fun isInstance(obj: JsAny?, runtime: ScriptRuntime): Boolean {
        return obj !is JsStringWrapper && super.isInstance(obj, runtime)
    }

    override suspend fun constructObject(args: List<JsAny?>, runtime: ScriptRuntime): JSObject {
        return JsStringObject(JsStringWrapper(invoke(args, runtime).toString()))
    }
}

private suspend fun CharSequence.replace(
    all : Boolean,
    runtime: ScriptRuntime,
    arguments: List<JsAny?>
): String {
    val pattern = JSStringFunction.toString(arguments[0], runtime)
    val replacement = JSStringFunction.toString(arguments[1], runtime)

    return when {
        pattern.isEmpty() -> replacement + this
        all -> toString().replace(pattern, replacement)
        else -> toString().replaceFirst(pattern, replacement)
    }
}

private suspend fun CharSequence.substring(
    runtime: ScriptRuntime,
    arguments: List<JsAny?>
): String {
    val start = runtime.toNumber(arguments[0]).toInt()
    val end = arguments.argOrNull(1)
        ?.let { runtime.toNumber(it) }?.toInt()
        ?.coerceAtMost(length)
        ?: length
    return substring(start, end)
}