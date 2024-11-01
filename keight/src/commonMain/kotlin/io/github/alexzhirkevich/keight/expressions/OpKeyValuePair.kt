package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime

internal class OpKeyValuePair(
    val key : String,
    val value : Expression
) : Expression by value