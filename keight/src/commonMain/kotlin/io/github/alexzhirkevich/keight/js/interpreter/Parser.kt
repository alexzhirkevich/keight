package io.github.alexzhirkevich.keight.js.interpreter

import io.github.alexzhirkevich.keight.Constructor
import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.Delegate
import io.github.alexzhirkevich.keight.js.JSFunction
import io.github.alexzhirkevich.keight.js.FunctionParam
import io.github.alexzhirkevich.keight.Named
import io.github.alexzhirkevich.keight.expressions.OpAssign
import io.github.alexzhirkevich.keight.expressions.OpAssignByIndex
import io.github.alexzhirkevich.keight.expressions.OpBlock
import io.github.alexzhirkevich.keight.expressions.OpBreak
import io.github.alexzhirkevich.keight.expressions.OpCase
import io.github.alexzhirkevich.keight.expressions.OpCompare
import io.github.alexzhirkevich.keight.expressions.OpConstant
import io.github.alexzhirkevich.keight.expressions.OpContinue
import io.github.alexzhirkevich.keight.expressions.OpDoWhileLoop
import io.github.alexzhirkevich.keight.expressions.OpEquals
import io.github.alexzhirkevich.keight.expressions.OpEqualsComparator
import io.github.alexzhirkevich.keight.expressions.OpExp
import io.github.alexzhirkevich.keight.expressions.OpForLoop
import io.github.alexzhirkevich.keight.expressions.OpGetProperty
import io.github.alexzhirkevich.keight.expressions.OpGreaterComparator
import io.github.alexzhirkevich.keight.expressions.OpIfCondition
import io.github.alexzhirkevich.keight.expressions.OpIncDecAssign
import io.github.alexzhirkevich.keight.expressions.OpIndex
import io.github.alexzhirkevich.keight.expressions.OpLessComparator
import io.github.alexzhirkevich.keight.expressions.OpLongInt
import io.github.alexzhirkevich.keight.expressions.OpLongLong
import io.github.alexzhirkevich.keight.expressions.OpMakeArray
import io.github.alexzhirkevich.keight.expressions.OpNot
import io.github.alexzhirkevich.keight.expressions.OpNotEquals
import io.github.alexzhirkevich.keight.expressions.OpReturn
import io.github.alexzhirkevich.keight.expressions.OpSwitch
import io.github.alexzhirkevich.keight.expressions.OpTouple
import io.github.alexzhirkevich.keight.expressions.OpTryCatch
import io.github.alexzhirkevich.keight.expressions.OpWhileLoop
import io.github.alexzhirkevich.keight.expressions.ThrowableValue
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JSClass
import io.github.alexzhirkevich.keight.js.JSError
import io.github.alexzhirkevich.keight.js.Object
import io.github.alexzhirkevich.keight.js.StaticClassMember
import io.github.alexzhirkevich.keight.js.SyntaxError
import io.github.alexzhirkevich.keight.expressions.OpCall
import io.github.alexzhirkevich.keight.expressions.OpSpread
import io.github.alexzhirkevich.keight.fastMap
import io.github.alexzhirkevich.keight.invoke
import io.github.alexzhirkevich.keight.isAssignable
import io.github.alexzhirkevich.keight.js.TypeError
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
        )
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
    isExpressionStart: Boolean = false,
    precedence: Int = 15
) : Expression {
    var x =  if (precedence == 0) {
        parseFactor(blockContext, isExpressionStart)
    } else {
        parseStatement(blockContext, isExpressionStart, precedence - 1)
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
                    degree = parseStatement(blockContext, false, precedence)
                )
                else -> return x.also { prevSignificant() }
            }
            3 -> when (nextSignificant()) {
                Token.Operator.Arithmetic.Mul -> Delegate(
                    x,
                    parseStatement(blockContext, false, precedence-1),
                    ScriptRuntime::mul
                )
                Token.Operator.Arithmetic.Div -> Delegate(
                    x,
                    parseStatement(blockContext, false, precedence-1),
                    ScriptRuntime::div
                )
                Token.Operator.Arithmetic.Mod -> Delegate(
                    x,
                    parseStatement(blockContext, false, precedence-1),
                    ScriptRuntime::mod
                )
                else -> return x.also { prevSignificant() }
            }
            4 -> when (nextSignificant()) {
                Token.Operator.Arithmetic.Plus -> Delegate(
                    x,
                    parseStatement(blockContext, false, precedence-1),
                    ScriptRuntime::sum
                )
                Token.Operator.Arithmetic.Minus -> Delegate(
                    x,
                    parseStatement(blockContext, false, precedence-1),
                    ScriptRuntime::sub
                )

                else -> return x.also { prevSignificant() }
            }
            5 ->  when (nextSignificant()) {
                Token.Operator.Bitwise.Shl -> OpLongInt(
                    x,
                    parseStatement(blockContext, false, precedence-1),
                    Long::shl
                )

                Token.Operator.Bitwise.Shr -> OpLongInt(
                    x,
                    parseStatement(blockContext, false, precedence-1),
                    Long::shr
                )
                Token.Operator.Bitwise.Ushr -> OpLongInt(
                    x,
                    parseStatement(blockContext, false, precedence-1),
                    Long::ushr
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
                    b = parseStatement(blockContext, false, precedence),
                    comparator = OpLessComparator
                )
                Token.Operator.Compare.LessOrEq -> OpCompare(
                    a = x, b = parseStatement(blockContext, false, precedence)
                ) { a, b, r ->
                    OpLessComparator(a, b, r) || OpEqualsComparator(a, b, r)
                }
                Token.Operator.Compare.Greater ->  OpCompare(
                    a = x,
                    b = parseStatement(blockContext, false, precedence),
                    comparator = OpGreaterComparator
                )
                Token.Operator.Compare.GreaterOrEq -> OpCompare(
                    a = x,
                    b = parseStatement(blockContext, false, precedence)
                ) { a, b,r  ->
                    OpGreaterComparator(a, b, r) || OpEqualsComparator(a, b,r)
                }

                else -> return x.also { prevSignificant() }
            }
            8 -> when (nextSignificant()) {
                Token.Operator.Compare.Eq -> OpEquals(
                    a = x,
                    b = parseStatement(blockContext, false, precedence),
                    isTyped = false
                )
                Token.Operator.Compare.StrictEq -> OpEquals(
                    a = x,
                    b = parseStatement(blockContext, false, precedence),
                    isTyped = true
                )
                Token.Operator.Compare.Neq -> OpNotEquals(
                    a = x,
                    b = parseStatement(blockContext, false, precedence),
                    isTyped = false
                )
                Token.Operator.Compare.StrictNeq -> OpNotEquals(
                    a = x,
                    b = parseStatement(blockContext, false, precedence),
                    isTyped = true
                )

                else -> return x.also { prevSignificant() }
            }
            9 -> when (nextSignificant()) {
                Token.Operator.Bitwise.And -> OpLongLong(
                    a = x,
                    b = parseStatement(blockContext, false, precedence),
                    op = Long::and
                )
                else -> return x.also { prevSignificant() }
            }
            10 -> when (nextSignificant()) {
                Token.Operator.Bitwise.Xor -> OpLongLong(
                    a = x,
                    b = parseStatement(blockContext, false, precedence),
                    op = Long::xor
                )
                else -> return x.also { prevSignificant() }
            }
            11 -> when (nextSignificant()) {
                Token.Operator.Bitwise.Or -> OpLongLong(
                    a = x,
                    b = parseStatement(blockContext, false, precedence),
                    op = Long::or
                )
                else -> return x.also { prevSignificant() }
            }
            12 -> when (nextSignificant()) {
                Token.Operator.Logical.And -> {
                    val a = x
                    val b = parseStatement(blockContext, false, precedence)
                    Expression {
                        !it.isFalse(a(it)) && !it.isFalse(b(it))
                    }
                }
                else -> return x.also { prevSignificant() }
            }

            13 ->  when (nextSignificant()) {
                Token.Operator.Logical.Or -> {
                    val a = x
                    val b = parseStatement(blockContext, false, precedence)
                    Expression {
                        !it.isFalse(a(it)) || !it.isFalse(b(it))
                    }
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
    isExpressionStart: Boolean = false
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
                blockContext = blockContext
            )
        )

        Token.Operator.Bitwise.Reverse -> {
            val expr = parseStatement(
                blockContext = blockContext
            )
            Expression {
                it.toNumber(expr(it)).toLong().inv()
            }
        }

        Token.Operator.Bracket.CurlyOpen -> {
            if (isExpressionStart) {
                prevSignificant()
                parseBlock(blockContext = blockContext, requireBlock = true)
            } else {
                parseObject()
            }
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
        is Token.Identifier.Keyword -> parseKeyword(next, blockContext)
        is Token.Identifier.Property -> OpGetProperty(next.name, receiver = null)

        else -> throw SyntaxError(unexpected(next::class.simpleName.orEmpty()))
    }

    return expr

}

