package io.github.alexzhirkevich.keight.js.interpreter

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.Constructor
import io.github.alexzhirkevich.keight.Delegate
import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.Named
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.expressions.OpAssign
import io.github.alexzhirkevich.keight.expressions.OpAssignByIndex
import io.github.alexzhirkevich.keight.expressions.OpBlock
import io.github.alexzhirkevich.keight.expressions.OpBreak
import io.github.alexzhirkevich.keight.expressions.OpCall
import io.github.alexzhirkevich.keight.expressions.OpCase
import io.github.alexzhirkevich.keight.expressions.OpCompare
import io.github.alexzhirkevich.keight.expressions.OpConstant
import io.github.alexzhirkevich.keight.expressions.OpContinue
import io.github.alexzhirkevich.keight.expressions.OpDoWhileLoop
import io.github.alexzhirkevich.keight.expressions.OpEquals
import io.github.alexzhirkevich.keight.expressions.OpExp
import io.github.alexzhirkevich.keight.expressions.OpForInLoop
import io.github.alexzhirkevich.keight.expressions.OpForLoop
import io.github.alexzhirkevich.keight.expressions.OpGetProperty
import io.github.alexzhirkevich.keight.expressions.OpIfCondition
import io.github.alexzhirkevich.keight.expressions.OpIn
import io.github.alexzhirkevich.keight.expressions.OpIncDecAssign
import io.github.alexzhirkevich.keight.expressions.OpIndex
import io.github.alexzhirkevich.keight.expressions.OpKeyValuePair
import io.github.alexzhirkevich.keight.expressions.OpLongInt
import io.github.alexzhirkevich.keight.expressions.OpLongLong
import io.github.alexzhirkevich.keight.expressions.OpMakeArray
import io.github.alexzhirkevich.keight.expressions.OpNot
import io.github.alexzhirkevich.keight.expressions.OpNotEquals
import io.github.alexzhirkevich.keight.expressions.OpReturn
import io.github.alexzhirkevich.keight.expressions.OpSpread
import io.github.alexzhirkevich.keight.expressions.OpSwitch
import io.github.alexzhirkevich.keight.expressions.OpTouple
import io.github.alexzhirkevich.keight.expressions.OpTryCatch
import io.github.alexzhirkevich.keight.expressions.OpWhileLoop
import io.github.alexzhirkevich.keight.expressions.ThrowableValue
import io.github.alexzhirkevich.keight.fastAll
import io.github.alexzhirkevich.keight.fastMap
import io.github.alexzhirkevich.keight.js.FunctionParam
import io.github.alexzhirkevich.keight.js.JSError
import io.github.alexzhirkevich.keight.js.JSFunction
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.Object
import io.github.alexzhirkevich.keight.js.OpClassInit
import io.github.alexzhirkevich.keight.js.StaticClassMember
import io.github.alexzhirkevich.keight.js.SyntaxError
import io.github.alexzhirkevich.keight.js.TypeError
import io.github.alexzhirkevich.keight.js.joinSuccess
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.math.pow

internal fun List<Token>.parse() : Expression {
    return sanitize()
        .listIterator()
        .parseBlock(
            scoped = false,
            isExpressible = true,
            blockContext = emptyList(),
            type = ExpectedBlockType.Block
        )
}

internal enum class ExpectedBlockType {
    None, Object, Block
}

internal enum class BlockContext {
    None, Loop, Switch, Function, Class
}

private fun unexpected(expr : String) : String = "Unexpected token '$expr'"

private fun List<Token>.sanitize() : List<Token> {
    return fold(mutableListOf<Token>()) { l, token ->
        // skip comments, double newlines and newlines after semicolon
        if (
            token !is Token.Comment &&
            (token !is Token.NewLine || (l.lastOrNull() !is Token.NewLine && l.lastOrNull() !is Token.Operator.SemiColon))
        ) {
            l.add(token)
        }
        l
    }
}

private inline fun ListIterator<Token>.eat(token: Token) : Boolean {
    if (nextSignificant() == token){
        return true
    } else {
        prevSignificant()
        return false
    }
}

private inline fun <reified R : Token> ListIterator<Token>.nextIsInstance() : Boolean {
    if (!hasNext())
        return false

    return (nextSignificant() is R).also { prevSignificant() }
}

private fun ListIterator<Token>.nextSignificant() : Token {
    var n = next()
    while (n is Token.NewLine){
        n = next()
    }
    return n
}

private fun ListIterator<Token>.prevSignificant() : Token {
    var n = previous()
    while (n is Token.NewLine){
        n = previous()
    }
    return n
}

