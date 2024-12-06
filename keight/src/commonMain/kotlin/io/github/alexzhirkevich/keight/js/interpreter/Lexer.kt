package io.github.alexzhirkevich.keight.js.interpreter

import io.github.alexzhirkevich.keight.js.SyntaxError


internal fun String.tokenize(
    ignoreWhitespaces: Boolean =  true
) : List<Token> = toList()
    .listIterator()
    .tokenize(
        untilEndOfBlock = false,
        ignoreWhitespaces = ignoreWhitespaces
    )

private fun ListIterator<Char>.tokenize(
    untilEndOfBlock : Boolean,
    ignoreWhitespaces : Boolean = false
) : List<Token> {
    return buildList {
        try {
            var blockStack = 0

            while (hasNext()) {
                this += when (val c = next()) {
                    '=' -> assign() // +eq, arrow
                    '+' -> plus()
                    '-' -> minus()
                    '*' -> mul() // +exp
                    '/' -> div().also { // +comment
                        if (it is Token.Comment && (it.isSingleLine || it.value.any { it.code in LSEP })) {
                            add(Token.NewLine)
                        }
                    }

                    '#' -> hashbangComment()
                    '%' -> mod()
                    '&' -> and()
                    '|' -> or()
                    '^' -> xor()
                    '<' -> less() // +shl
                    '>' -> greater() // +shr, ushr
                    '!' -> not() // + neq
                    '.' -> period() // + spread
                    '?' -> questionMark() // for ternary + nullish coalescing, optional chaining
                    '~' -> Token.Operator.Bitwise.Reverse
                    '{' -> Token.Operator.Bracket.CurlyOpen.also {
                        blockStack++
                    }

                    '}' -> Token.Operator.Bracket.CurlyClose.also {
                        if (--blockStack < 0 && untilEndOfBlock) {
                            return@buildList
                        }
                    }

                    '(' -> Token.Operator.Bracket.RoundOpen
                    ')' -> Token.Operator.Bracket.RoundClose
                    '[' -> Token.Operator.Bracket.SquareOpen
                    ']' -> Token.Operator.Bracket.SquareClose
                    ':' -> Token.Operator.Colon
                    ';' -> Token.Operator.SemiColon
                    ',' -> Token.Operator.Comma
                    '\n', ' ' -> Token.NewLine
                    '`' -> templateString(ignoreWhitespaces)
                    in STRING_START -> string(c)
                    in NUMBERS -> number(c)
                    else -> {
                        val isWhiteSpace = c.isWhitespace()
                        when {
                            isWhiteSpace && ignoreWhitespaces -> continue
                            isWhiteSpace -> Token.Whitespace(c)
                            else -> identifier(c)
                        }
                    }
                }
            }
        } catch (_: NoSuchElementException) {
            throw SyntaxError("Invalid or unexpected token")
        }
    }
}

private fun ListIterator<Char>.questionMark() : Token {
    if (!hasNext()){
        return Token.Operator.QuestionMark
    }

    return when(next()){
        '?' -> nullishCoalescing()
        '.' -> Token.Operator.OptionalChaining
        else -> Token.Operator.QuestionMark.also { previous() }
    }
}

private fun ListIterator<Char>.nullishCoalescing() : Token {
    if (!hasNext()) {
        return Token.Operator.NullishCoalescing
    }

    return when (next()) {
        '=' -> Token.Operator.Assign.NullCoalescingAssign
        else -> Token.Operator.NullishCoalescing.also { previous() }
    }
}


private fun ListIterator<Char>.period() : Token {
    if (!hasNext()){
        return Token.Operator.Period
    }

    return when(next()){
        '.' -> doublePeriod()
        else -> Token.Operator.Period.also { previous() }
    }
}

private fun ListIterator<Char>.doublePeriod() : Token {
    if (!hasNext()){
        return Token.Operator.DoublePeriod
    }

    return when(next()){
        '.' -> Token.Operator.Spread
        else -> Token.Operator.DoublePeriod.also { previous() }
    }
}

private fun ListIterator<Char>.assign() : Token {
    if (!hasNext()){
        return Token.Operator.Assign.Assignment
    }

    return when(next()){
        '=' -> eq()
        '>' -> Token.Operator.Arrow
        else -> Token.Operator.Assign.Assignment.also { previous() }
    }
}

