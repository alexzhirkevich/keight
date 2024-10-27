package io.github.alexzhirkevich.keight.expressions

import io.github.alexzhirkevich.keight.Expression
import kotlin.jvm.JvmInline

@JvmInline
internal value class OpSpread(val value : Expression) : Expression by value