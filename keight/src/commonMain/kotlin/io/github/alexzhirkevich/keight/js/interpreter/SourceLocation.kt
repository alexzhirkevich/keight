package io.github.alexzhirkevich.keight.js.interpreter

import io.github.alexzhirkevich.keight.SourceLocation

/**
 * Wraps a [Token] with its [SourceLocation] in the source code.
 */
internal data class LocatedToken(
    val token: Token,
    val location: SourceLocation
)
