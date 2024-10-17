package io.github.alexzhirkevich.keight.ecmascript

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.common.Function

internal interface ESFunction : ESClass {
    override val type: String
        get() = "function"
}

internal abstract class ESFunctionBase(
    override val name : String
) : ESObjectBase(name), ESFunction {

    override val constructorClass: Expression? get() = null

    override val extends: Expression? get() = null

    override val construct: Function?
        get() = null

    override val type: String
        get() = "function"

    override fun toString(): String {
        return "function $name() { [native code] }"
    }
}