private fun ListIterator<Token>.parseStatement(
    blockContext: List<BlockContext> = emptyList(),
    precedence: Int = 15,
    blockType: ExpectedBlockType,
) : Expression {
    var x =  if (precedence == 0) {
        parseFactor(blockContext, blockType)
    } else {
        parseStatement(blockContext, precedence - 1, blockType)
    }

    while (true) {
        x = when (precedence) {
            0 -> when (nextSignificant()) {
                Token.Operator.Bracket.RoundOpen -> {
                    prevSignificant()
                    parseFunctionCall(x)
                }
                Token.Operator.Period,
                Token.Operator.Bracket.SquareOpen -> {
                    prevSignificant()
                    parseMemberOf(x)
                }
                Token.Operator.OptionalChaining-> parseOptionalChaining(x)

                else -> return x.also { prevSignificant() }
            }
            1 -> when (val it = nextSignificant()) {
                is Token.Operator.Arithmetic.Inc,
                is Token.Operator.Arithmetic.Dec -> {
                    syntaxCheck(x.isAssignable()) {
                        "Value is not assignable"
                    }
                    OpIncDecAssign(
                        variable = x,
                        isPrefix = false,
                        isInc = it is Token.Operator.Arithmetic.Inc
                    )
                }
                else -> return x.also { prevSignificant() }
            }

            2 -> when (nextSignificant()) {
                Token.Operator.Arithmetic.Exp -> OpExp(
                    x = x,
                    degree = parseStatement(blockContext,  precedence, ExpectedBlockType.Object)
                )
                else -> return x.also { prevSignificant() }
            }
            3 -> when (nextSignificant()) {
                Token.Operator.Arithmetic.Mul -> Delegate(
                    a = x,
                    b = parseStatement(blockContext, precedence-1, ExpectedBlockType.Object),
                    op = ScriptRuntime::mul
                )
                Token.Operator.Arithmetic.Div -> Delegate(
                    a = x,
                    b = parseStatement(blockContext, precedence-1, ExpectedBlockType.Object),
                    op = ScriptRuntime::div
                )
                Token.Operator.Arithmetic.Mod -> Delegate(
                    a = x,
                    b = parseStatement(blockContext,  precedence-1, ExpectedBlockType.Object),
                    op = ScriptRuntime::mod
                )
                else -> return x.also { prevSignificant() }
            }
            4 -> when (nextSignificant()) {
                Token.Operator.Arithmetic.Plus -> Delegate(
                    a = x,
                    b = parseStatement(blockContext, precedence-1, ExpectedBlockType.Object),
                    op = ScriptRuntime::sum
                )
                Token.Operator.Arithmetic.Minus -> Delegate(
                    a = x,
                    b = parseStatement(blockContext,  precedence-1, ExpectedBlockType.Object),
                    op = ScriptRuntime::sub
                )

                else -> return x.also { prevSignificant() }
            }
            5 ->  when (nextSignificant()) {
                Token.Operator.Bitwise.Shl -> OpLongInt(
                    a = x,
                    b = parseStatement(blockContext, precedence-1, ExpectedBlockType.Object),
                    op = Long::shl
                )

                Token.Operator.Bitwise.Shr -> OpLongInt(
                    a = x,
                    b = parseStatement(blockContext, precedence-1, ExpectedBlockType.Object),
                    op = Long::shr
                )
                Token.Operator.Bitwise.Ushr -> OpLongInt(
                    a = x,
                    b = parseStatement(blockContext, precedence-1, ExpectedBlockType.Object),
                    op = Long::ushr
                )
                else -> return x.also { prevSignificant() }

            }
            6 ->  when (nextSignificant()) {
                Token.Operator.In -> parseInOperator(x, precedence)
                Token.Operator.Instanceof -> parseInstanceOfOperator(x, precedence)
                else -> return x.also { prevSignificant() }
            }
            7 ->  when (nextSignificant()) {
                Token.Operator.Compare.Less -> OpCompare(
                    a = x,
                    b = parseStatement(blockContext,  precedence, ExpectedBlockType.Object),
                    result = { it < 0 }
                )
                Token.Operator.Compare.LessOrEq -> OpCompare(
                    a = x,
                    b = parseStatement(blockContext, precedence, ExpectedBlockType.Object),
                    result = { it <= 0 }
                )
                Token.Operator.Compare.Greater -> OpCompare(
                    a = x,
                    b = parseStatement(blockContext, precedence, ExpectedBlockType.Object),
                    result = { it > 0 }
                )
                Token.Operator.Compare.GreaterOrEq -> OpCompare(
                    a = x,
                    b = parseStatement(blockContext, precedence, ExpectedBlockType.Object),
                    result = { it >= 0 }
                )

                else -> return x.also { prevSignificant() }
            }
            8 -> when (nextSignificant()) {
                Token.Operator.Compare.Eq -> OpEquals(
                    a = x,
                    b = parseStatement(blockContext, precedence, ExpectedBlockType.Object),
                    isTyped = false
                )
                Token.Operator.Compare.StrictEq -> OpEquals(
                    a = x,
                    b = parseStatement(blockContext,  precedence, ExpectedBlockType.Object),
                    isTyped = true
                )
                Token.Operator.Compare.Neq -> OpNotEquals(
                    a = x,
                    b = parseStatement(blockContext,  precedence, ExpectedBlockType.Object),
                    isTyped = false
                )
                Token.Operator.Compare.StrictNeq -> OpNotEquals(
                    a = x,
                    b = parseStatement(blockContext,  precedence, ExpectedBlockType.Object),
                    isTyped = true
                )

                else -> return x.also { prevSignificant() }
            }
            9 -> when (nextSignificant()) {
                Token.Operator.Bitwise.And -> OpLongLong(
                    a = x,
                    b = parseStatement(blockContext, precedence, ExpectedBlockType.Object),
                    op = Long::and
                )
                else -> return x.also { prevSignificant() }
            }
            10 -> when (nextSignificant()) {
                Token.Operator.Bitwise.Xor -> OpLongLong(
                    a = x,
                    b = parseStatement(blockContext, precedence, ExpectedBlockType.Object),
                    op = Long::xor
                )
                else -> return x.also { prevSignificant() }
            }
            11 -> when (nextSignificant()) {
                Token.Operator.Bitwise.Or -> OpLongLong(
                    a = x,
                    b = parseStatement(blockContext, precedence, ExpectedBlockType.Object),
                    op = Long::or
                )
                else -> return x.also { prevSignificant() }
            }
            12 -> when (nextSignificant()) {
                Token.Operator.Logical.And -> {
                    val a = x
                    val b = parseStatement(blockContext, precedence, ExpectedBlockType.Object)
                    Expression {
                        val av = a(it)
                        if (it.isFalse(av)){
                            return@Expression av
                        }
                        b(it)
                    }
                }
                else -> return x.also { prevSignificant() }
            }

            13 ->  when (nextSignificant()) {
                Token.Operator.Logical.Or -> {
                    val a = x
                    val b = parseStatement(blockContext, precedence, ExpectedBlockType.Object)
                    Expression {
                        val av = a(it)
                        if (!it.isFalse(av)){
                            return@Expression av
                        }
                        b(it)
                    }
                }
                Token.Operator.NullishCoalescing -> {
                    val replacement = parseStatement(blockContext, precedence, ExpectedBlockType.Object)
                    val subject = x
                    Expression { subject(it)?.takeUnless { it is Unit } ?: replacement(it) }
                }
                else -> return x.also { prevSignificant() }
            }

            14 -> when (nextSignificant()) {
                Token.Operator.QuestionMark -> parseTernary(
                    condition = x,
                    blockContext = blockContext
                )
                Token.Operator.Arrow -> OpConstant(parseArrowFunction(blockContext, x))

                else -> return x.also { prevSignificant() }
            }

            15 -> when (val next = nextSignificant()) {
                is Token.Operator.Assign -> parseAssignmentValue(
                    x, getMergeForAssignment(next)
                )

                else -> return x.also { prevSignificant() }
            }
            else -> error("Invalid operator priority - $precedence")
        }
    }
}

