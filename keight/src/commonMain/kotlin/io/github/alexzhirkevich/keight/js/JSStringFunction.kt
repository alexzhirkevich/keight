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
        "length" eq JsNumberWrapper(0)
        "charAt".func("index") { args ->
            valueAtIndexOrUnit(args[0].let(::toNumber).toInt())
        }
        "charAt".func("index") { args ->
            valueAtIndexOrUnit(args[0].let(::toNumber).toInt())
        }
        "indexOf".func("char") { args ->
            val search = checkNotEmpty(toString(args[0], this)[0])
            thisRef.toString().indexOf(search)
        }
        "last".func("char") { args ->
            val search = checkNotEmpty(toString(args[0], this)[0])
            thisRef.toString().lastIndexOf(search)
        }
        "concat".func(FunctionParam("strings", isVararg = true)) { args ->
            thisRef.toString() + (args[0] as List<*>).joinToString("")
        }
        "charCodeAt" eq CharCodeAt("")
        "endsWith" eq EndsWith("")
        "split" eq Split("")
        "startsWith" eq StartsWith("")
        "includes" eq Includes("")
        "padStart" eq PadStart("")
        "padEnd" eq PadEnd("")
        "match" eq Match("")
        "replace" eq Replace("")
        "replaceAll" eq ReplaceAll("")
        "repeat" eq Repeat("")
        "substring" eq Substring("")
        "substr" eq Substring("")
        "trim" eq Trim("")
        "trimStart" eq TrimStart("")
        "trimEnd" eq TrimEnd("")
        "toUpperCase" eq ToUpperCase("")
        "toLocaleUpperCase" eq ToUpperCase("")
        "toLowerCase" eq ToLowerCase("")
        "toLocaleLowerCase" eq ToLowerCase("")
    },
    properties = mutableMapOf(
        "fromCharCode" to "fromCharCode".func(FunctionParam("codes", isVararg = true)) {
            (it[0] as List<*>).joinToString {
                val c = toNumber(it).toInt()
                if (c in LSEP) "\n" else Char(c).toString()
            }
        }
    ),
    parameters = listOf("str" defaults OpConstant("")),
    body = Expression {
        toString(it.get("str"), it)
    }
) {
    companion object {
        suspend fun toString(value: Any?, runtime: ScriptRuntime) : String {
            val toString = (value as? JsAny)?.get("toString", runtime)?.callableOrNull()

            return if (toString != null) {
                toString.call(value, emptyList(), runtime).toString()
            } else {
                value.toString()
            }
        }
    }

    override suspend fun isInstance(obj: Any?, runtime: ScriptRuntime): Boolean {
        return obj !is JsStringWrapper && super.isInstance(obj, runtime)
    }

    override suspend fun constructObject(args: List<Any?>, runtime: ScriptRuntime): JSObject {
        return JsStringObject(JsStringWrapper(invoke(args, runtime).toString()))
    }

    @JvmInline
    private value class LastIndexOf(val value: String) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Int {
            val search = checkNotEmpty(toString(args[0],runtime)[0])
            return value.lastIndexOf(search)
        }

        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return LastIndexOf(toString(thisArg, runtime))
        }
    }

    @JvmInline
    private value class Concat(val value: String) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): String {
            return value + args.joinToString("")
        }

        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return Concat(toString(thisArg, runtime))
        }
    }

    @JvmInline
    private value class CharCodeAt(val value: String) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Int {
            val ind = args[0].let(runtime::toNumber).toInt()
            return value[ind].code
        }

        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return CharCodeAt(toString(thisArg, runtime))
        }
    }

    @JvmInline
    private value class EndsWith(val value: String) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Boolean {
            val searchString = toString(args[0], runtime)
            val position = args.getOrNull(1)?.let(runtime::toNumber)?.toInt()
            return if (position == null) {
                value.endsWith(searchString)
            } else {
                value.take(position.toInt()).endsWith(searchString)
            }
        }

        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return EndsWith(toString(thisArg, runtime))
        }
    }

    @JvmInline
    private value class Split(val value: String) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): List<String> {
            val delimiters = toString(args[0], runtime)
            return value.split(delimiters).let { it
                if (delimiters.isEmpty()) {
                    it.subList(1, it.size - 1)
                } else it
            }
        }

        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return Split(toString(thisArg, runtime))
        }
    }

    @JvmInline
    private value class StartsWith(val value: String) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Boolean {
            val searchString = toString(args[0], runtime)
            val position = args.getOrNull(1)?.let(runtime::toNumber)?.toInt()

            return if (position == null) {
                value.startsWith(searchString)
            } else {
                value.drop(position).startsWith(searchString)
            }
        }

        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return StartsWith(toString(thisArg, runtime))
        }
    }

    @JvmInline
    private value class Includes(val value: String) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Boolean {
            val searchString = toString(args[0], runtime)
            val position = args.getOrNull(1)?.let(runtime::toNumber)?.toInt()

            return value.indexOf(searchString, startIndex = position ?: 0) >=0
        }

        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return Includes(toString(thisArg, runtime))
        }
    }

    @JvmInline
    private value class PadStart(val value: String) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): String {
            val targetLength = args[0].let(runtime::toNumber).toInt()
            val padString = toString(args.getOrNull(1) ?: " ", runtime)
            val toAppend = targetLength - value.length

            return buildString(targetLength) {
                while (length < toAppend) {
                    append(padString.take(toAppend - length))
                }
                append(value)
            }
        }

        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return PadStart(toString(thisArg, runtime))
        }
    }

    @JvmInline
    private value class PadEnd(val value: String) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): String {
            val targetLength = args[0].let(runtime::toNumber).toInt()
            val padString = toString(args.getOrNull(1) ?: " ", runtime)

            return buildString(targetLength) {
                append(value)
                while (length < targetLength) {
                    append(padString.take(targetLength - length))
                }
            }
        }

        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return PadEnd(toString(thisArg, runtime))
        }
    }

    @JvmInline
    private value class Match(val value: String) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): Boolean {
            val regex = toString(args[0], runtime)
            return value.matches(regex.toRegex())
        }

        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return Match(toString(thisArg, runtime))
        }
    }

    @JvmInline
    private value class Replace(val value: String) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): String {
            return value.replace(false, runtime, args)
        }

        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return Replace(toString(thisArg, runtime))
        }
    }
    @JvmInline
    private value class ReplaceAll(val value: String) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): String {
            return value.replace(true, runtime, args)
        }

        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return ReplaceAll(toString(thisArg, runtime))
        }
    }

    @JvmInline
    private value class Repeat(val value: String) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): String {
            val count = args[0].let(runtime::toNumber).toInt()
            return value.repeat(count)
        }

        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return Repeat(toString(thisArg, runtime))
        }
    }

    @JvmInline
    private value class Substring(val value: String) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): String {
            val start = args[0].let(runtime::toNumber).toInt()
            val end = args.getOrNull(1)
                ?.let(runtime::toNumber)?.toInt()?.coerceAtMost(value.length) ?: value.length
            return value.substring(start, end)
        }

        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return Substring(toString(thisArg, runtime))
        }
    }

    @JvmInline
    private value class Trim(val value: String) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): String {
            return value.trim()
        }

        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return Trim(toString(thisArg, runtime))
        }
    }

    @JvmInline
    private value class TrimStart(val value: String) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): String {
            return value.trimStart()
        }

        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return TrimStart(toString(thisArg, runtime))
        }
    }

    @JvmInline
    private value class TrimEnd(val value: String) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): String {
            return value.trimEnd()
        }

        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return TrimEnd(toString(thisArg, runtime))
        }
    }

    @JvmInline
    private value class ToUpperCase(val value: String) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): String {
            return value.uppercase()
        }

        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
            return ToUpperCase(toString(thisArg, runtime))
        }
    }

    @JvmInline
    private value class ToLowerCase(val value: String) : Callable {
        override suspend fun invoke(args: List<Any?>, runtime: ScriptRuntime): String {
            return value.lowercase()
        }

        override suspend fun bind(thisArg: Any?, args: List<Any?>, runtime: ScriptRuntime): Callable {
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