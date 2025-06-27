package io.github.alexzhirkevich.keight.js

import io.github.alexzhirkevich.keight.JSRuntime
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.Wrapper
import io.github.alexzhirkevich.keight.findJsRoot
import io.github.alexzhirkevich.keight.findRoot

private const val LAST_INDEX = "lastIndex"

internal enum class RegexOptions(
    val option: RegexOption? = null,
    val char : Char
) {
    IgnoreCase(RegexOption.IGNORE_CASE, 'i'),
    Multiline(RegexOption.MULTILINE, 'm'),
    Global(char = 'g'),
    HasIndices(char = 'd'),
}

internal class JsRegexWrapper(
    val pattern : String = "(?:)",
    val options: Set<RegexOptions> = emptySet(),
) : JsObjectImpl(), Wrapper<Regex> {

    override val value: Regex = Regex(pattern, options.mapNotNullTo(mutableSetOf()) { it.option })

    val global get() = RegexOptions.Global in options

    val hasIndices get() = RegexOptions.HasIndices in options

    val ignoreCase get() = RegexOptions.IgnoreCase in options

    private val flags = options.sorted().joinToString("") { it.char.toString() }

    init {
        defineOwnProperty("global".js, JsProperty { global.js })
        defineOwnProperty("hasIndices".js, JsProperty { hasIndices.js })
        defineOwnProperty("ignoreCase".js, JsProperty { ignoreCase.js })
        defineOwnProperty("flags".js, JsProperty { flags.js })
        defineOwnProperty(LAST_INDEX.js, 0.js)
    }

    override suspend fun proto(runtime: ScriptRuntime): JsAny? {
        return runtime.findJsRoot().RegExp.get(PROTOTYPE, runtime)
    }

    override fun toKotlin(runtime: ScriptRuntime): Any {
        return value
    }

    override fun toString(): String {
        return "/$pattern/$flags"
    }
}

internal fun String.toRegexOptions() : Set<RegexOptions>{
    return mapNotNullTo(mutableSetOf()) {
        RegexOptions.entries.firstOrNull { e -> e.char == it }
    }
}


internal fun String.toJsRegex(options : Set<RegexOptions>? = null) : JsRegexWrapper {
    return JsRegexWrapper(
        pattern = removePrefix("/")
            .substringBeforeLast("/"),
        options = options ?: substringAfterLast("/").toRegexOptions()
    )
}

internal suspend fun JsRegexWrapper.exec(string: String, runtime: ScriptRuntime) : JsAny? {
    val result = value.findAll(string)

    return if (global) {
        val res = result.mapTo(mutableListOf()) { it.value.js }
        if (res.isEmpty()) null else res.js
    } else {
        val res = result.firstOrNull() ?: return null
        (listOf(res.value.js).js as JsObject).apply {
            val range = res.range
            set("index".js, range.first.js, runtime)
            set("input".js, string.js, runtime)
            if (hasIndices) {
                set("indices".js, listOf(listOf(range.first.js, (range.last+1).js).js).js, runtime)
            }
        }
    }
}

internal suspend fun JsRegexWrapper.test(string: String, runtime: ScriptRuntime) : Boolean {

    val lastIndex = if (global)
        runtime.toNumber(get(LAST_INDEX.js, runtime)).toInt()
    else 0

    val result = value.find(string, lastIndex)

    if (global) {
        set(LAST_INDEX.js, (result?.range?.last?.plus(1) ?: 0).js, runtime)
    }

    return result != null
}