private fun ListIterator<Token>.parseFactor(
    blockContext: List<BlockContext>,
    blockType: ExpectedBlockType = ExpectedBlockType.None
): Expression {
    val expr =  when (val next = nextSignificant()) {
        is Token.Str -> OpConstant(next.value)
        is Token.TemplateString -> {
            val expressions = next.tokens.fastMap {
                when (it) {
                    is TemplateStringToken.Str -> OpConstant(it.value)
                    is TemplateStringToken.Template -> it.value.sanitize().listIterator()
                        .parseBlock(
                            scoped = true,
                            type = ExpectedBlockType.Block,
                            isExpressible = true,
                            blockContext = emptyList()
                    )
                }
            }
            Expression { r ->
                expressions.fastMap { it(r) }.joinToString("")
            }
        }
        is Token.Operator.Period, is Token.Num -> {
            val num = if (next is Token.Num) {
                next.value
            } else {
                val number = next()
                syntaxCheck(
                    number is Token.Num
                            && !number.isFloat
                            && number.format == NumberFormat.Dec
                ) {
                    unexpected(".")
                }
                "0.${number.value}".toDouble()
            }
            OpConstant(num)
        }

        Token.Operator.Spread -> OpSpread(parseFactor(blockContext))
        Token.Operator.Arithmetic.Inc,
        Token.Operator.Arithmetic.Dec -> {
            val isInc = next is Token.Operator.Arithmetic.Inc
            val variable = parseFactor(blockContext)
            require(variable.isAssignable()) {
                unexpected(if (isInc) "++" else "--")
            }
            OpIncDecAssign(
                variable = variable,
                isPrefix = true,
                isInc = isInc
            )
        }

        Token.Operator.Arithmetic.Plus,
        Token.Operator.Arithmetic.Minus -> Delegate(
            a = parseFactor(blockContext = blockContext),
            op = if (next is Token.Operator.Arithmetic.Plus)
                ScriptRuntime::pos else ScriptRuntime::neg
        )

        Token.Operator.Logical.Not -> OpNot(
            condition = parseStatement(
                blockContext = blockContext,
                blockType = ExpectedBlockType.Object
            )
        )

        Token.Operator.Bitwise.Reverse -> {
            val expr = parseStatement(
                blockContext = blockContext,
                blockType = ExpectedBlockType.Object
            )
            Expression {
                it.toNumber(expr(it)).toLong().inv()
            }
        }

        Token.Operator.Bracket.CurlyOpen -> {
            prevSignificant()
            parseBlock(
                blockContext = blockContext,
                type = blockType
            )
        }

        Token.Operator.Bracket.RoundOpen -> {
            prevSignificant()
            parseExpressionGrouping()
        }
        Token.Operator.Bracket.SquareOpen -> {
            prevSignificant()
            parseArrayCreation()
        }
        is Token.Operator.New -> parseNew()
        is Token.Operator.Typeof -> parseTypeof()
        is Token.Operator.Delete -> parseDelete()
        is Token.Identifier.Keyword -> parseKeyword(next, blockContext)
        is Token.Identifier.Property -> OpGetProperty(next.identifier, receiver = null)
        else -> throw SyntaxError(unexpected(next::class.simpleName.orEmpty()))
    }

    return expr

}

private fun ListIterator<Token>.parseAssignmentValue(
    x: Expression,
    merge: (ScriptRuntime.(Any?, Any?) -> Any?)? = null
): Expression {
    return when (x) {
        is OpIndex -> OpAssignByIndex(
            receiver = x.receiver,
            index = x.index,
            assignableValue = parseStatement(blockType = ExpectedBlockType.Object),
            merge = merge
        )

        is OpGetProperty -> OpAssign(
            variableName = x.name,
            receiver = x.receiver,
            assignableValue = parseStatement(blockType = ExpectedBlockType.Object),
            merge = merge
        )

        else -> throw SyntaxError("Invalid left-hand in assignment")
    }
}

