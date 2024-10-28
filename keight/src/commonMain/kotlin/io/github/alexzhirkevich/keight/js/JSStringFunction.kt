package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.argAt
import io.github.alexzhirkevich.keight.argAtOrNull
import io.github.alexzhirkevich.keight.checkNotEmpty
import io.github.alexzhirkevich.keight.expressions.OpConstant
import io.github.alexzhirkevich.keight.fastMap
import io.github.alexzhirkevich.keight.invoke
import io.github.alexzhirkevich.keight.valueAtIndexOrUnit
import kotlin.jvm.JvmInline

internal class JSStringFunction : JSFunction(name = "String") {

    init {
        setPrototype(Object {
            "length" eq JsNumberWrapper(0)
            "charAt" eq CharAt("")
            "at" eq CharAt("")
            "indexOf" eq IndexOf("")
            "lastIndexOf" eq LastIndexOf("")
            "concat" eq Concat("")
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
        })
    }

    override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): Any {
        return if (args.isEmpty()) {
            ""
        } else {
            args[0].invoke(runtime).toString()
        }
    }

    override suspend fun construct(args: List<Expression>, runtime: ScriptRuntime): Any {
        return invoke(args, runtime)
    }

    @JvmInline
    private value class CharAt(val value: String) : Callable {
        override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): Any? {
            val idx = args.argAt(0).invoke(runtime).let(runtime::toNumber).toInt()
            return valueAtIndexOrUnit(idx)
        }

        override suspend fun bind(args: List<Expression>, runtime: ScriptRuntime): Callable {
            return CharAt(args.firstStringOrEmpty(runtime))
        }
    }

    @JvmInline
    private value class IndexOf(val value: String) : Callable {
        override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): Int {
            val search = checkNotEmpty(args.argAt(0).invoke(runtime).toString()[0])
            return value.indexOf(search)
        }

        override suspend fun bind(args: List<Expression>, runtime: ScriptRuntime): Callable {
            return IndexOf(args.firstStringOrEmpty(runtime))
        }
    }

    @JvmInline
    private value class LastIndexOf(val value: String) : Callable {
        override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): Int {
            val search = checkNotEmpty(args.argAt(0).invoke(runtime).toString()[0])
            return value.lastIndexOf(search)
        }

        override suspend fun bind(args: List<Expression>, runtime: ScriptRuntime): Callable {
            return LastIndexOf(args.firstStringOrEmpty(runtime))
        }
    }

    @JvmInline
    private value class Concat(val value: String) : Callable {
        override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): String {
            return value + args.fastMap { it.invoke(runtime) }.joinToString("")
        }

        override suspend fun bind(args: List<Expression>, runtime: ScriptRuntime): Callable {
            return Concat(args.firstStringOrEmpty(runtime))
        }
    }

    @JvmInline
    private value class CharCodeAt(val value: String) : Callable {
        override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): Int {
            val ind = args.argAt(0).invoke(runtime).let(runtime::toNumber).toInt()
            return value[ind].code
        }

        override suspend fun bind(args: List<Expression>, runtime: ScriptRuntime): Callable {
            return CharCodeAt(args.firstStringOrEmpty(runtime))
        }
    }


    @JvmInline
    private value class EndsWith(val value: String) : Callable {
        override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): Boolean {
            val searchString = args.argAt(0).invoke(runtime).toString()
            val position = args.argAtOrNull(1)?.invoke(runtime)?.let(runtime::toNumber)?.toInt()
            return if (position == null) {
                value.endsWith(searchString)
            } else {
                value.take(position.toInt()).endsWith(searchString)
            }
        }

        override suspend fun bind(args: List<Expression>, runtime: ScriptRuntime): Callable {
            return EndsWith(args.firstStringOrEmpty(runtime))
        }
    }

    @JvmInline
    private value class Split(val value: String) : Callable {
        override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): List<String> {
            val delimiters = args.argAt(0).invoke(runtime).toString()
            return value.split(delimiters).let { it
                if (delimiters.isEmpty()) {
                    it.subList(1, it.size - 1)
                } else it
            }
        }

        override suspend fun bind(args: List<Expression>, runtime: ScriptRuntime): Callable {
            return Split(args.firstStringOrEmpty(runtime))
        }
    }

    @JvmInline
    private value class StartsWith(val value: String) : Callable {
        override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): Boolean {
            val searchString = args.argAt(0).invoke(runtime).toString()
            val position = args.argAtOrNull(1)?.invoke(runtime)?.let(runtime::toNumber)?.toInt()
            return if (position == null) {
                value.startsWith(searchString)
            } else {
                value.drop(position.toInt()).startsWith(searchString)
            }
        }

        override suspend fun bind(args: List<Expression>, runtime: ScriptRuntime): Callable {
            return StartsWith(args.firstStringOrEmpty(runtime))
        }
    }

    @JvmInline
    private value class Includes(val value: String) : Callable {
        override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): Boolean {
            val searchString = args.argAt(0).invoke(runtime).toString()
            val position = args.argAtOrNull(1)?.invoke(runtime)?.let(runtime::toNumber)?.toInt()

            return value.indexOf(searchString, startIndex = position ?: 0) >=0
        }

        override suspend fun bind(args: List<Expression>, runtime: ScriptRuntime): Callable {
            return Includes(args.firstStringOrEmpty(runtime))
        }
    }

    @JvmInline
    private value class PadStart(val value: String) : Callable {
        override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): String {
            val targetLength = args.argAt(0).invoke(runtime).let(runtime::toNumber).toInt()
            val padString = args.argAtOrNull(1)?.invoke(runtime)?.toString() ?: " "
            val toAppend = targetLength - value.length

            return buildString(targetLength) {
                while (length < toAppend) {
                    append(padString.take(toAppend - length))
                }
                append(value)
            }
        }

        override suspend fun bind(args: List<Expression>, runtime: ScriptRuntime): Callable {
            return PadStart(args.firstStringOrEmpty(runtime))
        }
    }

    @JvmInline
    private value class PadEnd(val value: String) : Callable {
        override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): String {
            val targetLength = args.argAt(0).invoke(runtime).let(runtime::toNumber).toInt()
            val padString = args.argAtOrNull(1)?.invoke(runtime)?.toString() ?: " "

            return buildString(targetLength) {
                append(value)
                while (length < targetLength) {
                    append(padString.take(targetLength - length))
                }
            }
        }

        override suspend fun bind(args: List<Expression>, runtime: ScriptRuntime): Callable {
            return PadEnd(args.firstStringOrEmpty(runtime))
        }
    }

    @JvmInline
    private value class Match(val value: String) : Callable {
        override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): Boolean {
            val regex = args.argAt(0).invoke(runtime).toString()
            return value.matches(regex.toRegex())
        }

        override suspend fun bind(args: List<Expression>, runtime: ScriptRuntime): Callable {
            return Match(args.firstStringOrEmpty(runtime))
        }
    }

    @JvmInline
    private value class Replace(val value: String) : Callable {
        override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): String {
            return value.replace(false, runtime, args)
        }

        override suspend fun bind(args: List<Expression>, runtime: ScriptRuntime): Callable {
            return Replace(args.firstStringOrEmpty(runtime))
        }
    }
    @JvmInline
    private value class ReplaceAll(val value: String) : Callable {
        override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): String {
            return value.replace(true, runtime, args)
        }

        override suspend fun bind(args: List<Expression>, runtime: ScriptRuntime): Callable {
            return ReplaceAll(args.firstStringOrEmpty(runtime))
        }
    }

    @JvmInline
    private value class Repeat(val value: String) : Callable {
        override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): String {
            val count = args.argAt(0).invoke(runtime).let(runtime::toNumber).toInt()
            return value.repeat(count)
        }

        override suspend fun bind(args: List<Expression>, runtime: ScriptRuntime): Callable {
            return Repeat(args.firstStringOrEmpty(runtime))
        }
    }

    @JvmInline
    private value class Substring(val value: String) : Callable {
        override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): String {
            val start = args.argAt(0).invoke(runtime).let(runtime::toNumber).toInt()
            val end = args.argAtOrNull(1)?.invoke(runtime)
                ?.let(runtime::toNumber)?.toInt()?.coerceAtMost(value.length) ?: value.length
            return value.substring(start, end)
        }

        override suspend fun bind(args: List<Expression>, runtime: ScriptRuntime): Callable {
            return Substring(args.firstStringOrEmpty(runtime))
        }
    }

    @JvmInline
    private value class Trim(val value: String) : Callable {
        override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): String {
            return value.trim()
        }

        override suspend fun bind(args: List<Expression>, runtime: ScriptRuntime): Callable {
            return Trim(args.firstStringOrEmpty(runtime))
        }
    }

    @JvmInline
    private value class TrimStart(val value: String) : Callable {
        override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): String {
            return value.trimStart()
        }

        override suspend fun bind(args: List<Expression>, runtime: ScriptRuntime): Callable {
            return TrimStart(args.firstStringOrEmpty(runtime))
        }
    }

    @JvmInline
    private value class TrimEnd(val value: String) : Callable {
        override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): String {
            return value.trimEnd()
        }

        override suspend fun bind(args: List<Expression>, runtime: ScriptRuntime): Callable {
            return TrimEnd(args.firstStringOrEmpty(runtime))
        }
    }

    @JvmInline
    private value class ToUpperCase(val value: String) : Callable {
        override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): String {
            return value.uppercase()
        }

        override suspend fun bind(args: List<Expression>, runtime: ScriptRuntime): Callable {
            return ToUpperCase(args.firstStringOrEmpty(runtime))
        }
    }

    @JvmInline
    private value class ToLowerCase(val value: String) : Callable {
        override suspend fun invoke(args: List<Expression>, runtime: ScriptRuntime): String {
            return value.lowercase()
        }

        override suspend fun bind(args: List<Expression>, runtime: ScriptRuntime): Callable {
            return ToLowerCase(args.firstStringOrEmpty(runtime))
        }
    }
}

private suspend fun List<Expression>.firstStringOrEmpty(runtime: ScriptRuntime) : String {
    if (isEmpty())
        return ""

    return first().invoke(runtime).toString()
}

private suspend fun String.replace(
    all : Boolean,
    context: ScriptRuntime,
    arguments: List<Expression>
): String {
    val pattern = arguments.argAt(0).invoke(context).toString()
    val replacement = arguments.argAt(1).invoke(context).toString()

    return when {
        pattern.isEmpty() -> replacement + this
        all -> replace(pattern, replacement)
        else -> replaceFirst(pattern, replacement)
    }
}