private fun ListIterator<Token>.parseAssignmentValue(
    x: Expression,
    merge: (ScriptRuntime.(Any?, Any?) -> Any?)? = null
): Expression {
    return when {
        x is OpIndex && x.property is OpGetProperty -> OpAssignByIndex(
            variableName = x.property.name,
            scope = x.property.assignmentType,
            index = x.index,
            assignableValue = parseStatement(),
            merge = merge
        )

        x is OpGetProperty -> OpAssign(
            variableName = x.name,
            receiver = x.receiver,
            assignableValue = parseStatement(),
            type = x.assignmentType,
            merge = merge
        )

        else -> throw SyntaxError("Invalid assignment")
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
            val case = if (keyword is Token.Identifier.Keyword.Case)
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
        Token.Identifier.Keyword.Else -> throw SyntaxError(unexpected("else"))

        Token.Identifier.Keyword.Function -> OpConstant(parseFunction(blockContext = blockContext))
        Token.Identifier.Keyword.Return -> {
            syntaxCheck(BlockContext.Function in blockContext){
                unexpected("return")
            }
            OpReturn(parseStatement())
        }

        Token.Identifier.Keyword.Class -> TODO()

        Token.Identifier.Keyword.Throw -> {
            val throwable = parseStatement()
            Expression {
                val t = throwable(it)
                throw if (t is Throwable) t else ThrowableValue(t)
            }
        }
        Token.Identifier.Keyword.Try -> parseTryCatch(blockContext)
        Token.Identifier.Keyword.Finally -> throw SyntaxError(unexpected("finally"))
        Token.Identifier.Keyword.Catch -> throw SyntaxError(unexpected("catch"))
        Token.Identifier.Keyword.Async -> parseAsync()
        Token.Identifier.Keyword.Await -> parseAwait()
    }
}