private fun getMergeForAssignment(operator: Token.Operator.Assign): (ScriptRuntime.(Any?, Any?) -> Any?)? {
    return when (operator) {
        Token.Operator.Assign.Assignment -> null
        Token.Operator.Assign.PlusAssign -> ScriptRuntime::sum
        Token.Operator.Assign.MinusAssign -> ScriptRuntime::sub
        Token.Operator.Assign.MulAssign -> ScriptRuntime::mul
        Token.Operator.Assign.DivAssign -> ScriptRuntime::div
        Token.Operator.Assign.ExpAssign -> { a, b ->
            toNumber(a).toDouble().pow(toNumber(b).toDouble())
        }

        Token.Operator.Assign.ModAssign -> { a, b ->
            toNumber(a).toLong() and toNumber(b).toLong()
        }

        Token.Operator.Assign.BitAndAssign -> { a, b ->
            toNumber(a).toLong() and toNumber(b).toLong()
        }

        Token.Operator.Assign.LogicAndAssign -> { a, b ->
            if (isFalse(a)) a else b
        }

        Token.Operator.Assign.BitOrAssign -> { a, b ->
            toNumber(a).toLong() or toNumber(b).toLong()
        }

        Token.Operator.Assign.LogicOrAssign -> { a, b ->
            if (isFalse(a)) b else a
        }

        Token.Operator.Assign.BitXorAssign -> { a, b ->
            toNumber(a).toLong() xor toNumber(b).toLong()
        }

        Token.Operator.Assign.UshrAssign -> { a, b ->
            toNumber(a).toLong() ushr toNumber(b).toInt()
        }

        Token.Operator.Assign.ShrAssign -> { a, b ->
            toNumber(a).toLong() shr toNumber(b).toInt()
        }

        Token.Operator.Assign.ShlAssign -> { a, b ->
            toNumber(a).toLong() shl toNumber(b).toInt()
        }

        Token.Operator.Assign.NullCoalescingAssign -> { a, b ->
            if (a == null || a is Unit) b else a
        }
    }
}

private fun ListIterator<Token>.parseKeyword(keyword: Token.Identifier.Keyword, blockContext: List<BlockContext>): Expression {
    return when(keyword){
        Token.Identifier.Keyword.Var,
        Token.Identifier.Keyword.Let,
        Token.Identifier.Keyword.Const, -> parseVariable(
            when(keyword){
                Token.Identifier.Keyword.Var -> VariableType.Global
                Token.Identifier.Keyword.Let -> VariableType.Local
                else -> VariableType.Const
            }
        )
        Token.Identifier.Keyword.True -> OpConstant(true)
        Token.Identifier.Keyword.False -> OpConstant(false)
        Token.Identifier.Keyword.Null -> OpConstant(null)

        Token.Identifier.Keyword.Switch -> parseSwitch(blockContext)
        Token.Identifier.Keyword.Case,
        Token.Identifier.Keyword.Default -> {
            syntaxCheck(blockContext.last() == BlockContext.Switch) {
                "Unexpected token 'case'"
            }
            val case = if (keyword == Token.Identifier.Keyword.Case)
                parseFactor(emptyList())
            else OpCase.Default

            syntaxCheck(nextSignificant() is Token.Operator.Colon) {
                "Expected ':' after 'case'"
            }
            OpCase(case)
        }

        Token.Identifier.Keyword.For -> parseForLoop(blockContext)
        Token.Identifier.Keyword.While -> parseWhileLoop(blockContext)
        Token.Identifier.Keyword.Do -> parseDoWhileLoop(blockContext)
        Token.Identifier.Keyword.Continue -> OpContinue.also {
            syntaxCheck(blockContext.lastOrNull() == BlockContext.Loop) {
                unexpected("continue")
            }
        }
        Token.Identifier.Keyword.Break -> OpBreak.also {
            val context = blockContext.lastOrNull()
            syntaxCheck(context == BlockContext.Loop || context == BlockContext.Switch){
                unexpected("break")
            }
        }

        Token.Identifier.Keyword.If ->  OpIfCondition(
            condition = parseExpressionGrouping(),
            onTrue = parseBlock(blockContext = blockContext),
            onFalse = if (eat(Token.Identifier.Keyword.Else)) {
                parseBlock(blockContext = blockContext)
            } else null
        )
        Token.Identifier.Keyword.Function -> OpConstant(parseFunction(blockContext = blockContext))
        Token.Identifier.Keyword.Return -> {
            syntaxCheck(BlockContext.Function in blockContext) {
                unexpected("return")
            }
            val next = next()
            if (next == Token.NewLine || next == Token.Operator.SemiColon){
                OpReturn(OpConstant(Unit))
            } else {
                previous()
                OpReturn(parseStatement(blockType = ExpectedBlockType.Object))
            }
        }

        Token.Identifier.Keyword.Class -> parseClass()
        Token.Identifier.Keyword.Throw -> {
            val throwable = parseStatement(blockType = ExpectedBlockType.Object)
            Expression {
                val t = throwable(it)
                throw if (t is Throwable) t else ThrowableValue(t)
            }
        }
        Token.Identifier.Keyword.Async -> parseAsync()
        Token.Identifier.Keyword.Await -> parseAwait()
        Token.Identifier.Keyword.Try -> parseTryCatch(blockContext)
        Token.Identifier.Keyword.This -> Expression { it.thisRef }
        Token.Identifier.Keyword.Else,
        Token.Identifier.Keyword.Extends,
        Token.Identifier.Keyword.Finally,
        Token.Identifier.Keyword.Catch -> throw SyntaxError(unexpected(keyword.identifier))
    }
}

