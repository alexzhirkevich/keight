package io.github.alexzhirkevich.keight.js

internal object Constants {
    internal const val get = "get"
    internal const val set = "set"
    internal const val value = "value"
    internal const val writable = "writable"
    internal const val enumerable = "enumerable"
    internal const val configurable = "configurable"
    internal const val next = "next"
    internal const val done = "done"
    internal const val default = "default"
    internal const val string = "string"
    internal const val number = "number"

    internal const val toString = "toString"
    internal const val valueOf = "valueOf"

    /**
     * Internal property key prefixes used for storing class getters and setters
     * before they are processed by [OpClassInit].
     */
    internal const val getterPrefix = "__getter__"
    internal const val setterPrefix = "__setter__"

    /**
     * JavaScript Number.MAX_SAFE_INTEGER = 2^53 - 1
     */
    internal const val MAX_SAFE_INTEGER = 9007199254740991.0
}