private fun ListIterator<Token>.parseNew() : Expression {
    val next = nextSignificant()
    syntaxCheck(next is Token.Identifier.Property){
        "Invalid syntax after 'new'"
    }

    val args = parseExpressionGrouping().expressions

    return Expression { runtime ->
        val constructor = runtime.get(next.name)
        syntaxCheck(constructor is Constructor) {
            "'${next.name}' is not a constructor"
        }
        constructor.construct(args, runtime)
    }
}

private fun ListIterator<Token>.parseTypeof() : Expression {
    val isArg = nextIsInstance<Token.Operator.Bracket.RoundOpen>()
    val expr = if (isArg) {
        parseExpressionGrouping()
    }  else {
        parseStatement(precedence = 1)
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

private fun ListIterator<Token>.parseArrayCreation(): Expression {
    check(eat(Token.Operator.Bracket.SquareOpen))

    val expressions = buildList {
        if (nextIsInstance<Token.Operator.Bracket.SquareClose>()) {
            return@buildList
        }
        do {
            add(parseStatement())
        } while (nextSignificant() is Token.Operator.Comma)
        prevSignificant()
    }
    syntaxCheck(nextSignificant() is Token.Operator.Bracket.SquareClose) {
        "Expected ')'"
    }

    return OpMakeArray(expressions)
}

private fun ListIterator<Token>.parseExpressionGrouping(): OpTouple {
    check(eat(Token.Operator.Bracket.RoundOpen))

    val expressions = if (nextIsInstance<Token.Operator.Bracket.RoundClose>()) {
        emptyList()
    } else buildList {
        do {
            add(parseStatement(emptyList()))
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
            OpGetProperty(name = next.name, receiver = receiver)
        }
        is Token.Operator.Bracket.SquareOpen -> {
            OpIndex(
                property = receiver,
                index = parseStatement()
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
                property = receiver,
                index = parseStatement(),
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
                name = next.name,
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
    val obj = parseStatement(precedence = precedence)
    return Expression {
        val o = obj(it)
        syntaxCheck(o is JsAny) {
            "Illegal usage of 'in' operator"
        }
        o.contains(subject(it), it)
    }
}

private fun ListIterator<Token>.parseInstanceOfOperator(subject : Expression, precedence: Int) : Expression {
    val obj = parseStatement(precedence = precedence)
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
    val onTrue = parseStatement(bContext)

    syntaxCheck(nextSignificant() is Token.Operator.Colon) {
        "Unexpected end of input"
    }

    return OpIfCondition(
        condition = condition,
        onTrue = onTrue,
        onFalse = parseStatement(bContext),
        expressible = true
    )
}

private fun ListIterator<Token>.parseSwitch(blockContext: List<BlockContext>) : Expression {
    val value = parseStatement() as OpTouple
    val body = parseBlock(
        requireBlock = true,
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

    if (assign != null) {
        syntaxCheck(nextSignificant() is Token.Operator.SemiColon) {
            "Invalid for loop"
        }
    }
    val comparison = if (eat(Token.Operator.SemiColon))
        null else parseStatement()

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

private fun ListIterator<Token>.parseWhileLoop(parentBlockContext: List<BlockContext>): Expression {
    return OpWhileLoop(
        condition = parseExpressionGrouping(),
        body = parseBlock(blockContext = parentBlockContext + BlockContext.Loop),
    )
}

private fun ListIterator<Token>.parseDoWhileLoop(blockContext: List<BlockContext>) : Expression {
    val body = parseBlock(requireBlock = true, blockContext = blockContext + BlockContext.Loop)

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
    val subject = parseStatement()

    syntaxCheck(subject is OpConstant && subject.value is JSFunction && !subject.value.isAsync) {
        "Illegal usage of 'async' keyword"
    }

    return OpConstant(subject.value.copy(isAsync = true))
}

private fun ListIterator<Token>.parseAwait(): Expression {
    val subject = parseStatement()

    return Expression {
        if (!it.isSuspendAllowed) {
            throw JSError("Await is not allowed in current context", null)
        }

        val job = it.toKotlin(subject(it))
        typeCheck(job is Job){
            "Can't await $job"
        }

        if (job is Deferred<*>){
            job.await()
        } else {
            job.join()
        }
    }
}

private fun ListIterator<Token>.parseTryCatch(blockContext: List<BlockContext>): Expression {
    val tryBlock = parseBlock(requireBlock = true, blockContext = blockContext)
    val catchBlock = if (eat(Token.Identifier.Keyword.Catch)) {
        if (eat(Token.Operator.Bracket.RoundOpen)) {
            val next = nextSignificant()
            syntaxCheck(next is Token.Identifier && eat(Token.Operator.Bracket.RoundClose)){
                "Invalid syntax after 'catch'"
            }
            next.name to parseBlock(
                scoped = false,
                requireBlock = true,
                blockContext = blockContext
            )
        } else {
            null to parseBlock(requireBlock = true, blockContext = blockContext)
        }
    } else null

    val finallyBlock = if (eat(Token.Identifier.Keyword.Finally)) {
        parseBlock(requireBlock = true, blockContext = blockContext)
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

    val lambda = parseBlock(blockContext = blockContext + BlockContext.Function) as OpBlock

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
    args: List<Expression>? = null,
    blockContext: List<BlockContext>
) : JSFunction {

    val actualName = name ?: run {
        if (nextIsInstance<Token.Identifier.Property>()) {
            (nextSignificant() as Token.Identifier.Property).name
        } else {
            ""
        }
    }

    val nArgs = if (args != null) {
        args
    } else {
        val touple = parseStatement()
        syntaxCheck (touple is OpTouple) {
            "Invalid function declaration"
        }
        touple.expressions
    }.map(Expression::toFunctionParam)

    val block = parseBlock(
        scoped = false,
        blockContext = blockContext + BlockContext.Function
    )

    return JSFunction(
        name = actualName,
        parameters = nArgs,
        body = block,
        isClassMember =  args != null
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

private fun ListIterator<Token>.parseObject(
    extraFields: Map<String, Expression> = emptyMap()
): Expression {
    val props = buildMap {
        while (!nextIsInstance<Token.Operator.Bracket.CurlyClose>()) {
            val variableName = when(val name = parseFactor(emptyList())){
                is OpGetProperty -> name.name
                is OpConstant -> name.value.toString()
                else -> throw SyntaxError("Invalid object declaration")
            }

            syntaxCheck(nextSignificant() is Token.Operator.Colon) {
                "Invalid syntax"
            }

            this[variableName] = parseStatement()
            when {
                nextIsInstance<Token.Operator.Comma>() -> nextSignificant()
                nextIsInstance<Token.Operator.Bracket.CurlyClose>() -> {}
                else -> throw SyntaxError("Invalid object declaration")
            }
        }
        check(nextSignificant() is Token.Operator.Bracket.CurlyClose){
            "} was expected"
        }
    } + extraFields

    return Expression { r ->
        Object("") {
            props.forEach { (k,v) ->
                k eq v.invoke(r)
            }
        }
    }
}

private fun ListIterator<Token>.parseBlock(
    scoped: Boolean = true,
    requireBlock: Boolean = false,
    isExpressible : Boolean = false,
    blockContext: List<BlockContext>,
    static : MutableList<StaticClassMember>? = null,
): Expression {
    var funcIndex = 0

    var isSurroundedWithBraces = false
    val list = buildList {
        if (eat(Token.Operator.Bracket.CurlyOpen)) {
            isSurroundedWithBraces = true
            while (!nextIsInstance<Token.Operator.Bracket.CurlyClose>()) {

                val expr = parseStatement(blockContext, isExpressionStart = true)

                if (isEmpty() && expr is OpGetProperty && eat(Token.Operator.Colon)) {
                    val fields = mapOf(expr.name to parseStatement())
                    eat(Token.Operator.Comma)
                    return parseObject(fields)
                }

                when {
                    expr is OpAssign && expr.isStatic -> {
                        static?.add(StaticClassMember.Variable(expr.variableName, expr.assignableValue))
                    }
                    expr is OpConstant && expr.value is JSFunction && expr.value.isStaticClassMember -> {
                        static?.add(StaticClassMember.Method(expr.value))
                    }
                    expr is OpConstant && (expr.value is JSFunction && !expr.value.isClassMember || expr.value is JSClass) -> {
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
                        if (expr.value is JSClass) {
                            expr.value.static.forEach { s ->
                                add(
                                    index = funcIndex++,
                                    element = Expression {
                                        s.assignTo(expr.value, it)
                                    }
                                )
                            }
                        }
                    }
                    else -> add(expr)
                }

                while (hasNext()) {
                    val next = next()
                    if (next !is Token.NewLine && next !is Token.Operator.SemiColon && next !is Token.Operator.Comma){
                        previous()
                        break
                    }
                }
            }
            check(nextSignificant() is Token.Operator.Bracket.CurlyClose){
                "} was expected"
            }

        } else {
            if (requireBlock) {
                throw SyntaxError("Block start was expected")
            }
            while (eat(Token.Operator.New)){
                //skip
            }
            do {
                add(parseStatement(blockContext))
            } while (eat(Token.Operator.Comma))
        }
    }
    return OpBlock(
        expressions = list,
        isScoped = scoped,
        isExpressible = isExpressible,
        isSurroundedWithBraces = isSurroundedWithBraces
    )
}

private fun ListIterator<Token>.parseVariable(type: VariableType) : Expression {
    val expressions = buildList<Expression> {
        do {
            val variable = when (val expr = parseStatement()) {
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
public inline fun syntaxCheck(value: Boolean, lazyMessage: () -> Any) {
    contract {
        returns() implies value
    }

    if (!value) {
        val message = lazyMessage()
        throw SyntaxError(message.toString())
    }
}

@OptIn(ExperimentalContracts::class)
public inline fun typeCheck(value: Boolean, lazyMessage: () -> Any) {
    contract {
        returns() implies value
    }

    if (!value) {
        val message = lazyMessage()
        throw TypeError(message.toString())
    }
}