private fun ListIterator<Token>.parseNew() : Expression {
    val next = nextSignificant()
    syntaxCheck(next is Token.Identifier.Property){
        "Invalid syntax after 'new'"
    }

    if (!nextIsInstance<Token.Operator.Bracket.RoundOpen>()) {
        return OpCall(
            callable = OpGetProperty(next.identifier, receiver = null),
            arguments = emptyList(),
            isOptional = false
        )
    }

    val args = parseExpressionGrouping().expressions
    return Expression { runtime ->
        val constructor = runtime.get(next.identifier)
        syntaxCheck(constructor is Constructor) {
            "'${next.identifier}' is not a constructor"
        }
        constructor.construct(args.fastMap { it(runtime) }, runtime)
    }
}

private fun ListIterator<Token>.parseTypeof() : Expression {
    val isArg = nextIsInstance<Token.Operator.Bracket.RoundOpen>()
    val expr = if (isArg) {
        parseExpressionGrouping()
    }  else {
        parseStatement(precedence = 1,blockType = ExpectedBlockType.Object)
    }
    return Expression {
        when (val v = expr(it)) {
            null -> "object"
            Unit -> "undefined"
            true, false -> "boolean"
            is CharSequence -> "string"
            is JsAny -> v.type
            is Callable, is Function<*> -> "function"
            else -> v::class.simpleName
        }
    }
}

private fun ListIterator<Token>.parseDelete() : Expression {
    val x = parseStatement(precedence = 1, blockType = ExpectedBlockType.Object)

    val (subj, obj) = when (x) {
        is OpIndex -> x.receiver to x.index
        is OpGetProperty -> (x.receiver ?: Expression { it.get("this") }) to OpConstant(x.name)
        else -> throw SyntaxError(unexpected("delete"))
    }
    return Expression {
        (subj(it) as JsAny).delete(obj(it), it)
    }
}


private fun ListIterator<Token>.parseArrayCreation(): Expression {
    check(eat(Token.Operator.Bracket.SquareOpen))

    val expressions = buildList {
        while (!eat(Token.Operator.Bracket.SquareClose)) {
            if (eat(Token.Operator.Comma)) {
                add(OpConstant(Unit))
            } else {
                add(parseStatement(blockType = ExpectedBlockType.Object))
                if (!eat(Token.Operator.Comma)) {
                    syntaxCheck(nextSignificant() is Token.Operator.Bracket.SquareClose) {
                        "Expected ')'"
                    }
                    break
                }
            }
        }
    }

    return OpMakeArray(expressions)
}

private fun ListIterator<Token>.parseExpressionGrouping(): OpTouple {
    check(eat(Token.Operator.Bracket.RoundOpen))

    val expressions = if (nextIsInstance<Token.Operator.Bracket.RoundClose>()) {
        emptyList()
    } else buildList {
        do {
            add(parseStatement(emptyList(),blockType = ExpectedBlockType.Object))
        } while (nextSignificant() is Token.Operator.Comma)
        prevSignificant()
    }
    syntaxCheck(eat(Token.Operator.Bracket.RoundClose)) {
        "Expected ')'"
    }

    return OpTouple(expressions)
}

private fun ListIterator<Token>.parseMemberOf(receiver: Expression, ): Expression {
    return when (nextSignificant()){

        is Token.Operator.Period -> {
            val next = nextSignificant()
            syntaxCheck(next is Token.Identifier) {
                "Illegal symbol after '.'"
            }
            OpGetProperty(name = next.identifier, receiver = receiver)
        }
        is Token.Operator.Bracket.SquareOpen -> {
            OpIndex(
                receiver = receiver,
                index = parseStatement(blockType = ExpectedBlockType.Object)
            ).also {
                syntaxCheck(nextSignificant() is Token.Operator.Bracket.SquareClose) {
                    "Missing ']'"
                }
            }
        }
        else -> throw IllegalStateException("Illegal 'member of' syntax")
    }
}

private fun ListIterator<Token>.parseOptionalChaining(receiver: Expression): Expression {
    return when(val next = nextSignificant()){
        is Token.Operator.Bracket.SquareOpen -> {
            OpIndex(
                receiver = receiver,
                index = parseStatement(blockType = ExpectedBlockType.Object),
                isOptional = true
            ).also {
                syntaxCheck(nextSignificant() is Token.Operator.Bracket.SquareClose) {
                    "Missing ']'"
                }
            }
        }
        is Token.Operator.Bracket.RoundOpen -> {
            prevSignificant()
            parseFunctionCall(receiver, optional = true)
        }
        is Token.Identifier -> {
            OpGetProperty(
                name = next.identifier,
                receiver = receiver,
                isOptional = true
            )
        }
        else -> throw SyntaxError("Invalid usage of ?. operator")
    }
}

private fun ListIterator<Token>.parseFunctionCall(function : Expression, optional : Boolean = false) : Expression {
    return OpCall(
        callable = function,
        arguments = parseExpressionGrouping().expressions,
        isOptional = optional
    )
}


private fun ListIterator<Token>.parseInOperator(subject : Expression, precedence: Int) : Expression {
    val obj = parseStatement(precedence = precedence, blockType = ExpectedBlockType.Object)
    return OpIn(
        property = subject,
        inObject = obj
    )
}

