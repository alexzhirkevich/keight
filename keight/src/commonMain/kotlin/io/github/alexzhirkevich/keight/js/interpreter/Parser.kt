package io.github.alexzhirkevich.keight.js.interpreter

import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.CallFrame
import io.github.alexzhirkevich.keight.Constructor
import io.github.alexzhirkevich.keight.Delegate
import io.github.alexzhirkevich.keight.Expression
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.VariableType
import io.github.alexzhirkevich.keight.js.Constants
import io.github.alexzhirkevich.keight.js.JSFunction
import io.github.alexzhirkevich.keight.js.JsNumberWrapper
import io.github.alexzhirkevich.keight.expressions.OpAssign
import io.github.alexzhirkevich.keight.expressions.OpAssignByIndex
import io.github.alexzhirkevich.keight.expressions.OpBlock
import io.github.alexzhirkevich.keight.expressions.OpBreak
import io.github.alexzhirkevich.keight.expressions.OpCall
import io.github.alexzhirkevich.keight.expressions.OpCase
import io.github.alexzhirkevich.keight.expressions.OpCompare
import io.github.alexzhirkevich.keight.expressions.OpConstant
import io.github.alexzhirkevich.keight.expressions.OpContinue
import io.github.alexzhirkevich.keight.expressions.OpDestructAssign
import io.github.alexzhirkevich.keight.expressions.OpDoWhileLoop
import io.github.alexzhirkevich.keight.expressions.OpEquals
import io.github.alexzhirkevich.keight.expressions.OpExp
import io.github.alexzhirkevich.keight.expressions.OpForInLoop
import io.github.alexzhirkevich.keight.expressions.OpForLoop
import io.github.alexzhirkevich.keight.expressions.OpForOfLoop
import io.github.alexzhirkevich.keight.expressions.OpGetProperty
import io.github.alexzhirkevich.keight.expressions.OpGetter
import io.github.alexzhirkevich.keight.expressions.OpIfCondition
import io.github.alexzhirkevich.keight.expressions.OpIn
import io.github.alexzhirkevich.keight.expressions.OpIncDecAssign
import io.github.alexzhirkevich.keight.expressions.OpIndex
import io.github.alexzhirkevich.keight.expressions.OpColonAssignment
import io.github.alexzhirkevich.keight.expressions.OpComputedProperty
import io.github.alexzhirkevich.keight.expressions.OpComputedPropertyMethod
import io.github.alexzhirkevich.keight.expressions.OpComputedPropertyName
import io.github.alexzhirkevich.keight.expressions.OpLongInt
import io.github.alexzhirkevich.keight.expressions.OpLongLong
import io.github.alexzhirkevich.keight.expressions.OpMake
import io.github.alexzhirkevich.keight.expressions.OpMakeArray
import io.github.alexzhirkevich.keight.expressions.OpMakeObject
import io.github.alexzhirkevich.keight.expressions.OpNot
import io.github.alexzhirkevich.keight.expressions.OpNotEquals
import io.github.alexzhirkevich.keight.expressions.OpReturn
import io.github.alexzhirkevich.keight.expressions.OpSetter
import io.github.alexzhirkevich.keight.expressions.OpSpread
import io.github.alexzhirkevich.keight.expressions.OpSuperGetProperty
import io.github.alexzhirkevich.keight.expressions.OpSuperGetPropertyComputed
import io.github.alexzhirkevich.keight.expressions.OpSwitch
import io.github.alexzhirkevich.keight.expressions.OpTouple
import io.github.alexzhirkevich.keight.expressions.OpTryCatch
import io.github.alexzhirkevich.keight.expressions.OpWhileLoop
import io.github.alexzhirkevich.keight.expressions.PropertyAccessorFactory
import io.github.alexzhirkevich.keight.expressions.ThrowableValue
import io.github.alexzhirkevich.keight.expressions.asDestruction
import io.github.alexzhirkevich.keight.fastAll
import io.github.alexzhirkevich.keight.fastMap
import io.github.alexzhirkevich.keight.findJsRoot
import io.github.alexzhirkevich.keight.js.JSError
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.OpClassInit
import io.github.alexzhirkevich.keight.js.OpFunctionInit
import io.github.alexzhirkevich.keight.js.ReferenceError
import io.github.alexzhirkevich.keight.js.StaticClassMember
import io.github.alexzhirkevich.keight.js.SyntaxError
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.Uninitialized
import io.github.alexzhirkevich.keight.expressions.AggregatingExportEntry
import io.github.alexzhirkevich.keight.expressions.ImportEntry
import io.github.alexzhirkevich.keight.expressions.OpAggregatingExport
import io.github.alexzhirkevich.keight.expressions.OpExport
import io.github.alexzhirkevich.keight.expressions.OpImport
import io.github.alexzhirkevich.keight.js.joinSuccess
import io.github.alexzhirkevich.keight.js.js
import io.github.alexzhirkevich.keight.js.listOf
import io.github.alexzhirkevich.keight.js.toFunctionParam
import io.github.alexzhirkevich.keight.js.toJsRegex
import io.github.alexzhirkevich.keight.js.validateFunctionParams
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlin.collections.List
import kotlin.collections.ListIterator
import kotlin.collections.associateBy
import kotlin.collections.buildList
import kotlin.collections.drop
import kotlin.collections.emptyList
import kotlin.collections.firstOrNull
import kotlin.collections.fold
import kotlin.collections.joinToString
import kotlin.collections.last
import kotlin.collections.lastOrNull
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.plus
import kotlin.collections.set
import kotlin.collections.single
import kotlin.collections.singleOrNull
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.math.pow
import io.github.alexzhirkevich.keight.SourceLocation

internal fun List<LocatedToken>.parse() : Expression {
    return sanitize()
        .listIterator()
        .parseBlock(
            scoped = false,
            isExpressible = true,
            blockContext = emptyList(),
            type = ExpectedBlockType.Block
        )
}

/**
 * Parse with a script name for error reporting.
 */
internal fun List<LocatedToken>.parse(scriptName: String? = null) : Expression {
    val tokens = if (scriptName != null) {
        sanitize().map { lt ->
            LocatedToken(lt.token, lt.location.copy(fileName = scriptName))
        }
    } else {
        sanitize()
    }
    return tokens
        .listIterator()
        .parseBlock(
            scoped = false,
            isExpressible = true,
            isStatementList = true,
            blockContext = emptyList(),
            type = ExpectedBlockType.Block
        )
}

internal enum class ExpectedBlockType {
    None, Object, Block
}

internal enum class BlockContext {
    None, Loop, Switch, Function, Class, Object, Ternary,
    /**
     * Represents being inside a property value in an object literal.
     * Used to distinguish between computed property names [expr] and array literals [1, 2].
     */
    ObjectPropertyValue
}

private fun unexpected(expr : String) : String = "Unexpected token '$expr'"

private fun List<LocatedToken>.sanitize() : List<LocatedToken> {

    return fold(mutableListOf<LocatedToken>()) { l, lt ->
        val token = lt.token
        // skip comments, double newlines and newlines after semicolon
        if (
            token !is Token.Comment &&
            (token !is Token.NewLine || (l.lastOrNull()?.token !is Token.NewLine && l.lastOrNull()?.token !is Token.Operator.SemiColon))
        ) {
            l.add(lt)
        }
        l
    }
}

/**
 * Sanitize a plain token list (used for template string internal tokens).
 */
