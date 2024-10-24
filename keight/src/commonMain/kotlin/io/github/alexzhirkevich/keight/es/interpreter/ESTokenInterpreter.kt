package io.github.alexzhirkevich.keight.es.interpreter

import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.InterpretationContext
import io.github.alexzhirkevich.keight.LangContext
import io.github.alexzhirkevich.keight.Script
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.common.Callable
import io.github.alexzhirkevich.keight.common.Delegate
import io.github.alexzhirkevich.keight.common.Function
import io.github.alexzhirkevich.keight.common.FunctionParam
import io.github.alexzhirkevich.keight.common.Named
import io.github.alexzhirkevich.keight.common.OpAssign
import io.github.alexzhirkevich.keight.common.OpAssignByIndex
import io.github.alexzhirkevich.keight.common.OpBlock
import io.github.alexzhirkevich.keight.common.OpBreak
import io.github.alexzhirkevich.keight.common.OpCase
import io.github.alexzhirkevich.keight.common.OpCompare
import io.github.alexzhirkevich.keight.common.OpConstant
import io.github.alexzhirkevich.keight.common.OpContinue
import io.github.alexzhirkevich.keight.common.OpEquals
import io.github.alexzhirkevich.keight.common.OpEqualsComparator
import io.github.alexzhirkevich.keight.common.OpExec
import io.github.alexzhirkevich.keight.common.OpExp
import io.github.alexzhirkevich.keight.common.OpForLoop
import io.github.alexzhirkevich.keight.common.OpGetVariable
import io.github.alexzhirkevich.keight.common.OpGreaterComparator
import io.github.alexzhirkevich.keight.common.OpIfCondition
import io.github.alexzhirkevich.keight.common.OpIncDecAssign
import io.github.alexzhirkevich.keight.common.OpIndex
import io.github.alexzhirkevich.keight.common.OpLessComparator
import io.github.alexzhirkevich.keight.common.OpLongInt
import io.github.alexzhirkevich.keight.common.OpLongLong
import io.github.alexzhirkevich.keight.common.OpMakeArray
import io.github.alexzhirkevich.keight.common.OpNot
import io.github.alexzhirkevich.keight.common.OpNotEquals
import io.github.alexzhirkevich.keight.common.OpReturn
import io.github.alexzhirkevich.keight.common.OpSwitch
import io.github.alexzhirkevich.keight.common.OpTouple
import io.github.alexzhirkevich.keight.es.BlockContext
import io.github.alexzhirkevich.keight.es.ESAny
import io.github.alexzhirkevich.keight.es.ESClass
import io.github.alexzhirkevich.keight.es.ESError
import io.github.alexzhirkevich.keight.es.EXPR_DEBUG_PRINT_ENABLED
import io.github.alexzhirkevich.keight.es.Object
import io.github.alexzhirkevich.keight.es.StaticClassMember
import io.github.alexzhirkevich.keight.es.SyntaxError
import io.github.alexzhirkevich.keight.es.syntaxCheck
import io.github.alexzhirkevich.keight.invoke
import io.github.alexzhirkevich.keight.isAssignable
import kotlin.math.pow

private fun ListIterator<Token>.skipAll(token: Token) {
    while (eat(Token.NewLine)){ }
}


private inline fun ListIterator<Token>.eat(token: Token) : Boolean {
    if (nextSignificant() == token){
        return true
    } else {
        prevSignificant()
        return false
    }
}