private fun ListIterator<Token>.parseInstanceOfOperator(subject : Expression, precedence: Int) : Expression {
    val obj = parseStatement(precedence = precedence,blockType = ExpectedBlockType.Object)
    return Expression {
        val o = obj(it)
        syntaxCheck(o is Constructor) {
            "Illegal usage of 'instanceof' operator"
        }
        o.isInstance(subject(it), it)
    }
}

private fun ListIterator<Token>.parseTernary(
    condition : Expression,
    blockContext: List<BlockContext>
) : Expression {
    val bContext = blockContext.dropLastWhile { it == BlockContext.Class }
    val onTrue = parseStatement(bContext, blockType = ExpectedBlockType.Block)

    syntaxCheck(nextSignificant() is Token.Operator.Colon) {
        "Unexpected end of input"
    }

    return OpIfCondition(
        condition = condition,
        onTrue = onTrue,
        onFalse = parseStatement(bContext, blockType = ExpectedBlockType.Block),
        expressible = true
    )
}

private fun ListIterator<Token>.parseClass() : OpClassInit {
    val name = nextSignificant()

    syntaxCheck(name is Token.Identifier) {
        "Invalid class declaration"
    }

    val extends = if (eat(Token.Identifier.Keyword.Extends)) {
        parseStatement(blockType = ExpectedBlockType.Object)
    } else null

    syntaxCheck(eat(Token.Operator.Bracket.CurlyOpen)) {
        "Invalid class declaration"
    }

    val staticMembers = mutableListOf<StaticClassMember>()
    val properties = mutableMapOf<String, Expression>()
    var construct : JSFunction? = null

    while (!eat(Token.Operator.Bracket.CurlyClose)) {
        val token = nextSignificant()

        when {
            token is Token.Identifier && nextIsInstance<Token.Operator.Bracket.RoundOpen>() -> {
                val f =  parseFunction(
                    name = token.identifier,
                    blockContext = listOf(BlockContext.Class)
                )
                if (token.identifier == "constructor"){
                    syntaxCheck(construct == null){
                        "A class may only have one constructor"
                    }
                    construct = f
                }
                properties[token.identifier] = OpConstant(f)
            }

            token is Token.Identifier.Property && token.identifier == "static" -> {
                staticMembers.add(parseStaticClassMember())
            }

            token is Token.Identifier -> {
                prevSignificant()
                when (val statement = parseStatement(blockType = ExpectedBlockType.None)) {
                    is OpAssign -> properties[statement.variableName] = statement.assignableValue
                    is OpGetProperty -> properties[statement.name] = OpConstant(Unit)
                    else -> throw SyntaxError("Invalid class member")
                }
            }
            else -> throw  SyntaxError("Invalid class declaration")
        }
    }

    return OpClassInit(
        name = name.identifier,
        extends = extends,
        properties = properties,
        static = staticMembers/*.reversed()*/.associateBy { it.name },
        construct = construct ?: JSFunction("")
    )
}

private fun ListIterator<Token>.parseStaticClassMember() : StaticClassMember {

    val name = nextSignificant()


    syntaxCheck(name is Token.Identifier) {
        "Invalid static class member"
    }

    return when(val n = nextSignificant()) {
        is Token.Operator.Assign.Assignment -> {
            prevSignificant()
            prevSignificant()
            val assign = parseStatement(blockType = ExpectedBlockType.Object)
            syntaxCheck(assign is OpAssign) {
                "Invalid static class member"
            }

            StaticClassMember.Variable(assign.variableName, assign.assignableValue)
        }

        is Token.Operator.Bracket.RoundOpen -> {
            prevSignificant()
            val func = parseFunction(name = name.identifier, blockContext = emptyList())

            StaticClassMember.Method(func)
        }

        else -> throw SyntaxError("Invalid static class member $n")
    }
}


private fun ListIterator<Token>.parseSwitch(blockContext: List<BlockContext>) : Expression {
    val value = parseStatement(blockType = ExpectedBlockType.Object) as OpTouple
    val body = parseBlock(
        type = ExpectedBlockType.Block,
        blockContext = blockContext + BlockContext.Switch
    ) as OpBlock
    return OpSwitch(
        value = value.expressions.single(),
        cases = body.expressions
    )
}

private fun ListIterator<Token>.parseForLoop(parentBlockContext: List<BlockContext>): Expression {

    syntaxCheck(nextSignificant() is Token.Operator.Bracket.RoundOpen) {
        "For loop must be followed by '('"
    }

    val assign = if (eat(Token.Operator.SemiColon))
        null
    else parseBlock(scoped = false, blockContext = emptyList())

    // for (x in y)
    if (assign is OpBlock) {
        val opIn = assign.expressions.singleOrNull() as? OpIn
        if (opIn != null) {
            return parseForInLoop(opIn, parentBlockContext)
        }
    }

    if (assign != null) {
        syntaxCheck(nextSignificant() is Token.Operator.SemiColon) {
            "Invalid for loop"
        }
    }

    val comparison = if (eat(Token.Operator.SemiColon))
        null else parseStatement(blockType = ExpectedBlockType.Block)

    if (comparison != null) {
        syntaxCheck(nextSignificant() is Token.Operator.SemiColon) {
            "Invalid for loop"
        }
    }

    val increment = if (eat(Token.Operator.Bracket.RoundClose))
        null else parseBlock(scoped = false, blockContext = emptyList())

    if (increment != null) {
        syntaxCheck(nextSignificant() is Token.Operator.Bracket.RoundClose) {
            "Invalid for loop"
        }
    }

    val body = parseBlock(blockContext = parentBlockContext + BlockContext.Loop)


    return OpForLoop(
        assignment = assign,
        increment = increment,
        comparison = comparison,
        body = body
    )
}