private fun ListIterator<Char>.eq() : Token {
    if (!hasNext()) {
        return Token.Operator.Compare.Eq
    }

    return when (next()) {
        '=' -> Token.Operator.Compare.StrictEq
        else -> Token.Operator.Compare.Eq.also { previous() }
    }
}

private fun ListIterator<Char>.plus() : Token.Operator {
    if (!hasNext()){
        return Token.Operator.Arithmetic.Plus
    }

    return when(next()){
        '+' -> Token.Operator.Arithmetic.Inc
        '=' -> Token.Operator.Assign.PlusAssign
        else -> Token.Operator.Arithmetic.Plus.also { previous() }
    }
}

private fun ListIterator<Char>.minus() : Token.Operator {
    if (!hasNext()){
        return Token.Operator.Arithmetic.Minus
    }

    return when(next()){
        '-' -> Token.Operator.Arithmetic.Dec
        '=' -> Token.Operator.Assign.MinusAssign
        else -> Token.Operator.Arithmetic.Minus.also { previous() }
    }
}

private fun ListIterator<Char>.mul() : Token.Operator {
    if (!hasNext()){
        return Token.Operator.Arithmetic.Mul
    }

    return when(next()){
        '=' -> Token.Operator.Assign.MulAssign
        '*' -> exp()
        else -> Token.Operator.Arithmetic.Mul.also { previous() }
    }
}

private fun ListIterator<Char>.exp() : Token.Operator {
    if (!hasNext()){
        return Token.Operator.Arithmetic.Exp
    }

    return when(next()){
        '=' -> Token.Operator.Assign.ExpAssign
        else -> Token.Operator.Arithmetic.Exp.also { previous() }
    }
}

private fun ListIterator<Char>.div() : Token {
    if (!hasNext()) {
        return Token.Operator.Arithmetic.Div
    }

    return when (next()) {
        '=' -> Token.Operator.Assign.DivAssign
        '/' -> comment(isSingleLine = true)
        '*' -> comment(isSingleLine = false)
        else -> Token.Operator.Arithmetic.Div.also { previous() }
    }
}

private fun ListIterator<Char>.mod() : Token {
    if (!hasNext()) {
        return Token.Operator.Arithmetic.Mod
    }

    return when (next()) {
        '=' -> Token.Operator.Assign.ModAssign
        else -> Token.Operator.Arithmetic.Mod.also { previous() }
    }
}

private fun ListIterator<Char>.and() : Token {
    if (!hasNext()){
        return Token.Operator.Bitwise.And
    }

    return when(next()){
        '&' -> andLogical()
        '=' -> Token.Operator.Assign.BitAndAssign
        else -> Token.Operator.Bitwise.And.also { previous() }
    }
}

private fun ListIterator<Char>.andLogical() : Token {
    if (!hasNext()){
        return Token.Operator.Logical.And
    }

    return when(next()){
        '=' -> Token.Operator.Assign.LogicAndAssign
        else -> Token.Operator.Logical.And.also { previous() }
    }
}

private fun ListIterator<Char>.or() : Token {
    if (!hasNext()){
        return Token.Operator.Bitwise.Or
    }

    return when(next()){
        '|' -> orLogical()
        '=' -> Token.Operator.Assign.BitOrAssign
        else -> Token.Operator.Bitwise.Or.also { previous() }
    }
}

private fun ListIterator<Char>.orLogical() : Token {
    if (!hasNext()) {
        return Token.Operator.Logical.Or
    }

    return when (next()) {
        '=' -> Token.Operator.Assign.LogicOrAssign
        else -> Token.Operator.Logical.Or.also { previous() }
    }
}

private fun ListIterator<Char>.xor() : Token {
    if (!hasNext()){
        return Token.Operator.Bitwise.Xor
    }

    return when(next()){
        '=' -> Token.Operator.Assign.BitXorAssign
        else -> Token.Operator.Bitwise.Xor.also { previous() }
    }
}

private fun ListIterator<Char>.less() : Token {
    if (!hasNext()){
        return Token.Operator.Compare.Less
    }

    return when(next()){
        '<' -> shl()
        '=' -> Token.Operator.Compare.LessOrEq
        else -> Token.Operator.Compare.Less.also { previous() }
    }
}

