package io.github.alexzhirkevich.keight.js.interpreter

import kotlin.jvm.JvmInline

internal enum class NumberFormat(
    val radix : Int,
    val alphabet : Set<Char>,
    val prefix : Char?
) {
    Dec(10, "_eE.0123456789".toHashSet(), null),
    Hex(16, "_0123456789abcdef".toHashSet(), 'x'),
    Oct(8, "_01234567".toHashSet(), 'o'),
    Bin(2, "_01".toHashSet(), 'b')
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
        object OptionalChaining : Operator
        object QuestionMark : Operator
        object NullishCoalescing : Operator
        object In : Operator
        object Instanceof : Operator
        object Typeof : Operator
        object Delete : Operator
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
            object NullCoalescingAssign : Assign
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

    sealed interface Identifier : Token {
        val identifier: String

        @JvmInline
        value class Property(override val identifier: String) : Identifier

        enum class Keyword : Identifier {
            Var,
            Let,
            Const,
            Null,
            True,
            False,
            If,
            Else,
            For,
            While,
            Do,
            Break,
            Continue,
            Function,
            Return,
            Class,
            Switch,
            Case,
            Default,
            Throw,
            Try,
            Catch,
            Finally,
            Async,
            Await,
            Extends;

            override val identifier: String = name.lowercase()
        }
    }
}