private fun ListIterator<Token>.parseForInLoop(opIn: OpIn, parentBlockContext : List<BlockContext>) : Expression {
    syntaxCheck(nextSignificant() is Token.Operator.Bracket.RoundClose) {
        "Invalid for loop"
    }

    val prepare = OpAssign(
        type = opIn.variableType,
        variableName = when (opIn.property){
            is OpAssign -> opIn.property.variableName
            is OpGetProperty -> opIn.property.name
            else -> throw SyntaxError("Invalid for..of loop syntax")
        },
        assignableValue = OpConstant(Unit),
        merge = null
    )

    return OpForInLoop(
        prepare = prepare,
        assign = { r, v -> r.set(prepare.variableName, v, null) },
        inObject = opIn.inObject,
        body = parseBlock(blockContext = parentBlockContext + BlockContext.Loop)
    )
}

private fun ListIterator<Token>.parseWhileLoop(parentBlockContext: List<BlockContext>): Expression {
    return OpWhileLoop(
        condition = parseExpressionGrouping(),
        body = parseBlock(blockContext = parentBlockContext + BlockContext.Loop),
    )
}

private fun ListIterator<Token>.parseDoWhileLoop(blockContext: List<BlockContext>) : Expression {
    val body = parseBlock(type = ExpectedBlockType.Block, blockContext = blockContext + BlockContext.Loop)

    syntaxCheck(eat(Token.Identifier.Keyword.While)) {
        "Missing while condition in do/while block"
    }
    val condition = parseExpressionGrouping()

    return OpDoWhileLoop(
        condition = condition,
        body = body as OpBlock,
    )
}

private fun ListIterator<Token>.parseAsync(): Expression {
    val subject = parseStatement(blockType = ExpectedBlockType.Object)

    syntaxCheck(subject is OpConstant && subject.value is JSFunction && !subject.value.isAsync) {
        "Illegal usage of 'async' keyword"
    }

    return OpConstant(subject.value.copy(isAsync = true))
}

private fun ListIterator<Token>.parseAwait(): Expression {
    val subject = parseStatement(blockType = ExpectedBlockType.Object)

    return Expression {
        if (!it.isSuspendAllowed) {
            throw JSError("Await is not allowed in current context")
        }

        val job = it.toKotlin(subject(it))
        typeCheck(job is Job){
            "$job is not a Promise"
        }

        if (job is Deferred<*>){
            job.await()
        } else {
            job.joinSuccess()
        }
    }
}

private fun ListIterator<Token>.parseTryCatch(blockContext: List<BlockContext>): Expression {
    val tryBlock = parseBlock(type = ExpectedBlockType.Block, blockContext = blockContext)
    val catchBlock = if (eat(Token.Identifier.Keyword.Catch)) {
        if (eat(Token.Operator.Bracket.RoundOpen)) {
            val next = nextSignificant()
            syntaxCheck(next is Token.Identifier && eat(Token.Operator.Bracket.RoundClose)){
                "Invalid syntax after 'catch'"
            }
            next.identifier to parseBlock(
                scoped = false,
                type = ExpectedBlockType.Block,
                blockContext = blockContext
            )
        } else {
            null to parseBlock(type = ExpectedBlockType.Block, blockContext = blockContext)
        }
    } else null

    val finallyBlock = if (eat(Token.Identifier.Keyword.Finally)) {
        parseBlock(type = ExpectedBlockType.Block, blockContext = blockContext)
    } else null

    return OpTryCatch(
        tryBlock = tryBlock,
        catchVariableName = catchBlock?.first,
        catchBlock = catchBlock?.second,
        finallyBlock = finallyBlock
    )
}

private fun ListIterator<Token>.parseArrowFunction(blockContext: List<BlockContext>, args: Expression) : JSFunction {
    val fArgs = when(args){
        is OpTouple -> args.expressions
        else -> listOf(args)
    }.filterIsInstance<OpGetProperty>()

    val lambda = parseBlock(
        type = ExpectedBlockType.Block,
        blockContext = blockContext + BlockContext.Function,
        allowCommaSeparator = false
    ) as OpBlock

    syntaxCheck (lambda.isSurroundedWithBraces || lambda.expressions.size <= 1){
        "Invalid arrow function"
    }

    return JSFunction(
        name = "",
        parameters = fArgs.map { FunctionParam(it.name) },
        body = lambda.copy(isExpressible = !lambda.isSurroundedWithBraces),
        isArrow = true
    )
}

private fun ListIterator<Token>.parseFunction(
    name: String? = null,
    blockContext: List<BlockContext>
) : JSFunction {

    val actualName = name ?: run {
        if (nextIsInstance<Token.Identifier.Property>()) {
            (nextSignificant() as Token.Identifier.Property).identifier
        } else {
            ""
        }
    }
    val touple = parseStatement(blockType = ExpectedBlockType.None)

    syntaxCheck (touple is OpTouple) {
        "Invalid function declaration"
    }
    val args = touple.expressions.map(Expression::toFunctionParam)

    val block = parseBlock(
        scoped = false,
        blockContext = blockContext + BlockContext.Function
    )

    return JSFunction(
        name = actualName,
        parameters = args,
        body = block
    )
}