private fun <T> ListIterator<T>.nextIs(condition : (T) -> Boolean) : Boolean {
    if (!hasNext())
        return false

    return condition(next()).also { previous() }
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


internal class ESTokenInterpreter(
    script : String,
    private val langContext: LangContext,
    private val globalContext : InterpretationContext,
) {
    private val tokens = "{$script}".toList().listIterator().tokens()
        .filterNot { it is Token.Comment || it is Token.Whitespace }
        .listIterator()

    fun interpret(): Script {
        val block = parseBlock(scoped = false, blockContext = emptyList())
        return object : Script {
            override fun invoke(runtime: ScriptRuntime): Any? {
                return try {
                    langContext.toKotlin(block(runtime))
                } catch (t: Throwable) {
                    if (t is ESAny) {
                        throw t
                    } else {
                        throw ESError(t.message, t)
                    }
                }
            }
        }
    }

    private fun parseStatement(
        blockContext: List<BlockContext> = emptyList(),
        unaryOnly: Boolean = false,
        isExpressionStart: Boolean = false,
        variableName: String? = null
    ): Expression {
        return  if (variableName == null) {
            if (unaryOnly) {
                parseFactor(blockContext, isExpressionStart)
            } else {
                parseOperator(blockContext = blockContext, isExpressionStart = isExpressionStart)
            }
        } else {
            OpGetVariable(variableName, receiver = null)
        }
    }

    private fun parseAssignmentValue(
        x: Expression,
        merge: ((Any?, Any?) -> Any?)? = null
    ): Expression {
        return when {
            x is OpIndex && x.variable is OpGetVariable -> OpAssignByIndex(
                variableName = x.variable.name,
                scope = x.variable.assignmentType,
                index = x.index,
                assignableValue = parseStatement(emptyList()),
                merge = merge
            )

            x is OpGetVariable -> OpAssign(
                variableName = x.name,
                receiver = x.receiver,
                assignableValue = parseStatement(emptyList()),
                type = x.assignmentType,
                merge = merge
            )

            else -> throw SyntaxError("Invalid assignment")
        }
    }

    private fun parseOperator(
        blockContext: List<BlockContext> = emptyList(),
        isExpressionStart: Boolean = false,
        priority: Int = 15
    ): Expression {
        var x = if (priority == 0) {
            parseFactor(blockContext, isExpressionStart)
        } else {
            parseOperator(blockContext, isExpressionStart, priority - 1)
        }

        while (true) {
            x = when (priority) {
                0 -> when (val it = tokens.nextSignificant()) {
                    Token.Operator.Dot,
                    Token.Operator.Bracket.SquareOpen -> {
                        parseMemberOf(
                            receiver = x,
                            isDot = it is Token.Operator.Dot
                        )
                    }

                    Token.Operator.Bracket.RoundOpen -> parseFunctionCall(
                        receiver = x,
                    )
                    else -> return x.also { tokens.prevSignificant() }
                }
                1 -> when (val it = tokens.nextSignificant()) {
                    is Token.Operator.Arithmetic.Inc,
                    is Token.Operator.Arithmetic.Dec ->{
                        syntaxCheck(x.isAssignable()) {
                            "Value is not assignable"
                        }
                        OpIncDecAssign(
                            variable = x,
                            isPrefix = false,
                            op = if (it is Token.Operator.Arithmetic.Inc)
                                langContext::inc else langContext::dec
                        )
                    }
                    else -> return x.also { tokens.prevSignificant() }
                }

                2 -> when (tokens.nextSignificant()) {
                    Token.Operator.Arithmetic.Exp -> {
                        OpExp(
                            x = x,
                            degree = parseOperator(
                                blockContext, isExpressionStart, priority
                            )
                        )
                    }
                    else -> return x.also { tokens.prevSignificant() }
                }
                3 -> when (tokens.nextSignificant()) {
                    Token.Operator.Arithmetic.Mul -> Delegate(
                        x,
                        parseOperator(blockContext, isExpressionStart, priority-1),
                        langContext::mul
                    )
                    Token.Operator.Arithmetic.Div -> Delegate(
                        x,
                        parseOperator(blockContext, isExpressionStart, priority-1),
                        langContext::div
                    )
                    Token.Operator.Arithmetic.Mod -> Delegate(
                        x,
                        parseOperator(blockContext, isExpressionStart, priority-1),
                        langContext::mod
                    )
                    else -> return x.also { tokens.prevSignificant() }
                }
                4 -> when (tokens.nextSignificant()) {
                    Token.Operator.Arithmetic.Plus -> Delegate(
                        x,
                        parseOperator(blockContext, isExpressionStart, priority-1),
                        langContext::sum
                    )
                    Token.Operator.Arithmetic.Minus -> Delegate(
                        x,
                        parseOperator(blockContext, isExpressionStart, priority-1),
                        langContext::sub
                    )

                    else -> return x.also { tokens.prevSignificant() }
                }
                5 ->  when (tokens.nextSignificant()) {
                    Token.Operator.Bitwise.Shl -> OpLongInt(
                        x,
                        parseOperator(blockContext, isExpressionStart, priority-1),
                        Long::shl
                    )

                    Token.Operator.Bitwise.Shr -> OpLongInt(
                        x,
                        parseOperator(blockContext, isExpressionStart, priority-1),
                        Long::shr
                    )
                    Token.Operator.Bitwise.Ushr -> OpLongInt(
                        x,
                        parseOperator(blockContext, isExpressionStart, priority-1),
                        Long::ushr
                    )
                    else -> return x.also { tokens.prevSignificant() }

                }
                6 ->  when (tokens.nextSignificant()) {
                    Token.Operator.In -> parseInKeyword(x)
                    Token.Operator.Instanceof -> TODO()
                    else -> return x.also { tokens.prevSignificant() }
                }
                7 ->  when (tokens.nextSignificant()) {
                    Token.Operator.Compare.Less -> OpCompare(
                        x,
                        parseOperator(blockContext, isExpressionStart, priority),
                        OpLessComparator
                    )
                    Token.Operator.Compare.LessOrEq -> OpCompare(
                        x, parseOperator(blockContext, isExpressionStart, priority)
                    ) { a, b, r ->
                        OpLessComparator(a, b, r) || OpEqualsComparator(a, b, r)
                    }
                    Token.Operator.Compare.Greater ->  OpCompare(
                        x,
                        parseOperator(blockContext, isExpressionStart, priority),
                        OpGreaterComparator
                    )
                    Token.Operator.Compare.GreaterOrEq -> OpCompare(
                        x,
                        parseOperator(blockContext, isExpressionStart, priority)
                    ) { a, b,r  ->
                        OpGreaterComparator(a, b, r) || OpEqualsComparator(a, b,r)
                    }

                    else -> return x.also { tokens.prevSignificant() }
                }
                8 -> when (tokens.nextSignificant()) {
                    Token.Operator.Compare.Eq -> OpEquals(
                        x, parseOperator(blockContext, isExpressionStart, priority), false
                    )
                    Token.Operator.Compare.StrictEq -> OpEquals(
                        x, parseOperator(blockContext, isExpressionStart, priority), true
                    )
                    Token.Operator.Compare.Neq -> OpNotEquals(
                        x, parseOperator(blockContext, isExpressionStart, priority), false
                    )
                    Token.Operator.Compare.StrictNeq -> OpNotEquals(
                        x, parseOperator(blockContext, isExpressionStart, priority), true
                    )

                    else -> return x.also { tokens.prevSignificant() }
                }
                9 -> when (tokens.nextSignificant()) {
                    Token.Operator.Bitwise.And -> OpLongLong(
                        x,
                        parseOperator(blockContext, isExpressionStart, priority),
                        Long::and
                    )
                    else -> return x.also { tokens.prevSignificant() }
                }
                10 -> when (tokens.nextSignificant()) {
                    Token.Operator.Bitwise.Xor -> OpLongLong(
                        x,
                        parseOperator(blockContext, isExpressionStart, priority),
                        Long::xor
                    )
                    else -> return x.also { tokens.prevSignificant() }
                }
                11 -> when (tokens.nextSignificant()) {
                    Token.Operator.Bitwise.Or -> OpLongLong(
                        x,
                        parseOperator(blockContext, isExpressionStart, priority),
                        Long::or
                    )
                    else -> return x.also { tokens.prevSignificant() }
                }
                12 -> when (tokens.nextSignificant()) {
                    Token.Operator.Logical.And -> {
                        val a = x
                        val b = parseOperator(blockContext, isExpressionStart, priority)
                        Expression {
                            !langContext.isFalse(a(it)) && !langContext.isFalse(b(it))
                        }
                    }
                    else -> return x.also { tokens.prevSignificant() }
                }

                13 ->  when (tokens.nextSignificant()) {
                    Token.Operator.Logical.Or -> {
                        val a = x
                        val b = parseOperator(blockContext, isExpressionStart, priority)
                        Expression {
                            !langContext.isFalse(a(it)) || !langContext.isFalse(b(it))
                        }
                    }
                    else -> return x.also { tokens.prevSignificant() }
                }

                14 -> when (tokens.nextSignificant()) {
                    Token.Operator.QuestionMark -> parseTernary(
                        condition = x,
                        blockContext = blockContext
                    )
                    Token.Operator.Arrow -> OpConstant(parseArrowFunction(blockContext, x))

                    else -> return x.also { tokens.prevSignificant() }
                }

                15 -> when (val next = tokens.nextSignificant()) {
                    is Token.Operator.Assign -> parseAssignmentValue(
                        x,
                        getMergeForAssignment(next)
                    )

                    else -> return x.also { tokens.prevSignificant() }
                }
                else -> {
                    error("Invalid operator priority - $priority")
                }
            }
        }
    }

    private fun parseFactor(
        blockContext: List<BlockContext>,
        isExpressionStart: Boolean = false,
        allowContinueWithContext: Boolean = true
    ): Expression {
        return when (val next = tokens.nextSignificant()) {
            is Token.Str -> OpConstant(langContext.fromKotlin(next.value))

            is Token.Operator.Dot, is Token.Num -> {
                val num = if (next is Token.Num) {
                    next.value
                } else {
                    val number = tokens.next()
                    syntaxCheck(number is Token.Num && !number.isFloat) {
                        unexpected(".")
                    }
                    "0.${number.value}".toFloat()
                }
                OpConstant(num)
            }

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
                    op = if (isInc) langContext::inc else langContext::dec
                )
            }

            Token.Operator.Arithmetic.Plus,
            Token.Operator.Arithmetic.Minus -> Delegate(
                a = parseFactor(
                    blockContext = blockContext
                ),
                op = if (next is Token.Operator.Arithmetic.Plus)
                    langContext::pos else langContext::neg
            )

            Token.Operator.Logical.Not -> OpNot(
                condition = parseStatement(
                    blockContext = blockContext
                ),
                isFalse = langContext::isFalse
            )

            Token.Operator.Bitwise.Reverse -> {
                val expr = parseStatement(
                    blockContext = blockContext
                )
                Expression {
                    langContext.toNumber(expr(it)).toLong().inv()
                }
            }

            Token.Operator.Bracket.CurlyOpen -> {
                if (isExpressionStart) {
                    parseBlock(blockContext = blockContext)
                } else {
                    parseObject()
                }
            }

            Token.Operator.Bracket.RoundOpen -> parseExpressionGrouping()
            Token.Operator.Bracket.SquareOpen -> parseArrayCreation()

            is Token.Keyword -> parseKeyword(next, blockContext)
            is Token.Operator.Typeof -> parseTypeof()
            is Token.Property -> OpGetVariable(next.name, receiver = null)

            else -> throw SyntaxError("Unexpected token $next")
        }
    }

    private fun parseKeyword(keyword: Token.Keyword, blockContext: List<BlockContext>): Expression {
        return when(keyword){
            Token.Keyword.Var,
            Token.Keyword.Let,
            Token.Keyword.Const, -> parseVariable(
                when(keyword){
                    Token.Keyword.Var -> VariableType.Global
                    Token.Keyword.Let -> VariableType.Local
                    else -> VariableType.Const
                }
            )
            Token.Keyword.True -> OpConstant(true)
            Token.Keyword.False -> OpConstant(false)
            Token.Keyword.Null -> OpConstant(null)
            Token.Keyword.Break -> OpBreak
            Token.Keyword.Switch -> parseSwitch(blockContext)
            Token.Keyword.Case,
            Token.Keyword.Default -> {
                syntaxCheck(blockContext.last() == BlockContext.Switch) {
                    "Unexpected token 'case'"
                }
                val case = if (keyword is Token.Keyword.Case)
                    parseFactor(emptyList())
                else OpCase.Default

                syntaxCheck(tokens.nextSignificant() is Token.Operator.SemiColon) {
                    "Expected ':' after 'case'"
                }
                OpCase(case)
            }
            Token.Keyword.Catch -> TODO()
            Token.Keyword.Class -> TODO()
            Token.Keyword.Continue -> OpContinue
            Token.Keyword.Do -> TODO()
            Token.Keyword.Else -> TODO()
            Token.Keyword.Finally -> TODO()
            Token.Keyword.For -> parseForLoop(blockContext)
            Token.Keyword.Function -> OpConstant(parseFunction(blockContext = blockContext))
            Token.Keyword.If -> parseIf(blockContext)
            Token.Keyword.New -> TODO()
            Token.Keyword.Return -> OpReturn(parseStatement())
            Token.Keyword.Throw -> TODO()
            Token.Keyword.Try -> TODO()
            Token.Keyword.While -> TODO()
        }
    }

    private fun getMergeForAssignment(operator: Token.Operator.Assign): ((Any?, Any?) -> Any?)? {
        return when (operator) {
            Token.Operator.Assign.Assignment -> null
            Token.Operator.Assign.PlusAssign -> langContext::sum
            Token.Operator.Assign.MinusAssign -> langContext::sub
            Token.Operator.Assign.MulAssign -> langContext::mul
            Token.Operator.Assign.DivAssign -> langContext::div
            Token.Operator.Assign.ExpAssign -> { a, b ->
                langContext.toNumber(a).toDouble().pow(langContext.toNumber(b).toDouble())
            }

            Token.Operator.Assign.ModAssign -> { a, b ->
                langContext.toNumber(a).toLong() and langContext.toNumber(b).toLong()
            }

            Token.Operator.Assign.BitAndAssign -> { a, b ->
                langContext.toNumber(a).toLong() and langContext.toNumber(b).toLong()
            }

            Token.Operator.Assign.LogicAndAssign -> { a, b ->
                if (langContext.isFalse(a)) a else b
            }

            Token.Operator.Assign.BitOrAssign -> { a, b ->
                langContext.toNumber(a).toLong() or langContext.toNumber(b).toLong()
            }

            Token.Operator.Assign.LogicOrAssign -> { a, b ->
                if (langContext.isFalse(a)) b else a
            }

            Token.Operator.Assign.BitXorAssign -> { a, b ->
                langContext.toNumber(a).toLong() xor langContext.toNumber(b).toLong()
            }

            Token.Operator.Assign.UshrAssign -> { a, b ->
                langContext.toNumber(a).toLong() ushr langContext.toNumber(b).toInt()
            }

            Token.Operator.Assign.ShrAssign -> { a, b ->
                langContext.toNumber(a).toLong() shr langContext.toNumber(b).toInt()
            }

            Token.Operator.Assign.ShlAssign -> { a, b ->
                langContext.toNumber(a).toLong() shl langContext.toNumber(b).toInt()
            }
        }
    }

    private fun parseTypeof() : Expression {
        val isArg = tokens.nextSignificant() is Token.Operator.Bracket.RoundOpen
        if (!isArg){
            tokens.prevSignificant()
        }
        val expr = parseStatement(unaryOnly = true)

        if (isArg) {
            syntaxCheck(tokens.nextSignificant() is Token.Operator.Bracket.RoundClose) {
                "Missing )"
            }
        }
        return Expression {
            when (val v = expr(it)) {
                null -> "object"
                Unit -> "undefined"
                true, false -> "boolean"

                is ESAny -> v.type
                is Callable -> "function"
                else -> v::class.simpleName
            }
        }
    }

    private fun parseArrayCreation(): Expression {
        val expressions = buildList {
            if (tokens.nextIsInstance<Token.Operator.Bracket.SquareClose>()) {
                return@buildList
            }
            do {
                add(parseStatement())
            } while (tokens.nextSignificant() is Token.Operator.Comma)
            tokens.prevSignificant()
        }
        syntaxCheck(tokens.nextSignificant() is Token.Operator.Bracket.SquareClose) {
            "Expected ')'"
        }

        return OpMakeArray(expressions)
    }

    private fun parseExpressionGrouping(): OpTouple {
        val expressions = buildList {
            if (tokens.nextIsInstance<Token.Operator.Bracket.RoundClose>()) {
                return@buildList
            }

            do {
                add(parseStatement(emptyList()))
            } while (tokens.nextSignificant() is Token.Operator.Comma)
            tokens.prevSignificant()
        }
        syntaxCheck(tokens.nextSignificant() is Token.Operator.Bracket.RoundClose) {
            "Expected ')'"
        }

        return OpTouple(expressions)
    }

    private fun parseMemberOf(receiver: Expression, isDot: Boolean): Expression {
        return if (isDot) {
            val memberName = parseFactor(emptyList())
            check(memberName is OpGetVariable)
            OpGetVariable(name = memberName.name, receiver = receiver)
        } else {
            OpIndex(
                variable = receiver,
                index = parseStatement()
            ).also {
                syntaxCheck(tokens.nextSignificant() is Token.Operator.Bracket.SquareClose) {
                    "Missing ']'"
                }
            }
        }
    }

    private fun parseFunctionCall(receiver: Expression): Expression {
        tokens.previous()
        val args = parseStatement() as OpTouple
        return OpExec(receiver, args.expressions)
    }

    private fun parseInKeyword(subject : Expression) : Expression {
        val obj = parseStatement()
        return Expression {
            val o = obj(it)
            syntaxCheck(o is ESAny) {
                "Illegal usage of 'in' operator"
            }
            o.contains(subject(it))
        }
    }

    private fun parseTernary(
        condition : Expression,
        blockContext: List<BlockContext>
    ) : Expression {
        val bContext = blockContext.dropLastWhile { it == BlockContext.Class }
        val onTrue = parseStatement(bContext)

        syntaxCheck(tokens.nextSignificant() is Token.Operator.Colon) {
            "Unexpected end of input"
        }
        val onFalse = parseStatement(bContext)

        return OpIfCondition(
            condition = condition,
            onTrue = onTrue,
            onFalse = onFalse,
            expressible = true
        )
    }

    private fun parseSwitch(blockContext: List<BlockContext>) : Expression {
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

    private fun parseIf(blockContext: List<BlockContext>) : Expression {

        val condition = (parseStatement() as OpTouple).expressions.single()

        val onTrue = parseBlock(blockContext = blockContext)

        val onFalse = if (tokens.eat(Token.Keyword.Else)) {
            parseBlock(blockContext = blockContext)
        } else null


        return OpIfCondition(
            condition = condition,
            onTrue = onTrue,
            onFalse = onFalse
        )
    }

    private fun parseForLoop(parentBlockContext: List<BlockContext>): Expression {

        syntaxCheck(tokens.nextSignificant() is Token.Operator.Bracket.RoundOpen) {
            "Invalid for loop"
        }

        val assign = if (tokens.eat(Token.Operator.SemiColon))
            null else parseStatement()

        syntaxCheck(assign is OpAssign?) {
            "Invalid for loop"
        }

        if (assign != null) {
            syntaxCheck(tokens.nextSignificant() is Token.Operator.SemiColon) {
                "Invalid for loop"
            }
        }
        val comparison = if (tokens.eat(Token.Operator.SemiColon))
            null else parseStatement()

        if (comparison != null) {
            syntaxCheck(tokens.nextSignificant() is Token.Operator.SemiColon) {
                "Invalid for loop"
            }
        }

        val increment = if (tokens.eat(Token.Operator.Bracket.RoundClose))
            null else parseStatement()

        if (increment != null) {
            syntaxCheck(tokens.nextSignificant() is Token.Operator.Bracket.RoundClose) {
                "Invalid for loop"
            }
        }

        val body = parseBlock(blockContext = parentBlockContext + BlockContext.Loop)

        return OpForLoop(
            assignment = assign,
            increment = increment,
            comparison = comparison,
            isFalse = langContext::isFalse,
            body = body
        )
    }


    private fun parseArrowFunction(blockContext: List<BlockContext>, args: Expression) : Function {
        val fArgs = when(args){
            is OpTouple -> args.expressions
            else -> listOf(args)
        }.filterIsInstance<OpGetVariable>()

        val lambda = parseBlock(blockContext = blockContext + BlockContext.Function)

        return Function(
            "",
            fArgs.map { FunctionParam(it.name) },
            body = lambda
        )
    }

    private fun parseFunction(
        name: String? = null,
        args: List<Expression>? = null,
        blockContext: List<BlockContext>
    ) : Function {

        val actualName = name ?: run {
            if (tokens.nextIsInstance<Token.Property>()) {
                (tokens.nextSignificant() as Token.Property).name
            } else {
                ""
            }
        }

        val nArgs = (args ?: (parseStatement() as OpTouple).expressions).let { a ->
            a.map {
                when (it) {
                    is OpGetVariable -> FunctionParam(name = it.name, default = null)
                    is OpAssign -> FunctionParam(
                        name = it.variableName,
                        default = it.assignableValue
                    )

                    else -> throw SyntaxError("Invalid function declaration ($name)")
                }
            }
        }


        val block = parseBlock(
            scoped = false,
            blockContext = blockContext + BlockContext.Function
        )

        return Function(
            name = actualName,
            parameters = nArgs,
            body = block,
            isClassMember =  args != null
        )
    }

    private fun parseObject(
        extraFields: Map<String, Expression> = emptyMap()
    ): Expression {
        val props = buildMap {
            while (!tokens.nextIsInstance<Token.Operator.Bracket.CurlyClose>()) {
                val variableName = when(val name = parseFactor(emptyList())){
                    is OpGetVariable -> name.name
                    is OpConstant -> name.value.toString()
                    else -> throw SyntaxError("Invalid object declaration")
                }

                syntaxCheck(tokens.nextSignificant() is Token.Operator.Colon) {
                    "Invalid syntax"
                }

                this[variableName] = parseStatement()
                when {
                    tokens.nextIsInstance<Token.Operator.Comma>() -> tokens.nextSignificant()
                    tokens.nextIsInstance<Token.Operator.Bracket.CurlyClose>() -> {}
                    else -> throw SyntaxError("Invalid object declaration")
                }
            }
            check(tokens.nextSignificant() is Token.Operator.Bracket.CurlyClose){
                "} was expected"
            }
        } + extraFields

        return Expression { r ->
            Object("") {
                props.forEach {
                    it.key eq it.value.invoke(r)
                }
            }
        }
    }

    private fun parseBlock(
        scoped: Boolean = true,
        requireBlock: Boolean = false,
        blockContext: List<BlockContext>,
        static : MutableList<StaticClassMember>? = null,
    ): Expression {
        var funcIndex = 0
        val list = buildList {
            if (tokens.eat(Token.Operator.Bracket.CurlyOpen)) {
                while (!tokens.nextIsInstance<Token.Operator.Bracket.CurlyClose>()) {

                    val expr = parseStatement(blockContext, isExpressionStart = true)

                    if (size == 0 && expr is OpGetVariable && tokens.nextIsInstance<Token.Operator.Colon>()) {
                        return parseObject(
                            mapOf(expr.name to parseStatement())
                        )
                    }

                    when {
                        expr is OpAssign && expr.isStatic -> {
                            static?.add(StaticClassMember.Variable(expr.variableName, expr.assignableValue))
                        }
                        expr is OpConstant && expr.value is Function && expr.value.isStatic -> {
                            static?.add(StaticClassMember.Method(expr.value))
                        }
                        expr is OpConstant && (expr.value is Function && !expr.value.isClassMember || expr.value is ESClass) -> {
                            val name = (expr.value as Named).name

                            if (EXPR_DEBUG_PRINT_ENABLED){
                                println("registering '$name' as class or top level function")
                            }
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
                            if (expr.value is ESClass) {
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
                    if (tokens.hasNext() && !tokens.nextIsInstance<Token.Operator.Bracket.CurlyClose>()){
                        syntaxCheck(skipStatementSeparators()){
                            "Unexpected identifier ${tokens.nextSignificant()}"
                        }
                    }
                }
                check(tokens.nextSignificant() is Token.Operator.Bracket.CurlyClose){
                    "} was expected"
                }

            } else {
                if (requireBlock) {
                    throw SyntaxError("Unexpected token: block start was expected")
                }
                tokens.skipAll(Token.NewLine)
                add(parseStatement(blockContext))
            }
        }
        return OpBlock(list, scoped)
    }

    private fun skipStatementSeparators() : Boolean {
        var skipped = false
        while (tokens.hasNext()) {
            val next = tokens.next()
            if (next !is Token.NewLine && next !is Token.Operator.SemiColon){
                tokens.previous()
                break
            }
            skipped = true
        }
        return skipped
    }

    private fun parseVariable(type: VariableType) : Expression {

        return when (val expr = parseStatement()) {
            is OpAssign -> {
                OpAssign(
                    type = type,
                    variableName = expr.variableName,
                    assignableValue = expr.assignableValue,
                    merge = null
                )
            }

            is OpGetVariable -> {
                OpAssign(
                    type = type,
                    variableName = expr.name,
                    assignableValue = OpConstant(Unit),
                    merge = null
                )
            }

            else -> throw SyntaxError("Unexpected identifier $expr")
        }
    }
}

private fun unexpected(expr : String) : String = "Unexpected '$expr'"