private fun ListIterator<Char>.greater() : Token {
    if (!hasNext()){
        return Token.Operator.Compare.Greater
    }

    return when(next()){
        '>' -> shr()
        '=' -> Token.Operator.Compare.GreaterOrEq
        else -> Token.Operator.Compare.Greater.also { previous() }
    }
}

private fun ListIterator<Char>.shl() : Token {
    if (!hasNext()){
        return Token.Operator.Bitwise.Shl
    }

    return when(next()){
        '=' -> Token.Operator.Assign.ShlAssign
        else -> Token.Operator.Bitwise.Shl.also { previous() }
    }
}

private fun ListIterator<Char>.shr() : Token {
    if (!hasNext()) {
        return Token.Operator.Bitwise.Shr
    }

    return when (next()) {
        '=' -> Token.Operator.Assign.ShrAssign
        '>' -> ushr()
        else -> Token.Operator.Bitwise.Shr.also { previous() }
    }
}

private fun ListIterator<Char>.ushr() : Token {
    if (!hasNext()){
        return Token.Operator.Bitwise.Ushr
    }

    return when(next()){
        '=' -> Token.Operator.Assign.UshrAssign
        else -> Token.Operator.Bitwise.Ushr.also { previous() }
    }
}

private fun ListIterator<Char>.not() : Token {
    if (!hasNext()){
        return Token.Operator.Logical.Not
    }

    return when(next()){
        '=' -> neq()
        else -> Token.Operator.Logical.Not.also { previous() }
    }
}

private fun ListIterator<Char>.neq() : Token {
    if (!hasNext()){
        return Token.Operator.Compare.Neq
    }

    return when(next()){
        '=' -> Token.Operator.Compare.StrictNeq
        else -> Token.Operator.Compare.Neq.also { previous() }
    }
}
private fun ListIterator<Char>.hashbangComment() : Token.Comment {
    syntaxCheck(hasNext() && next() == '!'){
        "Unexpected #"
    }

    return comment(isSingleLine = true)
}
private fun ListIterator<Char>.comment(isSingleLine : Boolean) : Token.Comment {
    val value = buildString {
        if (isSingleLine) {
            while (hasNext()) {
                val n = next()
                if (n.code in LSEP)
                    break
                append(n)
            }
        } else {
            var a = next()
            var b = next()

            while (a != '*' || b != '/') {
                append(a)
                a = b
                b = next()
            }
        }
    }

    return Token.Comment(value, isSingleLine)
}

internal fun ListIterator<Char>.string(start : Char) : Token.Str {
    val str = try {
        buildString {
            var isEscaping = false

            var n = next()

            while (isEscaping || n != start) {
                append(n)
                isEscaping = if (n == '\\') !isEscaping else false
                n = next()
            }
        }
    } catch (t: NoSuchElementException) {
        throw SyntaxError("Unexpected string")
    }

    return Token.Str(str.unescape())
}

private val UNICODE_REGEX = "\\\\u[0-9a-fA-F]{4}".toRegex()

private fun String.unescape() : String {
    return replace("\\'", "'")
        .replace("\\\"", "\"")
        .replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t")
        .replace("\\b", "\b")
        .replace("\\\\", "\"")
        .replace(UNICODE_REGEX) {
            when (val unicode = it.value.drop(2).toInt(16)) {
                in LSEP -> '\n'
                else -> Char(unicode)
            }.toString()
        }
}

internal fun String.escape() : String {
    return replace("'", "\\'")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
        .replace("\b", "\\b")
        .replace("\"", "\\\\")
}