private fun Expression.toFunctionParam() : FunctionParam {
    return when (this) {
        is OpGetProperty -> FunctionParam(name = name, default = null)
        is OpSpread -> value.toFunctionParam().let {
            FunctionParam(
                name = it.name,
                isVararg = true,
                default = it.default
            )
        }
        is OpAssign -> FunctionParam(
            name = variableName,
            default = assignableValue
        )

        else -> throw SyntaxError("Invalid function declaration")
    }
}


private fun ListIterator<Token>.parseBlock(
    scoped: Boolean = true,
    type: ExpectedBlockType = ExpectedBlockType.None,
    isExpressible: Boolean = false,
    allowCommaSeparator : Boolean = true,
    blockContext: List<BlockContext>,
): Expression {
    var funcIndex = 0

    var isSurroundedWithBraces = false
    val list = buildList {
        if (eat(Token.Operator.Bracket.CurlyOpen)) {
            isSurroundedWithBraces = true
            while (!nextIsInstance<Token.Operator.Bracket.CurlyClose>()) {
                val expr = parseStatement(blockContext, blockType = ExpectedBlockType.None)

                if (type != ExpectedBlockType.Block
                    && (expr is OpGetProperty && expr.receiver == null || expr is OpConstant && expr.value is CharSequence)
                    && eat(Token.Operator.Colon)
                ) {
                    val value = parseStatement(blockContext, blockType = ExpectedBlockType.Object)
                    val name = when (expr){
                        is OpConstant -> expr.value.toString()
                        is OpGetProperty -> expr.name
                        else -> error("Should never happen")
                    }
                    add(OpKeyValuePair(name, value))
                } else {
                    when {
                        expr is OpClassInit -> {
                            add(
                                index = funcIndex++,
                                element = OpAssign(
                                    type = VariableType.Local,
                                    variableName = expr.name,
                                    receiver = null,
                                    assignableValue = expr,
                                    merge = null
                                )
                            )
                        }

                        expr is OpConstant && expr.value is JSFunction -> {
                            val name = (expr.value as Named).name

                            add(
                                index = funcIndex++,
                                element = OpAssign(
                                    type = VariableType.Local,
                                    variableName = name,
                                    receiver = null,
                                    assignableValue = expr,
                                    merge = null
                                )
                            )
                        }

                        else -> add(expr)
                    }
                }
                while (hasNext()) {
                    val next = next()
                    if (next !is Token.NewLine && next !is Token.Operator.SemiColon && next !is Token.Operator.Comma) {
                        previous()
                        break
                    }
                }
            }

            check(nextSignificant() is Token.Operator.Bracket.CurlyClose) {
                "} was expected"
            }
        } else {

            while (eat(Token.Operator.New)) {
                //skip
            }
            do {
                add(parseStatement(blockContext, blockType = type))
            } while (allowCommaSeparator && eat(Token.Operator.Comma))
        }
    }

    return if (
        type != ExpectedBlockType.Block
        && isSurroundedWithBraces && list.fastAll { it is OpKeyValuePair || it is OpSpread }
    ) {
        Expression { r ->
            Object("") {
                list.forEach { expr ->
                    when (expr) {
                        is OpKeyValuePair -> {
                            expr.key eq expr.value(r)
                        }

                        is OpSpread -> {
                            val any = expr.value(r) as JsAny
                            any.keys.forEach {
                                it eq any.get(it, r)
                            }
                        }
                    }
                }
            }
        }
    } else {
        OpBlock(
            expressions = list,
            isScoped = scoped,
            isExpressible = isExpressible,
            isSurroundedWithBraces = isSurroundedWithBraces
        )
    }
}

private fun ListIterator<Token>.parseVariable(type: VariableType) : Expression {
    val expressions = buildList {
        do {
            val variable = when (val expr = parseStatement(blockType = ExpectedBlockType.None)) {
                is OpAssign -> OpAssign(
                    type = type,
                    variableName = expr.variableName,
                    assignableValue = expr.assignableValue,
                    merge = null
                )

                is OpGetProperty -> OpAssign(
                    type = type,
                    variableName = expr.name,
                    assignableValue = OpConstant(Unit),
                    merge = null
                )

                // for (let x in y) ...
                is OpIn -> expr.also { it.variableType = type }

                else -> throw SyntaxError(unexpected(expr::class.simpleName.orEmpty()))
            }
            add(variable)
        } while (eat(Token.Operator.Comma))
    }

    return expressions.singleOrNull() ?: OpBlock(
        expressions = expressions,
        isScoped = false,
        isExpressible = false,
        isSurroundedWithBraces = false
    )
}

@OptIn(ExperimentalContracts::class)
internal fun checkArgs(args : List<*>?, count : Int, func : String) {
    contract {
        returns() implies (args != null)
    }
    checkNotNull(args){
        "$func call was missing"
    }
    require(args.size == count){
        "$func takes $count arguments, but ${args.size} got"
    }
}


@OptIn(ExperimentalContracts::class)
internal inline fun syntaxCheck(value: Boolean, lazyMessage: () -> Any) {
    contract {
        returns() implies value
    }

    if (!value) {
        val message = lazyMessage()
        throw SyntaxError(message.toString())
    }
}

@OptIn(ExperimentalContracts::class)
internal inline fun typeCheck(value: Boolean, lazyMessage: () -> Any) {
    contract {
        returns() implies value
    }

    if (!value) {
        val message = lazyMessage()
        throw TypeError(message.toString())
    }
}

internal fun Expression.isAssignable() : Boolean {
    return this is OpGetProperty ||
            this is OpIndex && receiver is OpGetProperty
}


