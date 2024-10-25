package io.github.alexzhirkevich.keight.es.interpreter

import io.github.alexzhirkevich.keight.es.SyntaxError

internal fun ListIterator<Char>.tokens(
    ignoreWhitespaces : Boolean = false
) : List<Token> {
    return buildList {
        while (hasNext()) {
            val token = when (val c = next()) {
                '=' -> assign() // +eq, arrow
                '+' -> plus()
                '-' -> minus()
                '*' -> mul() // +exp
                '/' -> div() // +comment
                '%' -> mod()
                '&' -> and()
                '|' -> or()
                '^' -> xor()
                '<' -> less() // +shl
                '>' -> greater() // +shr, ushr
                '!' -> not() // + neq
                '~' -> Token.Operator.Bitwise.Reverse
                '{' -> Token.Operator.Bracket.CurlyOpen
                '}' -> Token.Operator.Bracket.CurlyClose
                '(' -> Token.Operator.Bracket.RoundOpen
                ')' -> Token.Operator.Bracket.RoundClose
                '[' -> Token.Operator.Bracket.SquareOpen
                ']' -> Token.Operator.Bracket.SquareClose
                ':' -> Token.Operator.Colon
                ';' -> Token.Operator.SemiColon
                ',' -> Token.Operator.Comma
                '.' -> Token.Operator.Period
                '?' -> Token.Operator.QuestionMark
                '\n' -> Token.NewLine
                in STRING_START -> string(c)
                in NUMBERS -> number(c)
                in JAVASCRIPT_WHITESPACE -> if (ignoreWhitespaces) {
                    continue
                } else {
                    Token.Whitespace(c)
                }

                in IDENTIFIER_ALPHABET -> identifier(c) // + keywords
                else -> continue
            }
            add(token)
        }
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

private fun ListIterator<Char>.comment(isSingleLine : Boolean) : Token.Comment {
    val value = StringBuilder()
    if (isSingleLine) {
        while (hasNext()) {
            val n = next()
            if (n == '\n')
                break
            value.append(n)
        }
    } else {
        var a = next()
        var b = next()

        while (a.toString() + b != "*/") {
            value.append(a)
            a = b
            b = next()
        }
    }

    return Token.Comment(value.toString())
}

private fun ListIterator<Char>.string(start : Char) : Token.Str {
    val value = StringBuilder()
    val isTemplate = start == '`'

    do {
        val next = next()
        value.append(next)
    } while (hasNext() && next != start && (isTemplate || next != '\n'))

    return Token.Str(value.deleteAt(value.lastIndex).toString())
}

private fun ListIterator<Char>.number(start : Char) : Token.Num {
    val value = StringBuilder()
    var numberFormat = NumberFormat.Dec
    var isFloat = false
    var ch = start
    do {
        when (ch) {
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
                if (numberFormat == NumberFormat.Hex) {
                    continue
                }
                syntaxCheck(numberFormat == NumberFormat.Dec && !isFloat) {
                    "Invalid number"
                }
                numberFormat = NumberFormat.Bin
            }
        }
        value.append(ch)
        ch = next().lowercaseChar()
    } while (ch in numberFormat.alphabet || ch in NumberFormatIndicators)

    previous()

    val number: Number = try {
        if (value.endsWith('.')) {
            previous()
            value.deleteAt(0)
            isFloat = false
        }
        if (isFloat) {
            value.toString().toDouble()
        } else {
            value.toString().trimEnd('.')
                .let { n -> numberFormat.prefix?.let(n::substringAfter) ?: n }
                .toULong(numberFormat.radix)
                .toLong()
        }
    } catch (t: NumberFormatException) {
        throw SyntaxError("Unexpected token '$value'")
    }

    return Token.Num(number, numberFormat, isFloat)
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

    return when (val string = value.toString()) {
        "var" -> Token.Keyword.Var
        "let" -> Token.Keyword.Let
        "const" -> Token.Keyword.Const
        "null" -> Token.Keyword.Null
        "true" -> Token.Keyword.True
        "false" -> Token.Keyword.False
        "if" -> Token.Keyword.If
        "else" -> Token.Keyword.Else
        "for" -> Token.Keyword.For
        "while" -> Token.Keyword.While
        "do" -> Token.Keyword.Do
        "break" -> Token.Keyword.Break
        "continue" -> Token.Keyword.Continue
        "function" -> Token.Keyword.Function
        "return" -> Token.Keyword.Return
        "class" -> Token.Keyword.Class
        "switch" -> Token.Keyword.Switch
        "case" -> Token.Keyword.Case
        "default" -> Token.Keyword.Default
        "throw" -> Token.Keyword.Throw
        "try" -> Token.Keyword.Try
        "catch" -> Token.Keyword.Catch
        "finally" -> Token.Keyword.Finally

        "new" -> Token.Operator.New
        "in" -> Token.Operator.In
        "instanceof" -> Token.Operator.Instanceof
        "typeof" -> Token.Operator.Typeof

        else -> Token.Identifier(string)
    }
}

private val STRING_START = hashSetOf('"', '\'', '`')
private val NUMBERS = hashSetOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
private val NumberFormatIndicators = NumberFormat.entries.mapNotNull { it.prefix }

private val JAVASCRIPT_WHITESPACE = hashSetOf(
    ' ',
    '\u2029',  // paragraph separator
    '\u00a0',  // Latin-1 space
    '\u1680',  // Ogham space mark
    '\u180e',  // separator, Mongolian vowel
    '\u2000',  // en quad
    '\u2001',  // em quad
    '\u2002',  // en space
    '\u2003',  // em space
    '\u2004',  // three-per-em space
    '\u2005',  // four-per-em space
    '\u2006',  // six-per-em space
    '\u2007',  // figure space
    '\u2008',  // punctuation space
    '\u2009',  // thin space
    '\u200a',  // hair space
    '\u202f',  // narrow no-break space
    '\u205f',  // medium mathematical space
    '\u3000',  // ideographic space
    '\ufeff'   // byte order mark
)

private val IDENTIFIER_ALPHABET = (('a'..'z').toList() + ('A'..'Z').toList() + '$' + '_' ).toHashSet()
private val PROPERTY_ALPHABET_WITH_NUM = (IDENTIFIER_ALPHABET + NUMBERS).toHashSet()