internal fun ListIterator<Char>.number(start : Char) : Token.Num {
    val value = StringBuilder()
    var numberFormat = NumberFormat.Dec
    var isFloat = false
    var ch = start
    var wasE = false
    do {
        when (ch) {
            'e','E' -> {
                if (wasE)
                    break
                wasE = true
                if (numberFormat == NumberFormat.Dec) {
                    isFloat = true
                }
            }
            '.' -> {
                if (isFloat) {
                    break
                }
                isFloat = true
            }

            NumberFormat.Hex.prefix -> {
                syntaxCheck(numberFormat == NumberFormat.Dec && !isFloat) {
                    "Invalid number"
                }
                numberFormat = NumberFormat.Hex
            }

            NumberFormat.Oct.prefix -> {
                syntaxCheck(numberFormat == NumberFormat.Dec && !isFloat) {
                    "Invalid number"
                }
                numberFormat = NumberFormat.Oct
            }

            NumberFormat.Bin.prefix -> {
                if (numberFormat != NumberFormat.Hex) {
                    syntaxCheck(numberFormat == NumberFormat.Dec && !isFloat) {
                        "Invalid number"
                    }
                    numberFormat = NumberFormat.Bin
                }
            }
        }
        value.append(ch)
        ch = next().lowercaseChar()
    } while (
        ch in numberFormat.alphabet ||
        ch in NumberFormatIndicators ||
        ((ch == '-'  || ch == '+') && value.lastOrNull() == 'e')
    )

    previous()

    val number: Number = try {
        if (value.endsWith('.')) {
            previous()
            isFloat = false
        }
        if (isFloat) {
            value.toString().replace("_","").toDouble()
        } else {
            value.toString().trimEnd('.')
                .replace("_","")
                .let { n -> numberFormat.prefix?.let(n::substringAfter) ?: n }
                .toULong(numberFormat.radix)
                .toLong()
        }
    } catch (t: NumberFormatException) {
        throw SyntaxError("Unexpected token '$value'", t)
    }

    return Token.Num(number, numberFormat, isFloat)
}

private val keywords by lazy {
    Token.Identifier.Keyword.entries.associateBy { it.identifier }
}

private val reserved by lazy {
    Token.Identifier.Reserved.entries.associateBy { it.identifier }
}

private fun ListIterator<Char>.identifier(start : Char) : Token {
    val value = StringBuilder(start.toString())

    while (hasNext()) {
        val next = next()

        if (next !in PROPERTY_ALPHABET_WITH_NUM) {
            previous()
            break
        }
        value.append(next)
    }

    return when (val string = value.toString().unescape()){
        "new" -> Token.Operator.New
        "in" -> Token.Operator.In
        "instanceof" -> Token.Operator.Instanceof
        "typeof" -> Token.Operator.Typeof
        "void" -> Token.Operator.Void
        "delete" -> Token.Operator.Delete
        in keywords -> keywords[string]!!
        in reserved -> reserved[string]!!
        else ->Token.Identifier.Property(string)
    }
}

private fun ListIterator<Char>.templateString(
    ignoreWhitespaces: Boolean
) : Token.TemplateString {

    val tokens = buildList {

        val str = StringBuilder()

        var isEscaping = false

        fun addStr(){
            if (str.isNotEmpty()) {
                add(TemplateStringToken.Str(str.toString().unescape()))
                str.clear()
            }
        }

        while (true) {
            val next = next()

            if (next == '`' && !isEscaping) {
                addStr()
                break
            }

            if (next == '$' && !isEscaping) {
                val nNext = next()
                if (nNext == '{'){
                    addStr()
                    add(
                        TemplateStringToken.Template(
                            buildList {
                                add(Token.Operator.Bracket.CurlyOpen)
                                addAll(
                                    tokenize(
                                        untilEndOfBlock = true,
                                        ignoreWhitespaces = ignoreWhitespaces
                                    )
                                )
                                add(Token.Operator.Bracket.CurlyClose)
                            },
                        )
                    )
                } else {
                    str.append(next)
                    str.append(nNext)
                }
            } else {
                str.append(next)
            }

            isEscaping = if (next == '\\') !isEscaping else false
        }
    }

    return Token.TemplateString(tokens)
}

private val STRING_START = hashSetOf('"', '\'')
internal val LSEP = hashSetOf(13, 10, 8232, 8233)
private val NUMBERS = hashSetOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
private val NumberFormatIndicators = NumberFormat.entries.mapNotNull { it.prefix }

private val IDENTIFIER_ALPHABET = (('a'..'z').toList() + ('A'..'Z').toList() + '$' + '_' ).toHashSet()
private val PROPERTY_ALPHABET_WITH_NUM = (IDENTIFIER_ALPHABET + NUMBERS).toHashSet()
