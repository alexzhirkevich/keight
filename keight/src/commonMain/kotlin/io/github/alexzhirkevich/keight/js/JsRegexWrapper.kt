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
    return RegexOptions.entries.filterTo(mutableSetOf()) { entry ->
        any { it == entry.char }
    }
}

/**
 * Converts JS Unicode escape sequences to actual Unicode characters.
 * Handles \u{XXXX} syntax used in JavaScript regex patterns with 'u' flag.
 */
private fun String.convertJsUnicodeEscapes(): String {
    // Pattern for \u{XXXX} where X is 1-6 hex digits (JS supports 0-10FFFF)
    val unicodeEscapeRegex = Regex("""\\u\{([0-9a-fA-F]+)}""")
    return unicodeEscapeRegex.replace(this) { matchResult ->
        val codePoint = matchResult.groupValues[1].toInt(16)
        // Convert code point to string using Kotlin's native method
        // For code points > 0xFFFF (surrogate pairs), use intArrayOf
        if (codePoint <= 0xFFFF) {
            codePoint.toChar().toString()
        } else {
            // Calculate surrogate pair for code points > 0xFFFF
            val adjusted = codePoint - 0x10000
            val highSurrogate = ((adjusted ushr 10) + 0xD800).toChar()
            val lowSurrogate = ((adjusted and 0x3FF) + 0xDC00).toChar()
            charArrayOf(highSurrogate, lowSurrogate).concatToString()
        }
    }
}


internal fun String.toJsRegex(options : Set<RegexOptions>? = null) : JsRegexWrapper {
    val rawPattern = removePrefix("/")
        .substringBeforeLast("/")
    val rawFlags = substringAfterLast("/")
    val flags = options ?: rawFlags.toRegexOptions()
    
    // Check if 'u' flag is present - only then convert \u{...} escapes
    val hasUnicodeFlag = RegexOptions.IgnoreCase in flags  // Use a placeholder check, we'll add Unicode later
        || rawFlags.contains('u')
        || (options?.any { it.char == 'u' } == true)
    
    val convertedPattern = if (hasUnicodeFlag) {
        rawPattern.convertJsUnicodeEscapes()
    } else {
        rawPattern
    }
    
    return JsRegexWrapper(
        pattern = convertedPattern,
        options = flags
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