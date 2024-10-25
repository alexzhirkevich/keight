package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.argAt
import io.github.alexzhirkevich.keight.argAtOrNull
import io.github.alexzhirkevich.keight.common.Callable
import io.github.alexzhirkevich.keight.common.checkNotEmpty
import io.github.alexzhirkevich.keight.common.valueAtIndexOrUnit
import io.github.alexzhirkevich.keight.es.ESAny
import io.github.alexzhirkevich.keight.es.checkArgs
import io.github.alexzhirkevich.keight.es.checkArgsNotNull
import io.github.alexzhirkevich.keight.invoke
import kotlin.jvm.JvmInline

@JvmInline
internal value class JsString(
    override val value : String
) : ESAny, JsWrapper<String>, Comparable<JsString>, CharSequence by value {

    override val type: String
        get() = "string"


    override fun toString(): String {
        return value
    }

    override fun get(variable: Any?): Any? {
        return when(variable){
            "length" -> value.length.toLong()
            "charAt", "at" -> CharAt(value)
            "indexOf" -> IndexOf(value)
            "lastIndexOf" -> LastIndexOf(value)
            "concat" -> Concat(value)
            "charCodeAt" -> CharCodeAt(value)
            "endsWith" -> EndsWith(value)
            "split" -> Split(value)
            "startsWith" -> StartsWith(value)
            "includes" -> Includes(value)
            "padStart" -> PadStart(value)
            "padEnd" -> PadEnd(value)
            "match" -> Match(value)
            "replace" -> Replace(value)
            "replaceAll" -> ReplaceAll(value)
            "repeat" -> Repeat(value)
            "substring","substr" -> Substring(value)
            "trim" -> Trim(value)
            "trimStart" -> TrimStart(value)
            "trimEnd" -> TrimEnd(value)
            "toUpperCase", "toLocaleUpperCase" -> ToUpperCase(value)
            "toLowerCase", "toLocaleLowerCase" -> ToLowerCase(value)
            else -> super.get(variable)
        }
    }

    override fun compareTo(other: JsString): Int {
        return value.compareTo(other.value)
    }

    @JvmInline
    private value class CharAt(val value: String) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime): Any? {
            val idx = args.argAt(0).invoke(runtime).let(runtime::toNumber).toInt()
            return valueAtIndexOrUnit(idx)
        }
    }

    @JvmInline
    private value class IndexOf(val value: String) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime): Int {
            val search = checkNotEmpty(args.argAt(0).invoke(runtime).toString()[0])
            return value.indexOf(search)
        }
    }

    @JvmInline
    private value class LastIndexOf(val value: String) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime): Int {
            val search = checkNotEmpty(args.argAt(0).invoke(runtime).toString()[0])
            return value.lastIndexOf(search)
        }
    }

    @JvmInline
    private value class Concat(val value: String) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime): String {
            return value + args.joinToString("") { it.invoke(runtime).toString() }
        }
    }

    @JvmInline
    private value class CharCodeAt(val value: String) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime): Int {
            val ind = args.argAt(0).invoke(runtime).let(runtime::toNumber).toInt()
            return value[ind].code
        }
    }


    @JvmInline
    private value class EndsWith(val value: String) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime): Boolean {
            val searchString = args.argAt(0).invoke(runtime).toString()
            val position = args.argAtOrNull(1)?.invoke(runtime)?.let(runtime::toNumber)?.toInt()
            return if (position == null) {
                value.endsWith(searchString)
            } else {
                value.take(position.toInt()).endsWith(searchString)
            }
        }
    }

    @JvmInline
    private value class Split(val value: String) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime): List<String> {
            val delimiters = args.argAt(0).invoke(runtime).toString()
            return value.split(delimiters).let { it
                if (delimiters.isEmpty()) {
                    it.subList(1, it.size - 1)
                } else it
            }
        }
    }

    @JvmInline
    private value class StartsWith(val value: String) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime): Boolean {
            val searchString = args.argAt(0).invoke(runtime).toString()
            val position = args.argAtOrNull(1)?.invoke(runtime)?.let(runtime::toNumber)?.toInt()
            return if (position == null) {
                value.startsWith(searchString)
            } else {
                value.drop(position.toInt()).startsWith(searchString)
            }
        }
    }

    @JvmInline
    private value class Includes(val value: String) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime): Boolean {
            val searchString = args.argAt(0).invoke(runtime).toString()
            val position = args.argAtOrNull(1)?.invoke(runtime)?.let(runtime::toNumber)?.toInt()

            return value.indexOf(searchString, startIndex = position ?: 0) >=0
        }
    }

    @JvmInline
    private value class PadStart(val value: String) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime): String {
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
    }

    @JvmInline
    private value class PadEnd(val value: String) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime): String {
            val targetLength = args.argAt(0).invoke(runtime).let(runtime::toNumber).toInt()
            val padString = args.argAtOrNull(1)?.invoke(runtime)?.toString() ?: " "

            return buildString(targetLength) {
                append(value)
                while (length < targetLength) {
                    append(padString.take(targetLength - length))
                }
            }
        }
    }

    @JvmInline
    private value class Match(val value: String) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime): Boolean {
            val regex = args.argAt(0).invoke(runtime).toString()
            return value.matches(regex.toRegex())
        }
    }

    @JvmInline
    private value class Replace(val value: String) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime): String {
            return value.replace(false, runtime, args)
        }
    }
    @JvmInline
    private value class ReplaceAll(val value: String) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime): String {
            return value.replace(true, runtime, args)
        }
    }

    @JvmInline
    private value class Repeat(val value: String) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime): String {
            val count = args.argAt(0).invoke(runtime).let(runtime::toNumber).toInt()
            return value.repeat(count)
        }
    }

    @JvmInline
    private value class Substring(val value: String) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime): String {
            val start = args.argAt(0).invoke(runtime).let(runtime::toNumber).toInt()
            val end = args.argAtOrNull(1)?.invoke(runtime)
                ?.let(runtime::toNumber)?.toInt()?.coerceAtMost(value.length) ?: value.length
            return value.substring(start, end)
        }
    }

    @JvmInline
    private value class Trim(val value: String) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime): String {
            return value.trim()
        }
    }

    @JvmInline
    private value class TrimStart(val value: String) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime): String {
            return value.trimStart()
        }
    }

    @JvmInline
    private value class TrimEnd(val value: String) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime): String {
            return value.trimEnd()
        }
    }

    @JvmInline
    private value class ToUpperCase(val value: String) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime): String {
            return value.uppercase()
        }
    }

    @JvmInline
    private value class ToLowerCase(val value: String) : Callable {
        override fun invoke(args: List<Expression>, runtime: ScriptRuntime): String {
            return value.lowercase()
        }
    }

}

private fun String.replace(
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


