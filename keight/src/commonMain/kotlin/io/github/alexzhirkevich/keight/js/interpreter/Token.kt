package io.github.alexzhirkevich.keight.js.interpreter

import kotlin.jvm.JvmInline

internal enum class NumberFormat(
    val radix : Int,
    val alphabet : Set<Char>,
    val prefix : Char?
) {
    Dec(10, "eE.0123456789".toHashSet(), null),
    Hex(16, "0123456789abcdef".toHashSet(), 'x'),
    Oct(8, "01234567".toHashSet(), 'o'),
    Bin(2, "01".toHashSet(), 'b')
}

internal sealed interface TemplateStringToken {
    @JvmInline
    value class Str(val value: String) : TemplateStringToken

    @JvmInline
    value class Template(val value : List<Token>) : TemplateStringToken
}

internal sealed interface Token {

    @JvmInline
    value class Str(val value : String) : Token

    @JvmInline
    value class TemplateString(val tokens : List<TemplateStringToken>) : Token

    class Num(
        val value : Number,
        val format : NumberFormat,
        val isFloat : Boolean
    ) : Token

    @JvmInline
    value class Identifier(val name : String) : Token

    @JvmInline
    value class Comment(val value : String) : Token

    @JvmInline
    value class Whitespace(val value : Char) : Token

    object NewLine : Token

    sealed interface Operator : Token {

        object Comma : Operator
        object Period : Operator
        object Colon  : Operator
        object SemiColon : Operator
        object Arrow : Operator
        object QuestionMark : Operator
        object In : Operator
        object Instanceof : Operator
        object Typeof : Operator
        object New : Operator
        object Spread : Operator

        sealed interface Bracket : Token {
            object RoundOpen : Operator
            object RoundClose : Operator
            object SquareOpen : Operator
            object SquareClose : Operator
            object CurlyOpen : Operator
            object CurlyClose : Operator
        }

        sealed interface Assign : Operator {
            object Assignment : Assign
            object PlusAssign : Assign
            object MinusAssign : Assign
            object MulAssign : Assign
            object DivAssign : Assign
            object ModAssign : Assign
            object ExpAssign : Assign
            object ShlAssign : Assign
            object ShrAssign : Assign
            object UshrAssign : Assign
            object BitAndAssign : Assign
            object BitXorAssign : Assign
            object BitOrAssign : Assign
            object LogicAndAssign : Assign
            object LogicOrAssign : Assign
        }

        sealed interface Compare : Operator {
            object Eq : Compare
            object Neq : Compare
            object StrictEq : Compare
            object StrictNeq : Compare
            object Greater : Compare
            object GreaterOrEq : Compare
            object Less : Compare
            object LessOrEq : Compare
        }

        sealed interface Arithmetic : Operator {
            object Minus : Operator
            object Plus : Operator
            object Mul : Operator
            object Div : Operator
            object Mod : Operator
            object Inc : Operator
            object Dec : Operator
            object Exp : Operator
        }

        sealed interface Bitwise : Operator {
            object And : Operator
            object Or : Operator
            object Xor : Operator
            object Shl : Operator
            object Shr : Operator
            object Ushr : Operator
            object Reverse : Operator
        }

        sealed interface Logical : Operator {
            object And : Operator
            object Or : Operator
            object Not : Operator
        }
    }

    sealed interface Keyword : Token {
        object Var : Keyword
        object Let : Keyword
        object Const : Keyword
        object Null : Keyword
        object True : Keyword
        object False : Keyword
        object If : Keyword
        object Else : Keyword
        object For : Keyword
        object While : Keyword
        object Do : Keyword
        object Break : Keyword
        object Continue : Keyword
        object Function : Keyword
        object Return : Keyword
        object Class : Keyword
        object Switch : Keyword
        object Case : Keyword
        object Default : Keyword
        object Throw : Keyword
        object Try : Keyword
        object Catch : Keyword
        object Finally : Keyword
    }
}
