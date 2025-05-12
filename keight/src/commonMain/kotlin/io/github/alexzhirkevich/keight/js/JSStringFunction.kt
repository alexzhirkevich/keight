package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.callableOrNull
import io.github.alexzhirkevich.keight.checkNotEmpty
import io.github.alexzhirkevich.keight.expressions.OpConstant
import io.github.alexzhirkevich.keight.fastMap
import io.github.alexzhirkevich.keight.js.interpreter.LSEP
import io.github.alexzhirkevich.keight.valueAtIndexOrUnit
import kotlin.jvm.JvmInline

internal class JSStringFunction : JSFunction(
    name = "String",
    prototype = Object {
        "length".js() eq JsNumberWrapper(0)
        "charAt".js().func("index") {
            valueAtIndexOrUnit(toNumber(it[0]).toInt())
        }
        "charAt".js().func("index") {
            valueAtIndexOrUnit(toNumber(it[0]).toInt())
        }
        "indexOf".js().func("char") {
            val search = checkNotEmpty(toString(it[0], this)[0])
            toString(thisRef, this).indexOf(search).js()
        }
        "last".js().func("char") {
            val search = checkNotEmpty(toString(it[0], this)[0])
            toString(thisRef, this).lastIndexOf(search).js()
        }
        "concat".js().func(FunctionParam("strings", isVararg = true)) {
            (toString(thisRef, this) + (it[0] as List<*>).joinToString("")).js()
        }
        "charCodeAt".js().func("index") {
            val ind = toNumber(it[0]).toInt()
            toString(thisRef, this)[ind].code.js()
        }
        "endsWith".js().func(FunctionParam("search"), "pos" defaults OpArgOmitted) {
            val searchString = toString(it[0], this)
            val position = it.argOrNull(1)?.let { toNumber(it) }?.toInt()
            val value = thisRef.toString()
            if (position == null) {
                value.endsWith(searchString)
            } else {
                value.take(position.toInt()).endsWith(searchString)
            }.js()
        }
        "split".js().func("delim") {
            val delimiters = toString(it[0], this)
            val str = toString(thisRef, this).split(delimiters)
            if (delimiters.isEmpty()) {
                str.subList(1, str.size - 1).fastMap { it.js() }.js()
            } else it.js()
        }
        "startsWith".js() eq StartsWith("")
        "includes".js() eq Includes("")
        "padStart".js() eq PadStart("")
        "padEnd".js() eq PadEnd("")
        "match".js() eq Match("")
        "replace".js() eq Replace("")
        "replaceAll".js() eq ReplaceAll("")
        "repeat".js() eq Repeat("")
        "substring".js() eq Substring("")
        "substr".js() eq Substring("")
        "trim".js() eq Trim("")
        "trimStart".js() eq TrimStart("")
        "trimEnd".js() eq TrimEnd("")
        "toUpperCase".js() eq ToUpperCase("")
        "toLocaleUpperCase".js() eq ToUpperCase("")
        "toLowerCase".js() eq ToLowerCase("")
        "toLocaleLowerCase".js() eq ToLowerCase("")
    },
    properties = mutableMapOf(
        "fromCharCode".js() to "fromCharCode".func(FunctionParam("codes", isVararg = true)) {
            (it[0] as List<JsAny?>).fastMap {
                val c = toNumber(it).toInt()
                if (c in LSEP) "\n" else Char(c).toString()
            }.joinToString("").js()

        }
    ),
    parameters = listOf("str" defaults OpConstant("".js())),
    body = Expression {
        toString(it.get("str".js()), it).js()
    }
) {
    companion object {
        suspend fun toString(value: Any?, runtime: ScriptRuntime) : String {
            return (value as? JsAny)?.get("toString".js(), runtime)
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

    @JvmInline
    private value class StartsWith(val value: String) : Callable {
        override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
            val searchString = toString(args[0], runtime)
            val position = args.getOrNull(1)?.let { runtime.toNumber(it) }?.toInt()

            return if (position == null) {
                value.startsWith(searchString)
            } else {
                value.drop(position).startsWith(searchString)
            }.js()
        }

        override suspend fun bind(thisArg: JsAny?, args: List<JsAny?>, runtime: ScriptRuntime): Callable {
            return StartsWith(toString(thisArg, runtime))
        }
    }

    @JvmInline
    private value class Includes(val value: String) : Callable {
        override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
            val searchString = toString(args[0], runtime)
            val position = args.getOrNull(1)?.let { runtime.toNumber(it) }?.toInt()

            return (value.indexOf(searchString, startIndex = position ?: 0) >=0).js()
        }

        override suspend fun bind(thisArg: JsAny?, args: List<JsAny?>, runtime: ScriptRuntime): Callable {
            return Includes(toString(thisArg, runtime))
        }
    }

    @JvmInline
    private value class PadStart(val value: String) : Callable {
        override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
            val targetLength = runtime.toNumber(args[0]).toInt()
            val padString = toString(args.getOrNull(1) ?: " ", runtime)
            val toAppend = targetLength - value.length

            return buildString(targetLength) {
                while (length < toAppend) {
                    append(padString.take(toAppend - length))
                }
                append(value)
            }.js()
        }

        override suspend fun bind(thisArg: JsAny?, args: List<JsAny?>, runtime: ScriptRuntime): Callable {
            return PadStart(toString(thisArg, runtime))
        }
    }

    @JvmInline
    private value class PadEnd(val value: String) : Callable {
        override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
            val targetLength = runtime.toNumber(args[0]).toInt()
            val padString = toString(args.getOrNull(1) ?: " ", runtime)

            return buildString(targetLength) {
                append(value)
                while (length < targetLength) {
                    append(padString.take(targetLength - length))
                }
            }.js()
        }

        override suspend fun bind(thisArg: JsAny?, args: List<JsAny?>, runtime: ScriptRuntime): Callable {
            return PadEnd(toString(thisArg, runtime))
        }
    }

    @JvmInline
    private value class Match(val value: String) : Callable {
        override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
            val regex = toString(args[0], runtime)
            return value.matches(regex.toRegex()).js()
        }

        override suspend fun bind(thisArg: JsAny?, args: List<JsAny?>, runtime: ScriptRuntime): Callable {
            return Match(toString(thisArg, runtime))
        }
    }

    @JvmInline
    private value class Replace(val value: String) : Callable {
        override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
            return value.replace(false, runtime, args).js()
        }

        override suspend fun bind(thisArg: JsAny?, args: List<JsAny?>, runtime: ScriptRuntime): Callable {
            return Replace(toString(thisArg, runtime))
        }
    }
    @JvmInline
    private value class ReplaceAll(val value: String) : Callable {
        override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
            return value.replace(true, runtime, args).js()
        }

        override suspend fun bind(thisArg: JsAny?, args: List<JsAny?>, runtime: ScriptRuntime): Callable {
            return ReplaceAll(toString(thisArg, runtime))
        }
    }

    @JvmInline
    private value class Repeat(val value: String) : Callable {
        override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
            val count = runtime.toNumber(args[0]).toInt()
            return value.repeat(count).js()
        }

        override suspend fun bind(thisArg: JsAny?, args: List<JsAny?>, runtime: ScriptRuntime): Callable {
            return Repeat(toString(thisArg, runtime))
        }
    }

    @JvmInline
    private value class Substring(val value: String) : Callable {
        override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
            val start = runtime.toNumber(args[0]).toInt()
            val end = args.getOrNull(1)
                ?.let { runtime.toNumber(it) }?.toInt()?.coerceAtMost(value.length) ?: value.length
            return value.substring(start, end).js()
        }

        override suspend fun bind(thisArg: JsAny?, args: List<JsAny?>, runtime: ScriptRuntime): Callable {
            return Substring(toString(thisArg, runtime))
        }
    }

    @JvmInline
    private value class Trim(val value: String) : Callable {
        override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
            return value.trim().js()
        }

        override suspend fun bind(thisArg: JsAny?, args: List<JsAny?>, runtime: ScriptRuntime): Callable {
            return Trim(toString(thisArg, runtime))
        }
    }

    @JvmInline
    private value class TrimStart(val value: String) : Callable {
        override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
            return value.trimStart().js()
        }

        override suspend fun bind(thisArg: JsAny?, args: List<JsAny?>, runtime: ScriptRuntime): Callable {
            return TrimStart(toString(thisArg, runtime))
        }
    }

    @JvmInline
    private value class TrimEnd(val value: String) : Callable {
        override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
            return value.trimEnd().js()
        }

        override suspend fun bind(thisArg: JsAny?, args: List<JsAny?>, runtime: ScriptRuntime): Callable {
            return TrimEnd(toString(thisArg, runtime))
        }
    }

    @JvmInline
    private value class ToUpperCase(val value: String) : Callable {
        override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
            return value.uppercase().js()
        }

        override suspend fun bind(thisArg: JsAny?, args: List<JsAny?>, runtime: ScriptRuntime): Callable {
            return ToUpperCase(toString(thisArg, runtime))
        }
    }

    @JvmInline
    private value class ToLowerCase(val value: String) : Callable {
        override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
            return value.lowercase().js()
        }

        override suspend fun bind(thisArg: JsAny?, args: List<JsAny?>, runtime: ScriptRuntime): Callable {
            return ToLowerCase(toString(thisArg, runtime))
        }
    }
}

private suspend fun String.replace(
    all : Boolean,
    runtime: ScriptRuntime,
    arguments: List<Any?>
): String {
    val pattern = JSStringFunction.toString(arguments[0], runtime)
    val replacement = JSStringFunction.toString(arguments[1], runtime)

    return when {
        pattern.isEmpty() -> replacement + this
        all -> replace(pattern, replacement)
        else -> replaceFirst(pattern, replacement)
    }
}