private fun List<Token>.sanitizeTokens() : List<Token> {

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

private fun ListIterator<LocatedToken>.eat(token: Token) : Boolean {
    val i = nextIndex()
    if (nextSignificant() == token){
        return true
    } else {
        returnToIndex(i)
        return false
    }
}

private inline fun <reified R : Token> ListIterator<LocatedToken>.nextIsInstance() : Boolean {
    if (!hasNext())
        return false

    val i = nextIndex()

    return (nextSignificant() is R).also { returnToIndex(i) }
}

/**
 * Consume an optional `;` (and surrounding newlines) followed by [kw].
 *
 * Returns `true` and consumes up to and including [kw] when the sequence
 * `;? <kw>` is present; otherwise consumes nothing and restores the stream.
 *
 * Used for `;`-terminated inline statements immediately followed by a
 * continuation keyword: `if (cond) s; else ...`, `do s; while (...)`,
 * `try { } ; catch ...` / `... ; finally ...`. Crucially, if the next token
 * after an optional `;` is NOT [kw], the `;` is left in the stream so the
 * enclosing block can still use it as a statement separator
 * (e.g. `if (a) b; c` must keep running `c`, `do b; c` keeps running `c`).
 *
 * Fixes issue #22 (and its `do/while` + `try/catch/finally` variants): without
 * this, the explicit `;` left after an inline body makes the continuation
 * keyword (`else` / `while` / `catch` / `finally`) get orphaned and later
 * parsed as a standalone statement, raising "Unexpected token" / "Missing ...".
 */
private fun ListIterator<LocatedToken>.eatKeywordAfterOptionalSemicolon(kw: Token.Identifier.Keyword): Boolean {
    val start = nextIndex()
    val first = nextSignificant()
    val found = if (first == Token.Operator.SemiColon) {
        nextSignificant() == kw
    } else {
        first == kw
    }
    if (!found) returnToIndex(start)
    return found
}

private fun ListIterator<LocatedToken>.nextSignificant() : Token {
    if (!hasNext()) return Token.EndOfFile
    var n = next().token
    while (n is Token.NewLine){
        if (!hasNext()) return Token.EndOfFile
        n = next().token
    }
    return n
}

/**
 * Peek the raw next token (including [Token.NewLine]) WITHOUT consuming it.
 * Unlike [nextSignificant], this does NOT skip newlines — required for ASI-sensitive
 * productions such as `return`, where an immediately-following newline terminates the
 * statement and must remain in the stream for the enclosing block to consume as a
 * statement separator (see issue #20).
 */
private fun ListIterator<LocatedToken>.peekRawToken(): Token? {
    if (!hasNext()) return null
    val i = nextIndex()
    val t = next().token
    returnToIndex(i)
    return t
}

/**
 * Get the next significant LocatedToken (skipping newlines), or null if exhausted.
 */
private fun ListIterator<LocatedToken>.nextSignificantLocated() : LocatedToken? {
    if (!hasNext()) return null
    val i = nextIndex()
    var lt = next()
    while (lt.token is Token.NewLine) {
        if (!hasNext()) { returnToIndex(i); return null }
        lt = next()
    }
    return lt
}

private fun ListIterator<LocatedToken>.returnToIndex(idx : Int){
    while (nextIndex() > idx){
        previous()
    }
}

private fun ListIterator<LocatedToken>.prevSignificant() : Token {
    var n = previous().token
    while (n is Token.NewLine){
        n = previous().token
    }
    return n
}

/**
 * Get the location of the next significant token, or null.
 */
private fun ListIterator<LocatedToken>.nextSignificantLocation() : SourceLocation? {
    val i = nextIndex()
    val lt = nextSignificantLocated()
    returnToIndex(i)
    return lt?.location
}

/**
 * Helper to set source location on an expression from a LocatedToken.
 */
private fun Expression.at(location: SourceLocation?) : Expression {
    if (location != null) this.sourceLocation = location
    return this
}

private fun ListIterator<LocatedToken>.parseStatement(
    blockContext: List<BlockContext> = emptyList(),
    maxPrecedence: Int = 14,
    blockType: ExpectedBlockType,
    isBlockAnchor : Boolean = false,
) : Expression {

    var precedence = -1

    var x = parseFactor(blockContext, blockType)

    while (++precedence <= maxPrecedence) {

        if (x is OpConstant && (x.value is JSFunction || x.value is OpClassInit) && isBlockAnchor) {
            return x
        }

        while (true) {

            val i = nextIndex()

            x = when (precedence) {
                0 -> when (nextSignificant()) {
                    is Token.Operator.Bracket.RoundOpen -> {
                        prevSignificant()
                        parseFunctionCall(x, blockContext = blockContext)
                    }

                    is Token.Operator.Period,
                    is Token.Operator.DoublePeriod,
                    is Token.Operator.Bracket.SquareOpen -> {
                        prevSignificant()
                        parseMemberOf(x)
                    }

                    is Token.Operator.OptionalChaining -> parseOptionalChaining(x)
                    else -> { returnToIndex(i); break; }
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

                    else -> { returnToIndex(i); break; }
                }

                2 -> when (nextSignificant()) {
                    Token.Operator.Arithmetic.Exp -> OpExp(
                        x = x,
                        degree = parseStatement(
                            blockContext,
                            precedence,
                            ExpectedBlockType.Object
                        )
                    )

                    else -> { returnToIndex(i); break; }
                }

                3 -> when (nextSignificant()) {

                    Token.Operator.Arithmetic.Mul -> Delegate(
                        a = x,
                        b = parseStatement(
                            blockContext,
                            precedence - 1,
                            ExpectedBlockType.Object
                        ),
                        op = ScriptRuntime::mul
                    )

                    Token.Operator.Arithmetic.Div -> Delegate(
                        a = x,
                        b = parseStatement(
                            blockContext,
                            precedence - 1,
                            ExpectedBlockType.Object
                        ),
                        op = ScriptRuntime::div
                    )

                    Token.Operator.Arithmetic.Mod -> Delegate(
                        a = x,
                        b = parseStatement(
                            blockContext,
                            precedence - 1,
                            ExpectedBlockType.Object
                        ),
                        op = ScriptRuntime::mod
                    )

                    else -> { returnToIndex(i); break; }
                }

                4 -> when (nextSignificant()) {
                    Token.Operator.Arithmetic.Plus -> Delegate(
                        a = x,
                        b = parseStatement(
                            blockContext,
                            precedence - 1,
                            ExpectedBlockType.Object
                        ),
                        op = ScriptRuntime::sum
                    )

                    Token.Operator.Arithmetic.Minus -> Delegate(
                        a = x,
                        b = parseStatement(
                            blockContext,
                            precedence - 1,
                            ExpectedBlockType.Object
                        ),
                        op = ScriptRuntime::sub
                    )

                    else -> { returnToIndex(i); break; }
                }

                5 -> when (nextSignificant()) {
                    Token.Operator.Bitwise.Shl -> OpLongInt(
                        a = x,
                        b = parseStatement(
                            blockContext,
                            precedence - 1,
                            ExpectedBlockType.Object
                        ),
                        op = Long::shl
                    )

                    Token.Operator.Bitwise.Shr -> OpLongInt(
                        a = x,
                        b = parseStatement(
                            blockContext,
                            precedence - 1,
                            ExpectedBlockType.Object
                        ),
                        op = Long::shr
                    )

                    Token.Operator.Bitwise.Ushr -> OpLongInt(
                        a = x,
                        b = parseStatement(
                            blockContext,
                            precedence - 1,
                            ExpectedBlockType.Object
                        ),
                        op = Long::ushr
                    )

                    else -> { returnToIndex(i); break; }
                }

                6 -> when (nextSignificant()) {
                    Token.Operator.In -> parseInOperator(x, precedence)
                    Token.Operator.Instanceof -> parseInstanceOfOperator(x, precedence)
                    Token.Identifier.Keyword.Of -> parseOfOperator(x, precedence)
                    else -> { returnToIndex(i); break; }
                }

                7 -> when (nextSignificant()) {
                    Token.Operator.Compare.Less -> OpCompare(
                        a = x,
                        b = parseStatement(blockContext, precedence, ExpectedBlockType.Object),
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

                    else -> { returnToIndex(i); break; }
                }

                8 -> when (nextSignificant()) {
                    Token.Operator.Compare.Eq -> OpEquals(
                        a = x,
                        b = parseStatement(blockContext, precedence, ExpectedBlockType.Object),
                        isTyped = false
                    )

                    Token.Operator.Compare.StrictEq -> OpEquals(
                        a = x,
                        b = parseStatement(blockContext, precedence, ExpectedBlockType.Object),
                        isTyped = true
                    )

                    Token.Operator.Compare.Neq -> OpNotEquals(
                        a = x,
                        b = parseStatement(blockContext, precedence, ExpectedBlockType.Object),
                        isTyped = false
                    )

                    Token.Operator.Compare.StrictNeq -> OpNotEquals(
                        a = x,
                        b = parseStatement(blockContext, precedence, ExpectedBlockType.Object),
                        isTyped = true
                    )

                    else -> { returnToIndex(i); break; }
                }

                9 -> when (nextSignificant()) {
                    Token.Operator.Bitwise.And -> OpLongLong(
                        a = x,
                        b = parseStatement(blockContext, precedence, ExpectedBlockType.Object),
                        op = Long::and
                    )

                    else -> { returnToIndex(i); break; }
                }

                10 -> when (nextSignificant()) {
                    Token.Operator.Bitwise.Xor -> OpLongLong(
                        a = x,
                        b = parseStatement(blockContext, precedence, ExpectedBlockType.Object),
                        op = Long::xor
                    )

                    else -> { returnToIndex(i); break; }
                }

                11 -> when (nextSignificant()) {
                    Token.Operator.Bitwise.Or -> OpLongLong(
                        a = x,
                        b = parseStatement(blockContext, precedence, ExpectedBlockType.Object),
                        op = Long::or
                    )

                    else -> { returnToIndex(i); break; }
                }

                12 -> when (nextSignificant()) {
                    Token.Operator.Logical.And -> {
                        val a = x
                        val b =
                            parseStatement(blockContext, precedence, ExpectedBlockType.Object)
                        Expression {
                            val av = a(it)
                            if (it.isFalse(av)) {
                                return@Expression av
                            }
                            b(it)
                        }
                    }

                    else -> { returnToIndex(i); break; }
                }

                13 -> when (nextSignificant()) {
                    Token.Operator.Logical.Or -> {
                        val a = x
                        val b =
                            parseStatement(blockContext, precedence, ExpectedBlockType.Object)
                        Expression {
                            val av = a(it)
                            if (!it.isFalse(av)) {
                                return@Expression av
                            }
                            b(it)
                        }
                    }

                    Token.Operator.NullishCoalescing -> {
                        val replacement =
                            parseStatement(blockContext, precedence, ExpectedBlockType.Object)
                        val subject = x
                        Expression {
                            subject(it)?.takeUnless { it is Undefined } ?: replacement(it)
                        }
                    }

                    else -> { returnToIndex(i); break; }
                }

                14 -> when (val next = nextSignificant()) {
                    Token.Operator.QuestionMark -> parseTernary(
                        condition = x,
                        precedence = precedence,
                        blockContext = blockContext
                    )

                    Token.Operator.Arrow -> OpFunctionInit(parseArrowFunction(blockContext, x))

                    is Token.Operator.Assign -> parseAssignmentValue(
                        x = x,
                        blockContext = blockContext,
                        merge = getMergeForAssignment(next)
                    )

                    is Token.Operator.Colon -> if (BlockContext.Ternary !in blockContext) {
                        // Check if this is a computed property name
                        // Computed property names include: [expr], OpComputedPropertyName, and OpMakeArray (when used as computed key)
                        val isComputedProperty = x is OpComputedPropertyName || x is OpMakeArray
                        
                        val key = when {
                            x is OpGetProperty -> x.name.js
                            x is OpConstant -> x.value
                            isComputedProperty -> null as JsAny? // Computed property - key will be evaluated at runtime
                            x is OpIndex -> throw SyntaxError("Invalid usage of : operator (unexpected index)")
                            else -> throw SyntaxError("Invalid usage of : operator")
                        }
                        
                        // When parsing property values in an object literal, use ObjectPropertyValue context
                        // to distinguish array literals [1,2] from computed property names [expr]
                        val propertyValueContext = blockContext + BlockContext.ObjectPropertyValue
                        
                        val value = if (isComputedProperty) {
                            // For computed property names, we need to store both the key expression and value expression
                            // For OpMakeArray, the array itself is the key expression
                            val keyExpr = when (x) {
                                is OpComputedPropertyName -> x.keyExpression
                                is OpMakeArray -> OpMakeArray(x.items)
                                else -> x
                            }
                            OpComputedProperty(
                                keyExpr, parseStatement(
                                propertyValueContext,
                                precedence,
                                ExpectedBlockType.Object
                            ))
                        } else {
                            parseStatement(
                                propertyValueContext,
                                precedence,
                                ExpectedBlockType.Object
                            )
                        }
                        
                        OpColonAssignment(key = key, value = value)
                    } else {
                        returnToIndex(i); break;
                    }

                    else -> { returnToIndex(i); break; }
                }


                else -> error("Invalid operator priority - $precedence")
            }
        }
    }
    return x
}

private fun ListIterator<LocatedToken>.parseFactor(
    blockContext: List<BlockContext>,
    blockType: ExpectedBlockType = ExpectedBlockType.None
): Expression {
    val loc = nextSignificantLocation()
    // In object / class property-key position *any* IdentifierName (incl. reserved words and the
    // operator-keywords below) is a valid property name; see [KEYWORD_OPERATOR_NAMES].
    val inMemberKeyPosition = blockContext.lastOrNull() == BlockContext.Object
        || blockContext.lastOrNull() == BlockContext.Class
    val expr =  when (val next = nextSignificant()) {
        is Token.Str -> OpConstant(next.value.js).at(loc)
        is Token.Regex -> OpConstant(next.value.toJsRegex()).at(loc)
        is Token.TemplateString -> {
            val expressions = next.tokens.fastMap {
                when (it) {
                    is TemplateStringToken.Str -> OpConstant(it.value.js)
                    is TemplateStringToken.Template -> it.value.sanitizeTokens()
                        .map { LocatedToken(it, SourceLocation(0, 0)) }
                        .listIterator()
                        .parseBlock(
                            type = ExpectedBlockType.Block,
                            isExpressible = true,
                            blockContext = emptyList()
                    )
                }
            }
            Expression { r ->
                expressions.fastMap { it(r) }.joinToString("").js
            }
        }
        is Token.Operator.Period, is Token.Num -> {
            val num = if (next is Token.Num) {
                next.value
            } else {
                val number = next().token
                syntaxCheck(
                    number is Token.Num
                            && !number.isFloat
                            && number.format == NumberFormat.Dec
                ) {
                    unexpected(".")
                }
                "0.${number.value}".toDouble()
            }
            OpConstant(num.js)
        }

        Token.Operator.Spread -> OpSpread(
            parseStatement(
                blockType = ExpectedBlockType.Object,
            )
        ).also {
            if (it.value is OpFunctionInit && it.value.function.isArrow){
                throw SyntaxError(unexpected("..."))
            }
        }
        Token.Operator.Arithmetic.Inc,
        Token.Operator.Arithmetic.Dec -> {
            val isInc = next is Token.Operator.Arithmetic.Inc
            val variable = parseStatement(maxPrecedence = 0, blockType = ExpectedBlockType.Object)

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
            a = parseStatement(maxPrecedence = 0, blockType = ExpectedBlockType.Object),
            op = if (next is Token.Operator.Arithmetic.Plus)
                ScriptRuntime::pos else ScriptRuntime::neg
        )

        Token.Operator.Logical.Not -> OpNot(
            condition = parseStatement(maxPrecedence = 0, blockType = ExpectedBlockType.Object)
        )

        Token.Operator.Bitwise.Reverse -> {
            val expr = parseStatement(maxPrecedence = 0, blockType = ExpectedBlockType.Object)
            Expression {
                it.toNumber(expr(it)).toLong().inv().js
            }
        }

        Token.Operator.Bracket.CurlyOpen -> {
            prevSignificant()
            // If we're in object context, treat { } as object literal
            val objectType = if (BlockContext.Object in blockContext)
                ExpectedBlockType.Object
            else
                blockType
            parseBlock(
                blockContext = blockContext,
                type = objectType
            )
        }

        Token.Operator.Bracket.RoundOpen -> {
            prevSignificant()
            parseExpressionGrouping(blockContext)
        }
        Token.Operator.Bracket.SquareOpen -> {
            prevSignificant()
            // Check if we're in object context but not inside a property value.
            // In object context, [ at property key position = computed property name.
            // In object property value position = array literal.
            val isObjectPropertyKey = blockContext.lastOrNull() == BlockContext.Object
            if (isObjectPropertyKey) {
                parseComputedPropertyName()
            } else {
                parseArrayCreation()
            }
        }
        // Operator-keywords name a property in member-key position (see [KEYWORD_OPERATOR_NAMES]);
        // otherwise each keeps its operator semantics.
        //
        // `in`/`instanceof` are binary operators, but they DO reach this point: object-literal
        // keys are parsed through the generic expression path, so `{ in: 1 }` / `({in(){}}).in()`
        // call `parseFactor` with an `In` token as the very first token of the key. Dropping these
        // two cases makes `({in: 42}).in` fail with "Unexpected token 'In'"
        // (see KeywordPropertyNameTest.allKeywordsAreValidPropertyNamesLikeV8).
        Token.Operator.New,
        Token.Operator.Typeof,
        Token.Operator.Void,
        Token.Operator.Delete,
        Token.Operator.In,
        Token.Operator.Instanceof -> {
            val keywordName = next.propertyNameOrNull()
            if (inMemberKeyPosition && keywordName != null) {
                OpGetProperty(name = keywordName, receiver = null).at(loc)
            } else when (next) {
                is Token.Operator.New -> parseNew(loc)
                is Token.Operator.Typeof -> parseTypeof()
                is Token.Operator.Void -> parseVoid()
                is Token.Operator.Delete -> parseDelete()
                else -> throw SyntaxError(unexpected(next::class.simpleName.orEmpty()))
            }
        }
        is Token.Identifier.Keyword -> {
            // In object/class property key position, keywords (e.g. `default`, `if`, `class`)
            // can be valid property names. We decide by peeking the token that immediately
            // follows the keyword.
            //
            // The set of "property-name" tokens below is derived from the ECMAScript grammar
            // for what may follow a `PropertyName` (which, per spec, may be *any* IdentifierName,
            // including reserved words/keywords) when it appears as the key of an object-literal
            // member or a class element:
            //
            //   PropertyDefinition / MethodDefinition / ClassElement
            //     PropertyName : AssignmentExpression        -> key followed by `:`
            //     MethodDefinition: PropertyName ( ... ) {... -> key followed by `(`
            //     shorthand IdentifierReference { default }   -> key followed by `}` (last member)
            //     shorthand IdentifierReference { default, ..} -> key followed by `,` (more follow)
            //
            // So if the keyword is followed by `:`, `(`, `,` or `}` it is unambiguously being used
            // as a property name. Any other continuation — notably *another identifier* such as the
            // `async foo()`, `get foo()`, `set foo()` or `static foo()` modifiers — means the
            // keyword is acting as a modifier/keyword, so we fall back to `parseKeyword`.
            //
            // Boundary note: class-field shorthand forms (e.g. `class X { default; }` /
            // `default = 1`) are followed by `;`/`=` and are intentionally NOT in this set; the
            // current parser handles those via the `OpAssign` path rather than as a bare keyword name.
            val ctx = blockContext.lastOrNull()
            val inMemberKeyPosition = ctx == BlockContext.Object || ctx == BlockContext.Class
            if (inMemberKeyPosition) {
                val i = nextIndex()
                val n = nextSignificant()
                returnToIndex(i)
                val isPropertyName = n is Token.Operator.Colon ||
                        n is Token.Operator.Bracket.RoundOpen ||
                        n is Token.Operator.Comma ||
                        n is Token.Operator.Bracket.CurlyClose
                if (isPropertyName) {
                    OpGetProperty(next.identifier, receiver = null).at(loc)
                } else {
                    parseKeyword(next, blockContext, loc)
                }
            } else {
                parseKeyword(next, blockContext, loc)
            }
        }
        is Token.Identifier.Reserved -> {
            // Reserved words (enum/implements/package/...) are valid IdentifierNames, so V8
            // accepts them as property keys (`{enum: 1}`, `({enum(){}}).enum()`, `a.enum`,
            // `a?.enum`) — but NOT as shorthand references (`{enum}` / `{enum, a:1}` are
            // SyntaxError in V8 because a reserved word can never be an IdentifierReference).
            // Hence: property name only when followed by `:` (key) or `(` (method).
            val ctx = blockContext.lastOrNull()
            val inMemberKeyPosition = ctx == BlockContext.Object || ctx == BlockContext.Class
            if (inMemberKeyPosition) {
                val i = nextIndex()
                val n = nextSignificant()
                returnToIndex(i)
                if (n is Token.Operator.Colon || n is Token.Operator.Bracket.RoundOpen) {
                    OpGetProperty(next.identifier, receiver = null).at(loc)
                } else {
                    throw SyntaxError("Unexpected reserved word (${next.identifier})")
                }
            } else {
                throw SyntaxError("Unexpected reserved word (${next.identifier})")
            }
        }
        is Token.Identifier.Property -> {
            val ctx2 = blockContext.lastOrNull()
            val canHaveAccessor = ctx2 == BlockContext.Object || ctx2 == BlockContext.Class
            var i = nextIndex()
            val n = nextSignificant()
            when {
                canHaveAccessor && next.identifier == "get" && n is Token.Identifier ->
                    OpGetter(parseFunction(name = n.identifier, blockContext = blockContext))
                canHaveAccessor && next.identifier == "set" && n is Token.Identifier ->
                    OpSetter(parseFunction(name = n.identifier, blockContext = blockContext))
                else -> {
                    returnToIndex(i)
                    OpGetProperty(next.identifier, receiver = null).at(loc)
                }
            }
        }
        is Token.Operator.SemiColon -> Expression { Undefined }.at(loc)
        else -> throw SyntaxError(unexpected(next::class.simpleName.orEmpty()))
    }

    return expr
}

/**
 * Keywords that can only begin a *statement* in ECMAScript and therefore must never appear
 * in an expression position (an initializer, the right-hand side of an assignment, ...).
 *
 * keight's parser is intentionally statement/expression agnostic: [parseFactor] routes every
 * keyword to [parseKeyword], and [parseAssignmentValue] parses the right-hand side through the
 * very same [parseStatement] entry point. Without this guard `var a = if (true) '1'` silently
 * parses as an if-statement and assigns its completion value instead of raising a SyntaxError
 * the way a spec-compliant engine does.
 *
 * `function`, `class` and `import` are deliberately excluded: all three have valid expression
 * forms (function expression, class expression, dynamic `import()`).
 */
private val StatementOnlyKeywords = setOf(
    Token.Identifier.Keyword.Var,
    Token.Identifier.Keyword.Let,
    Token.Identifier.Keyword.Const,
    Token.Identifier.Keyword.If,
    Token.Identifier.Keyword.Else,
    Token.Identifier.Keyword.For,
    Token.Identifier.Keyword.While,
    Token.Identifier.Keyword.Do,
    Token.Identifier.Keyword.Switch,
    Token.Identifier.Keyword.Case,
    Token.Identifier.Keyword.Default,
    Token.Identifier.Keyword.Try,
    Token.Identifier.Keyword.Catch,
    Token.Identifier.Keyword.Finally,
    Token.Identifier.Keyword.Throw,
    Token.Identifier.Keyword.Return,
    Token.Identifier.Keyword.Break,
    Token.Identifier.Keyword.Continue,
    Token.Identifier.Keyword.With,
    Token.Identifier.Keyword.Debugger,
    Token.Identifier.Keyword.Export,
)

/**
 * Reject a [StatementOnlyKeywords] token found at the start of an expression.
 *
 * Nothing is consumed: the stream is always restored so the caller can parse the expression
 * itself when the upcoming token is legal.
 *
 * Called from every position where the grammar requires an expression rather than a statement:
 * assignment/initializer right-hand sides, concise arrow-function bodies, parenthesized groups
 * (call/`new` arguments, `if`/`while`/`switch` heads), array elements, both ternary branches and
 * the operands of `return` / `throw`.
 */
private fun ListIterator<LocatedToken>.checkExpressionStart() {
    if (!hasNext()) return
    val i = nextIndex()
    val next = nextSignificant()
    returnToIndex(i)
    if (next is Token.Identifier.Keyword && next in StatementOnlyKeywords) {
        throw SyntaxError(unexpected(next.identifier))
    }
}

private fun ListIterator<LocatedToken>.parseAssignmentValue(
    x: Expression,
    blockContext: List<BlockContext>,
    merge: (suspend ScriptRuntime.(JsAny?, JsAny?) -> JsAny?)? = null
): Expression {
    // Every branch below parses the right-hand side as an expression, so a statement-only
    // keyword there is always a syntax error (e.g. `var a = if (true) '1'`).
    checkExpressionStart()

    // When parsing property values in an object literal, use ObjectPropertyValue context
    // to distinguish array literals [1,2] from computed property names [expr]
    val propertyValueContext = blockContext + BlockContext.ObjectPropertyValue
    return when (x) {
        is OpIndex -> OpAssignByIndex(
            receiver = x.receiver,
            index = x.index,
            assignableValue = parseStatement(blockType = ExpectedBlockType.Object, blockContext = propertyValueContext),
            merge = merge
        )

        is OpGetProperty -> OpAssign(
            variableName = x.name,
            receiver = x.receiver,
            assignableValue = parseStatement(blockType = ExpectedBlockType.Object,blockContext = propertyValueContext),
            merge = merge
        )
        is OpBlock, is OpMake -> OpDestructAssign(
            destruction = x.asDestruction(),
            variableType = null,
            value = parseStatement(blockType = ExpectedBlockType.Object, blockContext = propertyValueContext)
        )
        is OpSpread -> throw SyntaxError("Rest parameter may not have a default initializer")

        else -> throw SyntaxError("Invalid left-hand in assignment ($x)")
    }
}


private fun getMergeForAssignment(operator: Token.Operator.Assign): (suspend ScriptRuntime.(JsAny?, JsAny?) -> JsAny?)? {
    return when (operator) {
        Token.Operator.Assign.Assignment -> null
        Token.Operator.Assign.PlusAssign -> ScriptRuntime::sum
        Token.Operator.Assign.MinusAssign -> ScriptRuntime::sub
        Token.Operator.Assign.MulAssign -> ScriptRuntime::mul
        Token.Operator.Assign.DivAssign -> ScriptRuntime::div
        Token.Operator.Assign.ExpAssign -> { a, b ->
            toNumber(a).toDouble().pow(toNumber(b).toDouble()).js
        }

        Token.Operator.Assign.ModAssign -> { a, b ->
            (toNumber(a).toLong() and toNumber(b).toLong()).js
        }

        Token.Operator.Assign.BitAndAssign -> { a, b ->
            (toNumber(a).toLong() and toNumber(b).toLong()).js
        }

        Token.Operator.Assign.LogicAndAssign -> { a, b ->
            if (isFalse(a)) a else b
        }

        Token.Operator.Assign.BitOrAssign -> { a, b ->
            (toNumber(a).toLong() or toNumber(b).toLong()).js
        }

        Token.Operator.Assign.LogicOrAssign -> { a, b ->
            if (isFalse(a)) b else a
        }

        Token.Operator.Assign.BitXorAssign -> { a, b ->
            (toNumber(a).toLong() xor toNumber(b).toLong()).js
        }

        Token.Operator.Assign.UshrAssign -> { a, b ->
            (toNumber(a).toLong() ushr toNumber(b).toInt()).js
        }

        Token.Operator.Assign.ShrAssign -> { a, b ->
            (toNumber(a).toLong() shr toNumber(b).toInt()).js
        }

        Token.Operator.Assign.ShlAssign -> { a, b ->
            (toNumber(a).toLong() shl toNumber(b).toInt()).js
        }

        Token.Operator.Assign.NullCoalescingAssign -> { a, b ->
            if (a == null || a is Undefined) b else a
        }
    }
}

private fun ListIterator<LocatedToken>.parseKeyword(
    keyword: Token.Identifier.Keyword,
    blockContext: List<BlockContext>,
    fnLoc: SourceLocation? = null,
): Expression {
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
        Token.Identifier.Keyword.True -> OpConstant(true.js)
        Token.Identifier.Keyword.False -> OpConstant(false.js)
        Token.Identifier.Keyword.Null -> OpConstant(null)

        Token.Identifier.Keyword.Switch -> parseSwitch(blockContext)
        Token.Identifier.Keyword.Case,
        Token.Identifier.Keyword.Default -> {
            syntaxCheck(blockContext.last() == BlockContext.Switch) {
                unexpected("case")
            }
            val case = if (keyword == Token.Identifier.Keyword.Case)
                parseFactor(blockContext)
            else OpCase.Default

            syntaxCheck(nextSignificant() is Token.Operator.Colon) {
                "Expected ':' after 'case'"
            }
            OpCase(case)
        }

        Token.Identifier.Keyword.For -> parseForLoop(blockContext)
        Token.Identifier.Keyword.While -> parseWhileLoop(blockContext)
        Token.Identifier.Keyword.Do -> parseDoWhileLoop(blockContext)
        Token.Identifier.Keyword.Continue -> {
            val label = (next().token as? Token.Identifier)?.identifier ?: null.also { previous() }
            OpContinue(label).also {
                syntaxCheck(blockContext.lastOrNull() == BlockContext.Loop) {
                    unexpected("continue")
                }
            }
        }
        Token.Identifier.Keyword.Break -> {
            val label = (next().token as? Token.Identifier)?.identifier ?: null.also { previous() }
            OpBreak(label).also {
                val context = blockContext.lastOrNull()
                syntaxCheck(context == BlockContext.Loop || context == BlockContext.Switch || label != null) {
                    unexpected("break")
                }
            }
        }

        Token.Identifier.Keyword.If ->  {
            val condition = parseExpressionGrouping(blockContext)
            val onTrue = parseBlock(blockContext = blockContext)
            // Issue #22: tolerate a stray `;` between the if-consequent and `else`
            // (e.g. `if (true) x=1; else y=2`). In standard JS an explicit `;` terminates
            // the if-statement, but many hand-written scripts rely on this form.
            // `eatKeywordAfterOptionalSemicolon` consumes an optional `;` ONLY when `else`
            // actually follows; otherwise the `;` stays in the stream so the enclosing
            // block can still use it as a statement separator (e.g. `if (a) b; c`).
            val onFalse = if (eatKeywordAfterOptionalSemicolon(Token.Identifier.Keyword.Else)) {
                parseBlock(blockContext = blockContext)
            } else null
            OpIfCondition(
                condition = condition,
                onTrue = onTrue,
                onFalse = onFalse,
                // Issue #23: propagate the completion value of the taken branch so that
                // `if (true) '1'` (as a program's last statement) yields `'1'`, matching
                // the ECMAScript completion-value semantics. `if (false)` without an
                // `else` still yields `Undefined` (onFalse is null -> res is null).
                expressible = true
            )
        }
        Token.Identifier.Keyword.Function -> OpFunctionInit(
            parseFunction(
                blockContext = blockContext,
                fnLoc = fnLoc
            )
        )
        Token.Identifier.Keyword.Return -> {
            syntaxCheck(BlockContext.Function in blockContext) {
                unexpected("return")
            }
            // ASI: an empty `return` is terminated by a newline or an explicit `;`.
            // We must NOT consume that separator here — the enclosing block is responsible
            // for consuming statement separators (newline / `;` / `,`). Consuming the newline
            // would leave the following statement without a separator and make the block fail
            // with "Unexpected token 'Keyword'" when the next statement starts with a keyword
            // (see issue #20, e.g. `if (true) return\nif (true) return`).
            // `}` and the end of the token stream terminate an empty `return` too
            // (`function f() { return }`), and unlike `\n` / `;` the `}` must stay in the
            // stream for parseBlock to close the body.
            val term = peekRawToken()
            if (term == null ||
                term == Token.NewLine ||
                term == Token.Operator.SemiColon ||
                term == Token.Operator.Bracket.CurlyClose
            ) {
                OpReturn(OpConstant(Undefined))
            } else {
                // The operand of a non-empty `return` is an Expression. ASI was already
                // resolved above, so peeking past newlines here is safe.
                checkExpressionStart()
                OpReturn(parseStatement(blockContext = blockContext, blockType = ExpectedBlockType.Object))
            }
        }

        Token.Identifier.Keyword.Class -> parseClass()
        Token.Identifier.Keyword.Throw -> {
            // The operand of `throw` is an Expression.
            checkExpressionStart()
            val throwable = parseStatement(blockType = ExpectedBlockType.Object)
            Expression {
                val t = throwable(it)
                throw t as? Throwable ?: ThrowableValue(t)
            }
        }
        Token.Identifier.Keyword.Async -> parseAsync(blockContext)
        Token.Identifier.Keyword.Await -> parseAwait()
        Token.Identifier.Keyword.Try -> parseTryCatch(blockContext)
        Token.Identifier.Keyword.This -> Expression { it.thisRef }
        Token.Identifier.Keyword.With -> {
            TODO()
//            val arg = parseExpressionGrouping().expressions.single()
//            val block = parseBlock(type = ExpectedBlockType.Block, scoped = false, blockContext = emptyList())
//
//            return Expression { r ->
//
//                val a = arg(r)
//
//                if (a is JSObject) {
//                    r.withScope(
//                        thisRef = a,
//                    ) {
//                        block(it)
//                    }
//                } else {
//
//                }
//            }
        }
        Token.Identifier.Keyword.Debugger -> throw SyntaxError("Debugger is not supported")
        Token.Identifier.Keyword.Of -> throw SyntaxError("'of' can only be used in for...of loop")
        Token.Identifier.Keyword.Super -> parseSuper(blockContext)
        Token.Identifier.Keyword.Else,
        Token.Identifier.Keyword.Extends,
        Token.Identifier.Keyword.Finally,
        Token.Identifier.Keyword.Catch -> throw SyntaxError(unexpected(keyword.identifier))
        Token.Identifier.Keyword.Import -> parseImport()
        Token.Identifier.Keyword.Export -> parseExport()

    }
}

/**
 * Parse super keyword and its subsequent operations.
 * super can be followed by:
 * - .property (property access on parent prototype)
 * - [expr] (computed property access on parent prototype)
 * - (args) (parent constructor call)
 */
private fun ListIterator<LocatedToken>.parseSuper(blockContext: List<BlockContext>): Expression {
    syntaxCheck(BlockContext.Class in blockContext) {
        "'super' can only be used in class methods"
    }

    return when (val next = nextSignificant()) {
        // super() - call parent constructor
        is Token.Operator.Bracket.RoundOpen -> {
            prevSignificant()
            parseSuperConstructorCall(blockContext)
        }
        // super.xxx - property access on parent prototype
        is Token.Operator.Period, is Token.Operator.DoublePeriod -> {
            val prop = nextSignificant()
            syntaxCheck(prop is Token.Identifier) {
                "Illegal super property access"
            }
            OpSuperGetProperty(name = prop.identifier)
        }
        // super[expr] - computed property access on parent prototype
        is Token.Operator.Bracket.SquareOpen -> {
            val indexExpr = parseStatement(blockType = ExpectedBlockType.Object)
            syntaxCheck(nextSignificant() is Token.Operator.Bracket.SquareClose) {
                "Missing ']' after super expression"
            }
            OpSuperGetPropertyComputed(index = indexExpr)
        }
        else -> {
            throw SyntaxError("'super' must be followed by '.', '[', or '('")
        }
    }
}

/**
 * Parse super(...) constructor call.
 * super() should use the 'super' variable that was passed as extraArgs in JSFunction.construct()
 */
private fun ListIterator<LocatedToken>.parseSuperConstructorCall(blockContext: List<BlockContext>): Expression {
    val args = parseExpressionGrouping(blockContext).expressions
    // The 'super' variable is injected by JSFunction.construct() via extraArgs
    return Expression { runtime ->
        val superFn = runtime.get("super".js)
        if (superFn is Callable) {
            superFn.call(runtime.thisRef, args.fastMap { it(runtime) }, runtime)
        } else {
            runtime.referenceError { "'super' is not defined".js }
        }
    }
}

private fun ListIterator<LocatedToken>.parseNew(loc: SourceLocation?) : Expression {
    // `loc` already points at the user's original source line (the script is no
    // longer wrapped), so no line-number compensation is needed.
    val adjustedLine = loc?.line

    val index = nextIndex()
    val next = nextSignificant()

    if (next is Token.Identifier.Property) {

        val args = if (nextIsInstance<Token.Operator.Bracket.RoundOpen>()) {
            parseExpressionGrouping().expressions
        } else {
            emptyList()
        }

        return Expression {
            val constructor = it.get(next.identifier.js)
            it.typeCheck(constructor is Constructor) {
                "'$constructor' (${next.identifier}) is not a constructor".js
            }
            val frame = CallFrame(
                functionName = next.identifier,
                fileName = loc?.fileName,
                lineNumber = adjustedLine,
                columnNumber = loc?.column,
                isConstructor = true
            )
            it.pushCallFrame(frame)
            try {
                constructor.construct(args.fastMap { arg -> arg(it) }, it).also { result ->
                    if (adjustedLine != null && result is Expression.LocationAttachable) {
                        result.attachLocation(adjustedLine, loc.column, loc.fileName)
                    }
                    if (result is JSError) {
                        val frames = it.captureLongStack()
                        // Drop the constructor's own call frame so the top stack
                        // frame is the *enclosing* function at the construction
                        // site, matching V8 (where `new Error` does not add a
                        // separate frame — the deepest frame is e.g. `at l3`).
                        result.callStackFrames =
                            if (frames.lastOrNull()?.isConstructor == true) frames.dropLast(1)
                            else frames
                    }
                }
            } finally {
                it.popCallFrame()
            }
        }
    } else {
        returnToIndex(index)
        val constructor = parseStatement(
            blockContext = emptyList(),
            blockType = ExpectedBlockType.Object
        )
        return Expression {
            val c = constructor.invoke(it)
            it.typeCheck(c is Constructor) {
                "'$c' is not a constructor".js
            }
            it.typeCheck(c !is JSFunction || !c.isArrow){
                "(intermediate value) is not a constructor".js
            }
            val frame = CallFrame(
                functionName = (c as? JSFunction)?.name?.takeIf { n -> n.isNotEmpty() },
                fileName = loc?.fileName,
                lineNumber = adjustedLine,
                columnNumber = loc?.column,
                isConstructor = true
            )
            it.pushCallFrame(frame)
            try {
                c.construct(emptyList(), it).also { result ->
                    if (adjustedLine != null && result is Expression.LocationAttachable) {
                        result.attachLocation(adjustedLine, loc.column, loc.fileName)
                    }
                    if (result is JSError) {
                        val frames = it.captureLongStack()
                        // Drop the constructor's own call frame so the top stack
                        // frame is the *enclosing* function at the construction
                        // site, matching V8 (where `new Error` does not add a
                        // separate frame — the deepest frame is e.g. `at l3`).
                        result.callStackFrames =
                            if (frames.lastOrNull()?.isConstructor == true) frames.dropLast(1)
                            else frames
                    }
                }
            } finally {
                it.popCallFrame()
            }
        }
    }
}

private fun ListIterator<LocatedToken>.parseVoid() : Expression {
    val isArg = nextIsInstance<Token.Operator.Bracket.RoundOpen>()
    val expr = if (isArg) {
        parseExpressionGrouping()
    }  else {
        parseStatement(maxPrecedence = 1,blockType = ExpectedBlockType.Object)
    }
    return Expression {
        expr(it)
        Undefined
    }
}

private fun ListIterator<LocatedToken>.parseTypeof() : Expression {
    val isArg = nextIsInstance<Token.Operator.Bracket.RoundOpen>()
    val expr = if (isArg) {
        parseExpressionGrouping()
    }  else {
        parseStatement(maxPrecedence = 1,blockType = ExpectedBlockType.Object)
    }
    return Expression {
        try {
            expr(it)?.type?.js ?: "object".js
        } catch (t : ReferenceError){
            "undefined".js
        }
    }
}

private fun ListIterator<LocatedToken>.parseDelete() : Expression {
    val x = parseStatement(maxPrecedence = 1, blockType = ExpectedBlockType.Object)

    val (subj, obj) = when (x) {
        is OpIndex -> x.receiver to x.index
        is OpGetProperty -> x.receiver to OpConstant(x.name.js)
        else -> return OpConstant(false.js)
    }

    return Expression {
        val s = subj?.invoke(it)
        val o = obj(it)
        s?.delete(o, it)?.js ?: it.delete(o).js
    }
}


private fun ListIterator<LocatedToken>.parseArrayCreation(): Expression {
    check(eat(Token.Operator.Bracket.SquareOpen))

    val expressions = buildList {
        while (!eat(Token.Operator.Bracket.SquareClose)) {
            if (eat(Token.Operator.Comma)) {
                add(OpConstant(Uninitialized))
            } else {
                // Array elements are AssignmentExpressions.
                checkExpressionStart()
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

/**
 * Parses a computed property name in an object literal context.
 * Syntax: { [expression]: value } or { [expression]() {} }
 */
private fun ListIterator<LocatedToken>.parseComputedPropertyName(): Expression {
    check(eat(Token.Operator.Bracket.SquareOpen))
    
    // Parse the key expression - use low precedence to get only the next token/expression
    // The key expression in computed property name is evaluated as-is
    val keyExpr = parseStatement(
        blockContext = emptyList(), 
        maxPrecedence = 14,
        blockType = ExpectedBlockType.Object
    )
    
    syntaxCheck(eat(Token.Operator.Bracket.SquareClose)) {
        "Missing ']' in computed property name"
    }
    
    // Return an expression that represents the computed property name.
    // Use a dedicated expression type for clarity.
    return OpComputedPropertyName(keyExpr)
}

private fun ListIterator<LocatedToken>.parseExpressionGrouping(blockContext: List<BlockContext> = emptyList()): OpTouple {
    check(eat(Token.Operator.Bracket.RoundOpen))

    val expressions = if (nextIsInstance<Token.Operator.Bracket.RoundClose>()) {
        emptyList()
    } else buildList {
        do {
            if (nextIsInstance<Token.Operator.Bracket.RoundClose>()) {
                return@buildList
            }
            // Every item of a parenthesized group is an expression: call arguments,
            // `new` arguments, `if`/`while`/`switch` heads, a parenthesized expression.
            checkExpressionStart()
            add(parseStatement(blockContext, blockType = ExpectedBlockType.Object))
        } while (nextSignificant() is Token.Operator.Comma)
        prevSignificant()
    }
    syntaxCheck(eat(Token.Operator.Bracket.RoundClose)) {
        "Expected ')'"
    }

    return OpTouple(expressions)
}

/**
 * ECMAScript allows *any* IdentifierName — including reserved words and punctuator-keywords —
 * to be used as a property name in three positions:
 *  - member access:      `a.delete`
 *  - optional chaining:  `a?.delete`
 *  - the key of an object literal / class element: `{ delete: 1 }`, `class C { delete(){} }`
 *
 * The lexer emits `delete`, `void`, `typeof`, `in`, `instanceof` and `new` as operator tokens
 * (which carry no textual name), so whenever such a token appears in a property-name position we
 * recover the original identifier from this map. `null` means the token cannot name a property.
 */
private val KEYWORD_OPERATOR_NAMES = mapOf<Token, String>(
    Token.Operator.New to "new",
    Token.Operator.In to "in",
    Token.Operator.Instanceof to "instanceof",
    Token.Operator.Typeof to "typeof",
    Token.Operator.Void to "void",
    Token.Operator.Delete to "delete",
)

/**
 * Returns the property-name string for a token if it may legally name a property, otherwise `null`.
 * `Token.Identifier` (incl. `Token.Identifier.Keyword` such as `class`/`function`/`if`) already
 * carries its name; the operator-keywords listed in [KEYWORD_OPERATOR_NAMES] recover theirs here.
 */
private fun Token.propertyNameOrNull(): String? = when (this) {
    is Token.Identifier -> identifier
    else -> KEYWORD_OPERATOR_NAMES[this]
}

private fun ListIterator<LocatedToken>.parseMemberOf(receiver: Expression): Expression {
    return when (nextSignificant()){

        is Token.Operator.Period, is Token.Operator.DoublePeriod -> {
            val propLoc = nextSignificantLocation()  // peek at property name location
            val next = nextSignificant()
            val name = next.propertyNameOrNull()
            syntaxCheck(name != null) {
                "Illegal symbol after '.'"
            }
            OpGetProperty(name = name, receiver = receiver)
                .at(propLoc)
        }
        is Token.Operator.Bracket.SquareOpen -> {
            val bracketLoc = nextSignificantLocation()  // peek at index location
            val index = parseStatement(blockType = ExpectedBlockType.Object)
            OpIndex(
                receiver = receiver,
                index = index
            ).also {
                syntaxCheck(nextSignificant() is Token.Operator.Bracket.SquareClose) {
                    "Missing ']'"
                }
            }.at(bracketLoc)
        }
        else -> throw IllegalStateException("Illegal 'member of' syntax")
    }
}

private fun ListIterator<LocatedToken>.parseOptionalChaining(receiver: Expression): Expression {
    val loc = nextSignificantLocation()  // peek at the next token's location before consuming
    return when(val next = nextSignificant()){
        is Token.Operator.Bracket.SquareOpen -> {
            val indexLoc = nextSignificantLocation()  // peek at index location
            OpIndex(
                receiver = receiver,
                index = parseStatement(blockType = ExpectedBlockType.Object),
                isOptional = true
            ).also {
                syntaxCheck(nextSignificant() is Token.Operator.Bracket.SquareClose) {
                    "Missing ']'"
                }
            }.at(indexLoc)
        }
        is Token.Operator.Bracket.RoundOpen -> {
            prevSignificant()
            parseFunctionCall(receiver, optional = true, blockContext = emptyList())
        }
        else -> {
            val name = next.propertyNameOrNull()
            if (name != null) {
                OpGetProperty(
                    name = name,
                    receiver = receiver,
                    isOptional = true
                ).at(loc)
            } else throw SyntaxError("Invalid usage of ?. operator")
        }
    }
}

private fun ListIterator<LocatedToken>.parseFunctionCall(
    function : Expression,
    optional : Boolean = false,
    blockContext: List<BlockContext>
) : Expression {

    val arguments = parseExpressionGrouping(blockContext).expressions
    val afterArgsIndex = nextIndex()

    // Check if this is a method shorthand: obj.method() { } or obj.method() {}
    val isInObjectContext = blockContext.lastOrNull() == BlockContext.Object
    
    // Support both OpGetProperty (e.g., method()) and OpComputedPropertyName (e.g., [key]())
    val isMethodShorthand = (function is OpGetProperty || function is OpComputedPropertyName)
            && isInObjectContext
            && hasNext()

    if (isMethodShorthand) {
        // Peek at the next token without consuming it
        val nextIndex = nextIndex()
        val nextToken = if (hasNext()) next().token else null
        returnToIndex(nextIndex)

        if (nextToken == Token.Operator.Bracket.CurlyOpen) {
            // This is method shorthand: { method() { } } or { method(params) { } }
            // or { [key]() { } } for computed property names
            
            // For computed property names, we need special handling
            if (function is OpComputedPropertyName) {
                // Create a function with a temporary name, we'll update it at runtime
                val keyExpr = function.keyExpression
                val body = parseBlock(
                    scoped = false,
                    blockContext = blockContext + BlockContext.Function,
                    type = ExpectedBlockType.Block
                ) as OpBlock
                
                val params = arguments.map { it.toFunctionParam() }
                
                // Create a function with placeholder name
                val tempFunction = JSFunction(
                    name = "",  // Will be set at runtime
                    parameters = params,
                    body = body
                )
                
                // Return an expression that will set the method at runtime
                return OpComputedPropertyMethod(keyExpr, tempFunction)
            } else {
                // Standard method shorthand - function is OpGetProperty
                return OpFunctionInit(parseMethodBody(name = (function as OpGetProperty).name, arguments = arguments, blockContext = blockContext))
            }
        }
    }

    returnToIndex(afterArgsIndex)
    return OpCall(
        callable = function,
        arguments = arguments,
        isOptional = optional
    ).at(function.sourceLocation)
}

private fun ListIterator<LocatedToken>.parseMethodBody(
    name: String,
    arguments: List<Expression>,
    blockContext: List<BlockContext>
) : JSFunction {
    // At this point, we just need to parse the { body }
    // Capture the method's declaration location (its name) for stack frames.
    val fnLoc = nextSignificantLocation()
    val body = parseBlock(
        scoped = false,
        blockContext = blockContext + BlockContext.Function,
        type = ExpectedBlockType.Block
    ) as OpBlock

    // Convert arguments expressions to function parameters
    val params = arguments.map { it.toFunctionParam() }

    return JSFunction(
        name = name,
        parameters = params,
        body = body,
        sourceLocation = fnLoc
    )
}


private fun ListIterator<LocatedToken>.parseInOperator(subject : Expression, precedence: Int) : Expression {
    val obj = parseStatement(maxPrecedence = precedence, blockType = ExpectedBlockType.Object)
    return OpIn(
        property = subject,
        inObject = obj
    )
}

private fun ListIterator<LocatedToken>.parseOfOperator(subject : Expression, precedence: Int) : Expression {
    val obj = parseStatement(maxPrecedence = precedence, blockType = ExpectedBlockType.Object)
    return OpIn(
        property = subject,
        inObject = obj
    ).also { it.isForOf = true }
}

private fun ListIterator<LocatedToken>.parseInstanceOfOperator(subject : Expression, precedence: Int) : Expression {
    val obj = parseStatement(maxPrecedence = precedence,blockType = ExpectedBlockType.Object)
    return Expression {
        val o = obj(it)
        syntaxCheck(o is Constructor) {
            "Illegal usage of 'instanceof' operator"
        }
        o.isInstance(subject(it), it).js
    }
}

private fun ListIterator<LocatedToken>.parseTernary(
    condition : Expression,
    precedence: Int,
    blockContext: List<BlockContext>
) : Expression {

    // Both ternary branches are AssignmentExpressions.
    checkExpressionStart()

    val body = parseStatement(
        blockContext = blockContext + BlockContext.Ternary,
        blockType = ExpectedBlockType.Block
    )

    val n = nextSignificant()

    syntaxCheck(n is Token.Operator.Colon){
        "Unexpected end of input: $n (after $body)"
    }

    return OpIfCondition(
        condition = condition,
        onTrue = body,
        onFalse = run {
            checkExpressionStart()
            parseStatement(
                blockContext = blockContext,
                blockType = ExpectedBlockType.Block
            )
        },
        expressible = true
    )
}

private fun ListIterator<LocatedToken>.parseClass() : OpClassInit {

    val i = nextIndex()
    val name = nextSignificant().let {
        if (it is Token.Identifier){
            it.identifier
        } else {
            returnToIndex(i)
            ""
        }
    }

    val extends = if (eat(Token.Identifier.Keyword.Extends)) {
        parseStatement(blockType = ExpectedBlockType.Object)
    } else null

    syntaxCheck(eat(Token.Operator.Bracket.CurlyOpen)) {
        "Invalid class declaration"
    }

    val staticMembers = mutableListOf<StaticClassMember>()
    val properties = mutableMapOf<JsAny?, Expression>()
    var construct : JSFunction? = null

    while (!eat(Token.Operator.Bracket.CurlyClose)) {
        val token = nextSignificant()

        when {
            token is Token.Identifier && nextIsInstance<Token.Operator.Bracket.RoundOpen>() -> {
                val f = parseFunction(
                    name = token.identifier,
                    blockContext = listOf(BlockContext.Class)
                )
                if (token.identifier == "constructor"){
                    syntaxCheck(construct == null){
                        "A class may only have one constructor"
                    }
                    construct = f
                }
                properties[token.identifier.js] = OpFunctionInit(f)
            }

            token is Token.Identifier.Property && token.identifier == "static" -> {
                staticMembers.add(parseStaticClassMember())
            }

            token is Token.Identifier -> {
                prevSignificant()
                when (val statement = parseStatement(blockContext = listOf(BlockContext.Class), blockType = ExpectedBlockType.None)) {
                    is OpAssign -> properties[statement.variableName.js] = statement.assignableValue
                    is OpGetProperty -> properties[statement.name.js] = OpConstant(Undefined)
                    is OpGetter -> {
                        // Store getter as a special marker, will be processed in OpClassInit
                        properties["${Constants.getterPrefix}${statement.value.name}".js] = statement
                    }
                    is OpSetter -> {
                        // Store setter as a special marker, will be processed in OpClassInit
                        properties["${Constants.setterPrefix}${statement.value.name}".js] = statement
                    }
                    else -> throw SyntaxError("Invalid class member")
                }
            }
            else -> throw  SyntaxError("Invalid class declaration")
        }
    }

    return OpClassInit(
        name = name,
        extends = extends,
        properties = properties,
        static = staticMembers/*.reversed()*/.associateBy { it.name.js },
        construct = construct
    )
}

private fun ListIterator<LocatedToken>.parseStaticClassMember() : StaticClassMember {

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


private fun ListIterator<LocatedToken>.parseSwitch(blockContext: List<BlockContext>) : Expression {
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

private fun ListIterator<LocatedToken>.parseForLoop(parentBlockContext: List<BlockContext>): Expression {

    syntaxCheck(nextSignificant() is Token.Operator.Bracket.RoundOpen) {
        "For loop must be followed by '('"
    }

    val assign = if (eat(Token.Operator.SemiColon)) {
        null
    }
    else {
        parseBlock(scoped = false, blockContext = emptyList())
    }

    // for (x in y) or for (x of y)
    if (assign is OpBlock) {
        val opIn = assign.expressions.singleOrNull() as? OpIn
        if (opIn != null) {
            // Use isForOf flag to determine loop type
            return if (opIn.isForOf) {
                parseForOfLoop(opIn, parentBlockContext)
            } else {
                parseForInLoop(opIn, parentBlockContext)
            }
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

    val increment = if (eat(Token.Operator.Bracket.RoundClose)) {
        null
    } else {
        parseBlock(scoped = false, blockContext = emptyList())
    }

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

private fun ListIterator<LocatedToken>.parseForInLoop(opIn: OpIn, parentBlockContext : List<BlockContext>) : Expression {
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
        assignableValue = OpConstant(Undefined),
        merge = null
    )

    return OpForInLoop(
        prepare = prepare,
        assign = { r, v -> r.set(prepare.variableName.js, v, null) },
        inObject = opIn.inObject,
        body = parseBlock(blockContext = parentBlockContext + BlockContext.Loop)
    )
}

private fun ListIterator<LocatedToken>.parseForOfLoop(opIn: OpIn, parentBlockContext: List<BlockContext>): Expression {
    syntaxCheck(nextSignificant() is Token.Operator.Bracket.RoundClose) {
        "Invalid for loop"
    }

    // Convert opIn.property to a Destruction for handling both simple and destructuring cases
    val dest = opIn.property.asDestruction()

    // assign function: for destructuring, use Destruction.destruct()
    // Note: variableType = null means assignment (not declaration)
    val assign: suspend (ScriptRuntime, JsAny?) -> Unit = { r, v ->
        dest.destruct(
            obj = v,
            variableType = null,
            runtime = r,
            default = null
        )
    }

    val body = parseBlock(blockContext = parentBlockContext + BlockContext.Loop)
    return OpForOfLoop(
        assign = assign,
        iterable = opIn.inObject,
        body = body
    )
}

private fun ListIterator<LocatedToken>.parseWhileLoop(parentBlockContext: List<BlockContext>): Expression {
    return OpWhileLoop(
        condition = parseExpressionGrouping(),
        body = parseBlock(blockContext = parentBlockContext + BlockContext.Loop),
    )
}

private fun ListIterator<LocatedToken>.parseDoWhileLoop(blockContext: List<BlockContext>) : Expression {
    val body = parseBlock(
        type = ExpectedBlockType.Block,
        blockContext = blockContext + BlockContext.Loop,
        scoped = false // while condition should have the same scope with body
    )

    // Issue #22 (do/while variant): tolerate a stray `;` between the do-body and
    // `while` (e.g. `do x=1; while (false)`). Only consume the `;` when `while`
    // actually follows, so `do x=1; y` keeps running `y`.
    syntaxCheck(eatKeywordAfterOptionalSemicolon(Token.Identifier.Keyword.While)) {
        "Missing while condition in do/while block"
    }
    val condition = parseExpressionGrouping()

    return OpDoWhileLoop(
        condition = condition,
        body = body as OpBlock,
    )
}

private fun ListIterator<LocatedToken>.parseAsync(blockContext: List<BlockContext>): Expression {
    // Check if we're in object context and the next token is an identifier (method shorthand)
    val isInObjectContext = blockContext.lastOrNull() == BlockContext.Object
    
    if (isInObjectContext && nextIsInstance<Token.Identifier.Property>()) {
        // This is async method shorthand: { async method() {} }
        // Capture the method's declaration location (its name) for stack frames.
        val fnLoc = nextSignificantLocation()
        val methodName = (nextSignificant() as Token.Identifier.Property).identifier
        // Parse the function parameters and body
        val touple = parseStatement(blockType = ExpectedBlockType.None)
        syntaxCheck(touple is OpTouple) {
            "Invalid function declaration"
        }
        val args = touple.expressions.map(Expression::toFunctionParam)
        
        val block = parseBlock(
            scoped = false,
            blockContext = blockContext + BlockContext.Function,
            type = ExpectedBlockType.Block
        )
        
        val func = JSFunction(
            name = methodName,
            parameters = args,
            body = block,
            isAsync = true,
            sourceLocation = fnLoc
        )
        
        return OpColonAssignment(
            key = methodName.js,
            value = OpFunctionInit(func)
        )
    }
    
    val subject = parseStatement(blockType = ExpectedBlockType.Object)

    // Support both async function expressions
    return when (subject) {
        is OpFunctionInit -> {
            syntaxCheck(!subject.function.isAsync) {
                "Illegal usage of 'async' keyword"
            }
            OpFunctionInit(subject.function.copy(isAsync = true))
        }
        else -> {
            throw SyntaxError("Illegal usage of 'async' keyword")
        }
    }
}

private fun ListIterator<LocatedToken>.parseAwait(): Expression {
    val subject = parseStatement(blockType = ExpectedBlockType.Object)

    return Expression {

        if (!it.isSuspendAllowed) {
            throw JSError("Await is not allowed in current context")
        }

        val job = subject(it)?.toKotlin(it)
        it.typeCheck(job is Job){
            "$job is not a Promise".js
        }

        if (job is Deferred<*>){
            job.await() as JsAny?
        } else {
            job.joinSuccess()
            Undefined
        }
    }
}

private fun ListIterator<LocatedToken>.parseTryCatch(blockContext: List<BlockContext>): Expression {
    val tryBlock = parseBlock(type = ExpectedBlockType.Block, blockContext = blockContext)
    // Issue #22 (try/catch variant): tolerate a stray `;` between the try-block and
    // `catch`/`finally` (e.g. `try { } ; catch (e) ...`). Only consume the `;` when
    // the continuation keyword actually follows, so `try { } ; x` keeps running `x`.
    val catchBlock = if (eatKeywordAfterOptionalSemicolon(Token.Identifier.Keyword.Catch)) {
        if (eat(Token.Operator.Bracket.RoundOpen)) {
            val next = nextSignificant()
            syntaxCheck(next is Token.Identifier && eat(Token.Operator.Bracket.RoundClose)) {
                "Invalid syntax after 'catch'"
            }
            next.identifier
        } else {
            null
        } to parseBlock(
            type = ExpectedBlockType.Block,
            blockContext = blockContext
        )
    } else null

    val finallyBlock = if (eatKeywordAfterOptionalSemicolon(Token.Identifier.Keyword.Finally)) {
        parseBlock(type = ExpectedBlockType.Block, blockContext = blockContext)
    } else null

    return OpTryCatch(
        tryBlock = tryBlock,
        catchVariableName = catchBlock?.first,
        catchBlock = catchBlock?.second,
        finallyBlock = finallyBlock
    )
}

private fun ListIterator<LocatedToken>.parseArrowFunction(blockContext: List<BlockContext>, args: Expression) : JSFunction {
    val fArgs = when(args){
        is OpTouple -> args.expressions
        else -> listOf(args)
    }.map(Expression::toFunctionParam)

    validateFunctionParams(fArgs,true)

    // A concise arrow body is an AssignmentExpression, so `() => if (true) '1'` is a
    // SyntaxError. A block body starts with `{`, which is not a keyword, so guarding
    // unconditionally leaves `() => { if (...) ... }` untouched.
    checkExpressionStart()

    // Capture the arrow function's location (its body start) for stack frames.
    val fnLoc = nextSignificantLocation()

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
        parameters = fArgs,
        body = lambda.copy(isExpressible = !lambda.isSurroundedWithBraces),
        isArrow = true,
        sourceLocation = fnLoc
    )
}

private fun ListIterator<LocatedToken>.parseFunction(
    name: String? = null,
    blockContext: List<BlockContext>,
    fnLoc: SourceLocation? = null,
) : JSFunction {

    // Use the explicitly provided declaration location (captured at the
    // `function` keyword by the caller) when available; otherwise peek the next
    // significant token (used for arrow / method / async expressions where the
    // keyword isn't in hand). This location drives the (file:line:col) shown on
    // programmatically-invoked callback stack frames.
    val effectiveLoc = fnLoc ?: nextSignificantLocation()

    val actualName = name ?: run {
        if (nextIsInstance<Token.Identifier.Property>()) {
            (nextSignificant() as Token.Identifier.Property).identifier
        } else {
            ""
        }
    }

    val touple = parseStatement(blockType = ExpectedBlockType.None)

    syntaxCheck(touple is OpTouple) {
        "Invalid function declaration"
    }

    val args = touple.expressions.map(Expression::toFunctionParam)

    validateFunctionParams(args, false)

    val block = parseBlock(
        scoped = false,
        blockContext = blockContext + BlockContext.Function,
        type = ExpectedBlockType.Block
    )

    return JSFunction(
        name = actualName,
        parameters = args,
        body = block,
        sourceLocation = effectiveLoc
    )
}

private fun ListIterator<LocatedToken>.parseImport() : Expression {

    val entries = buildList {
        while (true) {
            val i = nextIndex()
            when (nextSignificant()) {
                is Token.Operator.Bracket.CurlyOpen -> {
                    if (!eat(Token.Operator.Bracket.CurlyClose)) {
                        do {
                            add(parseImportEntry(true))
                        } while (eat(Token.Operator.Comma))

                        val close = nextSignificant()
                        syntaxCheck(close is Token.Operator.Bracket.CurlyClose) {
                            "Invalid import: '}' was expected but got '$close'"
                        }
                    }
                }
                else -> {
                    returnToIndex(i)
                    add(parseImportEntry(false))
                }
            }

            if (!eat(Token.Operator.Comma)){
                break
            }
        }
    }

    val fromKeyword = nextSignificant()

    syntaxCheck(fromKeyword is Token.Identifier && fromKeyword.identifier == "from") {
        "Invalid import: 'from' was expected"
    }

    val moduleName = parseStatement(blockType = ExpectedBlockType.Block)

    return OpImport(
        entries = entries,
        fromModule = moduleName
    )
}

private fun ListIterator<LocatedToken>.parseImportEntry(
    isInBrackets : Boolean
) : ImportEntry {
    return if (isInBrackets) {
        val import = when(val i = nextSignificant()){
            is Token.Identifier -> i.identifier
            is Token.Str -> i.value
            else -> throw SyntaxError( "Invalid import: unexpected token $i")
        }
        val i = nextIndex()
        val n = nextSignificant()
        val alias = if (n is Token.Identifier && n.identifier == "as"){
            nextSignificant().identifier()
        } else {
            returnToIndex(i)
            null
        }
        if (import == "default") {
            syntaxCheck(alias != null){
                "Invalid import: alias for default import was expected"
            }
            ImportEntry.Default(alias)
        } else {
            ImportEntry.Named(import, alias)
        }
    } else {
        when (val n = nextSignificant()){
            is Token.Operator.Arithmetic.Mul -> {
                val n = nextSignificant()
                syntaxCheck(n is Token.Identifier && n.identifier == "as"){
                    "Invalid import: unexpected token $n"
                }
                ImportEntry.Star(nextSignificant().identifier())
            }
            is Token.Identifier -> ImportEntry.Default(n.identifier)
            else -> throw SyntaxError("Invalid import: unexpected token $n")
        }
    }
}

private fun Token.identifier() : String {
    syntaxCheck(this is Token.Identifier){
        "Invalid import: unexpected token $this"
    }
    return identifier
}

private fun ListIterator<LocatedToken>.parseExport(isDefault : Boolean = false) : Expression {

    if (!isDefault){
        val i = nextIndex()

        if (nextSignificant() == Token.Identifier.Keyword.Default){
            return parseExport(isDefault = true)
        } else {
            returnToIndex(i)
        }
    }

    return when {
        isDefault -> parseExportDeclaration(true)
        eat(Token.Operator.Arithmetic.Mul) -> {
            val index = nextIndex()
            val (alias, declareProperty) = parseExportAliasAndType()
            if (alias == null) {
                returnToIndex(index)
            }

            val from = nextSignificant()
            syntaxCheck(from is Token.Identifier && from.identifier == "from"){
                "Invalid export: 'from' was unexpected but got $from"
            }
            val fromModule = parseStatement(blockType = ExpectedBlockType.None)

            OpAggregatingExport(
                exports = listOf(AggregatingExportEntry.Star(alias, declareProperty)),
                fromModule = fromModule
            )
        }
        eat(Token.Operator.Bracket.CurlyOpen) -> {
            val exports = buildList {
                while (!eat(Token.Operator.Bracket.CurlyClose)) {
                    add(parseAggregatingExportEntry())
                }
            }
            val from = nextSignificant()

            syntaxCheck(from is Token.Identifier && from.identifier == "from"){
                "Invalid export: 'from' was unexpected but got $from"
            }
            val fromModule = parseStatement(blockType = ExpectedBlockType.None)

            OpAggregatingExport(exports, fromModule)
        }
        else -> parseExportDeclaration()
    }
}

private fun ListIterator<LocatedToken>.parseExportAliasAndType() : Pair<String?, Boolean> {
    val n = nextSignificant()
    syntaxCheck(n is Token.Identifier){
        "Invalid export"
    }

    return if (n.identifier == "as"){
        nextSignificant().asExportAliasAndType()
    } else {
        null to false
    }
}

private fun Token.asExportAliasAndType() : Pair<String?, Boolean> {
    return when (this){
        is Token.Identifier -> identifier to true
        is Token.Str -> value to false
        else -> throw SyntaxError( "Invalid export: unexpected token $this")
    }
}

private fun ListIterator<LocatedToken>.parseAggregatingExportEntry() : AggregatingExportEntry {
    val import = nextSignificant()

    syntaxCheck(import is Token.Identifier) {
        "Invalid export"
    }

    val (alias, declareProperty) = if (eat(Token.Operator.Comma) || nextIsInstance<Token.Operator.Bracket.CurlyClose>()) {
        null to false
    } else {
        parseExportAliasAndType()
    }

    return AggregatingExportEntry.Single(
        import = if (import.identifier == "default") null else import.identifier,
        alias = alias,
        assignPropertyForAlias = declareProperty
    )
}


private fun ListIterator<LocatedToken>.parseExportDeclaration(isDefault: Boolean = false) : Expression {
    val expr = parseStatement(blockType = ExpectedBlockType.Block)

    if (isDefault){
        return OpExport(null, expr)
    }

    val (name, property) = when (expr) {
        is OpFunctionInit -> expr.function.name to expr
        is OpClassInit -> expr.name to expr
        is OpAssign -> expr.variableName to expr
        is OpGetProperty -> expr.name to expr
        else -> error("Invalid export")
    }

    return OpExport(name, property)
}

private fun ListIterator<LocatedToken>.parseBlock(
    scoped: Boolean = true,
    type: ExpectedBlockType = ExpectedBlockType.None,
    isExpressible: Boolean = false,
    allowCommaSeparator : Boolean = true,
    isStatementList: Boolean = false,
    blockContext: List<BlockContext>,
): Expression {
    var hoistedIndex = 0

    var isSurroundedWithBraces = false
    val list = buildList {
        /**
         * Parse a flat statement list (either surrounded by `{`...`}` or the bare
         * top level).  The body — hoisting, separator consumption, and ASI — is
         * identical; only the loop condition, the fallback syntax-check terminator,
         * and the context passed to `parseStatement` differ.
         */
        fun parseStatementList(
            stmtContext: List<BlockContext>,
            shouldContinue: () -> Boolean,
            isNaturalTerminator: () -> Boolean,
        ) {
            while (shouldContinue()) {
                val expr = parseStatement(
                    blockContext = stmtContext,
                    blockType = ExpectedBlockType.None,
                    isBlockAnchor = true
                )
                // Index of the first token *after* this statement. Used below to
                // detect ASI: if the previous statement ended with a block-closing
                // '}' and the next token is not an explicit separator, ECMAScript
                // inserts an implicit ';' (ASI rule #2).
                val afterStmtIndex = nextIndex()

                // hoisted
                when (expr) {
                    is OpClassInit -> {
                        val assign = OpAssign(
                            type = VariableType.Local,
                            variableName = expr.name,
                            assignableValue = expr,
                            merge = null
                        )
                        add(
                            index = hoistedIndex++,
                            element = Expression { assign(it); Undefined }
                        )
                    }

                    is OpFunctionInit if !expr.function.isArrow -> {
                        // In object context, OpFunctionInit is a method, not a hoisted function
                        if (type == ExpectedBlockType.Object) {
                            add(expr)
                        } else {
                            val name = expr.function.name

                            syntaxCheck(name.isNotBlank()) {
                                "Function statements require a function name"
                            }

                            val assign = OpAssign(
                                type = VariableType.Local,
                                variableName = name,
                                assignableValue = expr,
                                merge = null
                            )
                            add(
                                index = hoistedIndex++,
                                element = Expression { assign(it); Undefined }
                            )
                        }
                    }

                    is OpExport if expr.property is OpClassInit -> {
                        val assign = OpAssign(
                            type = VariableType.Local,
                            variableName = expr.property.name,
                            assignableValue = expr.property,
                            merge = null
                        )
                        add(
                            index = hoistedIndex++,
                            element = Expression {
                                expr(it)
                                assign(it);
                                Undefined
                            }
                        )
                    }

                    is OpExport if expr.property is OpFunctionInit && !expr.property.function.isArrow -> {

                        val name = expr.property.function.name

                        syntaxCheck(name.isNotBlank()) {
                            "Function statements require a function name"
                        }

                        val assign = OpAssign(
                            type = VariableType.Local,
                            variableName = name,
                            assignableValue = expr.property,
                            merge = null
                        )
                        add(
                            index = hoistedIndex++,
                            element = Expression {
                                expr(it)
                                assign(it);
                                Undefined
                            }
                        )
                    }

                    is OpImport -> add(hoistedIndex++, expr)
                    else -> add(expr)
                }
                var hasSeparator = false
                while (hasNext()) {
                    val next = next().token
                    if (next !is Token.NewLine && next !is Token.Operator.SemiColon && next !is Token.Operator.Comma) {
                        previous()
                        break
                    }
                    hasSeparator = true
                }
                // ASI (automatic semicolon insertion), rule #2: when a statement is
                // not followed by an explicit separator (';', newline or ',') but the
                // previous significant token was a block-closing '}', ECMAScript
                // inserts an implicit ';'. This lets constructs such as
                // `try { } catch { } nextStmt` (written on one line, without ';')
                // parse, matching V8.
                //
                // Restricted to statement-level contexts: object literals and class
                // bodies separate members with commas, not ASI, so a '}' there must
                // NOT start a fresh statement (e.g. `{ a(){} b(){} }` stays a syntax
                // error, as in V8).
                val asiAfterBlock = !hasSeparator && hasNext() &&
                    BlockContext.Object !in stmtContext &&
                    BlockContext.Class !in stmtContext &&
                    run {
                        var ok = false
                        if (afterStmtIndex > 0) {
                            // Rewind to just before the next statement, step back over
                            // any newlines to the last token of the previous statement,
                            // then restore the iterator to afterStmtIndex.
                            while (nextIndex() > afterStmtIndex) previous()
                            if (hasPrevious()) {
                                var lt = previous()
                                while (lt.token is Token.NewLine && hasPrevious()) {
                                    lt = previous()
                                }
                                // ASI rule #2: insert an implicit ';' before the next
                                // token when it is preceded by a block-closing '}' (any
                                // statement) or a ')' belonging to a control statement
                                // such as `do/while` (whose body is not a block).
                                // Expression statements (`a b`, `1 2`, `a() b()`,
                                // `(1) 2`, `x = 1 y = 2`) stay SyntaxErrors, matching V8.
                                ok = when (lt.token) {
                                    is Token.Operator.Bracket.CurlyClose -> true
                                    is Token.Operator.Bracket.RoundClose -> expr is OpDoWhileLoop
                                    else -> false
                                }
                            }
                            while (nextIndex() < afterStmtIndex) next()
                        }
                        ok
                    }
                syntaxCheck(
                    hasSeparator || asiAfterBlock || expr is OpCase || isNaturalTerminator()
                ) {
                    unexpected(next().token::class.simpleName.orEmpty())
                }
            }
        }

        // For a real statement list (top-level unwrapped scripts) a leading `{`
        // is NOT the script's surrounding brace — it starts a block *statement*.
        // Eating it here (with `scoped` inherited from the caller, which is
        // `false` at the top level) would make the block's `let`/`const`
        // declarations leak into the surrounding scope, and any statements
        // after the closing `}` would be silently dropped (e.g.
        // `{ const t = 1; }\nt()` must throw ReferenceError, and
        // `{ f(); } g()` must still run `g()`). The brace-less statement-list
        // branch below parses a leading `{` via parseStatement -> parseBlock
        // with its own (scoped) scope.
        if (!isStatementList && eat(Token.Operator.Bracket.CurlyOpen)) {
            isSurroundedWithBraces = true
            val context = if (type == ExpectedBlockType.Object)
                blockContext + BlockContext.Object
            else blockContext

            parseStatementList(
                stmtContext = context,
                shouldContinue = { !nextIsInstance<Token.Operator.Bracket.CurlyClose>() },
                isNaturalTerminator = { nextIsInstance<Token.Operator.Bracket.CurlyClose>() },
            )

            check(nextSignificant() is Token.Operator.Bracket.CurlyClose) {
                "} was expected"
            }
        } else {

            // Skip leading `new` keywords in comma-separated expression lists
            // (for-loop heads / argument lists). For a real statement list (top-level
            // unwrapped scripts) `new` starts a `new`-expression statement and must
            // NOT be consumed here, otherwise `new Number(1)` degrades to the plain
            // call `Number(1)` (returning a primitive instead of a wrapper object).
            if (!isStatementList) {
                while (eat(Token.Operator.New)) {
                    //skip
                }
            }
            if (isStatementList) {
                parseStatementList(
                    stmtContext = blockContext,
                    shouldContinue = { hasNext() },
                    isNaturalTerminator = { !hasNext() },
                )
            } else {
                do {
                    add(parseStatement(blockContext, blockType = type))
                } while (allowCommaSeparator && eat(Token.Operator.Comma))
            }
        }
    }

    return if (
        type != ExpectedBlockType.Block
        && isSurroundedWithBraces
        && list.fastAll {
            it is OpColonAssignment //  { a : 'b' }
                    || it is OpSpread //  { ...obj }
                    || it is PropertyAccessorFactory //  { get x(){} }
                    || it is OpGetProperty // { x } - property shorthand
                    || it is OpFunctionInit // { method() {} } - method shorthand
                    || it is OpComputedPropertyMethod // { [key]() {} } - computed property method
        }
    ) {
        // Transform list to handle ES6 shorthand properties and methods
        val transformedList = list.map { expr ->
            when (expr) {
                // Property shorthand: { x } -> { x: x }
                is OpGetProperty -> OpColonAssignment(
                    key = expr.name.js,
                    value = expr
                )
                // Method shorthand: { method() {} } already handled by parseFunctionCall
                // but OpFunctionInit at object context needs to be wrapped as method
                is OpFunctionInit -> OpColonAssignment(
                    key = expr.function.name.js,
                    value = expr
                )
                else -> expr
            }
        }
        OpMakeObject(transformedList)
    } else {
        val (isStrict, exprs) = if ((list.firstOrNull() as? OpConstant)?.value?.toString() == "use strict") {
            true to list.drop(1)
        } else {
            false to list
        }

        OpBlock(
            expressions = exprs,
            isScoped = scoped,
            isStrict = isStrict,
            isExpressible = isExpressible,
            isSurroundedWithBraces = isSurroundedWithBraces
        )
    }
}

private fun ListIterator<LocatedToken>.parseVariable(type: VariableType) : Expression {
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
                    assignableValue = OpConstant(Undefined),
                    merge = null
                )

                is OpDestructAssign -> OpDestructAssign(
                    destruction = expr.destruction,
                    variableType = type,
                    value = expr.value
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
internal suspend inline fun ScriptRuntime.typeCheck(value: Boolean, lazyMessage: () -> JsAny) {
    contract { returns() implies value }

    if (!value) {
        typeError(lazyMessage)
    }
}

internal suspend inline fun ScriptRuntime.typeError(lazyMessage: () -> JsAny) : Nothing {
    throw makeTypeError(lazyMessage)
}

@OptIn(ExperimentalContracts::class)
internal suspend inline fun ScriptRuntime.referenceCheck(value: Boolean, lazyMessage: () -> JsAny) {
    contract { returns() implies value }

    if (!value) {
        referenceError(lazyMessage)
    }
}

internal suspend inline fun ScriptRuntime.referenceError(lazyMessage: () -> JsAny) : Nothing {
    throw makeReferenceError(lazyMessage)
}

internal suspend inline fun ScriptRuntime.makeReferenceError(lazyMessage: () -> JsAny) : ReferenceError {
    return findJsRoot().ReferenceError
        .construct(lazyMessage().listOf(), this) as ReferenceError
}

internal suspend inline fun ScriptRuntime.makeTypeError(lazyMessage: () -> JsAny) : Throwable {
    return findJsRoot().TypeError
        .construct(lazyMessage().listOf(), this) as Throwable
}


internal fun Expression.isAssignable() : Boolean {
    return this is OpGetProperty ||
            this is OpIndex && receiver is OpGetProperty
}


