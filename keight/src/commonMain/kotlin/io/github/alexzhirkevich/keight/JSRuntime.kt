package io.github.alexzhirkevich.keight

import io.github.alexzhirkevich.keight.expressions.JSErrorFunction
import io.github.alexzhirkevich.keight.expressions.JSReferenceErrorFunction
import io.github.alexzhirkevich.keight.expressions.JSSyntaxErrorFunction
import io.github.alexzhirkevich.keight.expressions.JSTypeErrorFunction
import io.github.alexzhirkevich.keight.js.JSArrayFunction
import io.github.alexzhirkevich.keight.js.JSBooleanFunction
import io.github.alexzhirkevich.keight.js.JSBooleanWrapper
import io.github.alexzhirkevich.keight.js.JSDateFunction
import io.github.alexzhirkevich.keight.js.JSError
import io.github.alexzhirkevich.keight.js.JSFunctionFunction
import io.github.alexzhirkevich.keight.js.JSKotlinError
import io.github.alexzhirkevich.keight.js.JSMapFunction
import io.github.alexzhirkevich.keight.js.JSMath
import io.github.alexzhirkevich.keight.js.JSNumberFunction
import io.github.alexzhirkevich.keight.js.JSON
import io.github.alexzhirkevich.keight.js.JSObject
import io.github.alexzhirkevich.keight.js.JSObjectFunction
import io.github.alexzhirkevich.keight.js.JSPromiseFunction
import io.github.alexzhirkevich.keight.js.JSPromiseWrapper
import io.github.alexzhirkevich.keight.js.JSPropertyAccessor
import io.github.alexzhirkevich.keight.js.JSSetFunction
import io.github.alexzhirkevich.keight.js.JSStringFunction
import io.github.alexzhirkevich.keight.js.JSSymbol
import io.github.alexzhirkevich.keight.js.JSSymbolFunction
import io.github.alexzhirkevich.keight.js.JsAny
import io.github.alexzhirkevich.keight.js.JsArrayWrapper
import io.github.alexzhirkevich.keight.js.JsConsole
import io.github.alexzhirkevich.keight.js.JsMapWrapper
import io.github.alexzhirkevich.keight.js.JsNumberWrapper
import io.github.alexzhirkevich.keight.js.JsSetWrapper
import io.github.alexzhirkevich.keight.js.JsStringWrapper
import io.github.alexzhirkevich.keight.js.OpArgOmitted
import io.github.alexzhirkevich.keight.js.argOrElse
import io.github.alexzhirkevich.keight.js.defaults
import io.github.alexzhirkevich.keight.js.func
import io.github.alexzhirkevich.keight.js.interpreter.NumberFormat
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck
import io.github.alexzhirkevich.keight.js.interpreter.typeError
import io.github.alexzhirkevich.keight.js.listOf
import io.github.alexzhirkevich.keight.js.numberMethods
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.math.absoluteValue

public open class JSRuntime(
    context: CoroutineContext,
    override val isSuspendAllowed: Boolean = true,
    override val isStrict: Boolean = false,
    public var console: Console = DefaultConsole,
) : DefaultRuntime() {

    override val coroutineContext: CoroutineContext =
        context + SupervisorJob(context[Job]).also {
            if (!isSuspendAllowed) {
                it.cancel()
            }
        } + CoroutineName("JS coroutine")

    internal lateinit var Number: JSNumberFunction
    internal lateinit var Object: JSObjectFunction
    internal lateinit var Function: JSFunctionFunction
    internal lateinit var Array: JSArrayFunction
    internal lateinit var Map: JSMapFunction
    internal lateinit var Set: JSSetFunction
    internal lateinit var String: JSStringFunction
    internal lateinit var Boolean: JSBooleanFunction
    internal lateinit var Promise: JSPromiseFunction
    internal lateinit var Symbol: JSSymbolFunction
    internal lateinit var Error: JSErrorFunction
    internal lateinit var TypeError: JSTypeErrorFunction
    internal lateinit var ReferenceError: JSReferenceErrorFunction
    internal lateinit var SyntaxError: JSSyntaxErrorFunction
    internal lateinit var Date: JSDateFunction

    internal var jobsCounter = 1L
    internal val jobsMap = mutableMapOf<Long, Job>()

    final override val thisRef: JSObject = RuntimeObject { this }

    init {
        init()
    }

    /**
     * Restore runtime to its initial state. Cancels all pending promises.
     * */
    override fun reset() {
        super.reset()
        init()

        coroutineContext.cancelChildren()
        jobsMap.clear()
        jobsCounter = 1
    }

    private fun init() {
        Number = JSNumberFunction()
        Object = JSObjectFunction()
        Function = JSFunctionFunction()
        Array = JSArrayFunction()
        Map = JSMapFunction()
        Set = JSSetFunction()
        String = JSStringFunction()
        Boolean = JSBooleanFunction()
        Promise = JSPromiseFunction()
        Symbol = JSSymbolFunction()
        Error = JSErrorFunction()
        TypeError = JSTypeErrorFunction(Error)
        ReferenceError = JSReferenceErrorFunction(Error)
        SyntaxError = JSSyntaxErrorFunction(Error)
        Date = JSDateFunction()

        listOf(
            Number,
            Object,
            Function,
            Array,
            Map,
            Set,
            String,
            Boolean,
            Promise,
            Symbol,
            Error,
            TypeError,
            ReferenceError,
            SyntaxError,
            Date
        ).fastForEach {
            variables[it.name] = VariableType.Global to it
        }

        variables["globalThis"] = null to thisRef

        variables["Infinity"] = null to JsNumberWrapper(Double.POSITIVE_INFINITY)
        variables["NaN"] = null to JsNumberWrapper(Double.NaN)
        variables["undefined"] = null to Unit
        variables["console"] = null to JsConsole(::console)
        variables["Math"] = null to JSMath()
        variables["JSON"] = null to JSON()

        numberMethods().forEach {
            variables[it.name] = null to it
        }

        regSetTimeout()
        regSetInterval()
        regClearJob("Timeout")
        regClearJob("Interval")

        variables["eval"] = null to "eval".func("script" defaults OpArgOmitted) {
            val script = toKotlin(it.argOrElse(0) { return@func Unit }).toString()
            JavaScriptEngine(this).evaluate(script)
        }
    }

    override suspend fun get(property: Any?): Any? {
        return when (property) {
            "Infinity" -> Double.POSITIVE_INFINITY
            "NaN" -> Double.NaN
            "undefined" -> Unit
            else -> super.get(property)
        }
    }

//    final override suspend fun get(property: Any?): Any? {
//        return if (contains(property)) {
//            super.get(property)
//        } else thisRef.get(property, this)
//    }

    override suspend fun isFalse(a: Any?): Boolean {
        return a == null
                || a == false
                || a is Unit
                || a is CharSequence && a.isEmpty()
                || a is Number && a.toDouble().let { it == 0.0 || it.isNaN() }
                || (a as? Wrapper<*>)?.value?.let { isFalse(it) } == true
    }

    override suspend fun isComparable(a: Any?, b: Any?): Boolean {

        val ka = toKotlin(a)
        val kb = toKotlin(b)

        if (ka is Number && kb is CharSequence || kb is Number && ka is CharSequence){
            return isComparable(toNumber(ka), toNumber(kb))
        }

        if (ka is Double && ka.isNaN() || kb is Double && kb.isNaN()) {
            return false
        }
        if (ka is Number && kb is Number || ka is CharSequence && kb is CharSequence) {
            return true
        }
        return false
    }

    override suspend fun compare(a: Any?, b: Any?): Int {

        val ka = toKotlin(a)
        val kb = toKotlin(b)

        return if (ka is Number || kb is Number) {
            toNumber(ka).toDouble().compareTo(toNumber(kb).toDouble())
        } else {
            ka.toString().compareTo(kb.toString())
        }
    }

    override suspend fun sum(a: Any?, b: Any?): Any? {
        var ta: Any? = a
        var tb: Any? = b

        if (ta is JSObject) {
            ta = a.ToPrimitive()
        }
        if (tb is JSObject) {
            tb = b.ToPrimitive()
        }

        if (ta is CharSequence || tb is CharSequence) {
            return ta.ToString() + tb.ToString()
        }

        val na = ta.ToNumber()
        val nb = tb.ToNumber()

        return when {
            na is Long && nb is Long -> na + nb
            else -> na.toDouble() + nb.toDouble()
        }
    }

    override suspend fun sub(a: Any?, b: Any?): Any? {
        return jssub(a?.numberOrThis(), b?.numberOrThis())
    }

    override suspend fun mul(a: Any?, b: Any?): Any? {
        return jsmul(a?.numberOrThis(), b?.numberOrThis())
    }

    override suspend fun div(a: Any?, b: Any?): Any? {
        return jsdiv(a?.numberOrThis(), b?.numberOrThis())
    }

    override suspend fun mod(a: Any?, b: Any?): Any? {
        return jsmod(a?.numberOrThis(), b?.numberOrThis(throwForObjects = true))
    }

    override suspend fun inc(a: Any?): Any? {
        return jsinc(a?.numberOrThis())
    }

    override suspend fun dec(a: Any?): Any? {
        return jsdec(a?.numberOrThis())
    }

    override suspend fun neg(a: Any?): Any? {
        return jsneg(a?.numberOrThis())
    }

    override suspend fun pos(a: Any?): Any? {
        return jspos(a?.numberOrThis())
    }

    override suspend fun toNumber(a: Any?): Number {
        return a.ToNumber()
    }

    override fun fromKotlin(a: Any?): Any? {
        return when (a) {
            is Wrapper<*> -> a
            is Boolean -> JSBooleanWrapper(a)
            is Number -> JsNumberWrapper(a)
            is UByte -> JsNumberWrapper(a.toLong())
            is UShort -> JsNumberWrapper(a.toLong())
            is UInt -> JsNumberWrapper(a.toLong())
            is ULong -> JsNumberWrapper(a.toLong())
            is Map<*, *> -> JsMapWrapper(
                a.map { fromKotlin(it.key) to fromKotlin(it.value) }.toMap().toMutableMap()
            )

            is Set<*> -> JsSetWrapper(a.map(::fromKotlin).toMutableSet())
            is List<*> -> JsArrayWrapper(a.map(::fromKotlin).toMutableList())
            is CharSequence -> JsStringWrapper(a.toString())
            is Job -> JSPromiseWrapper(a)
            is Throwable -> if (a is JSError) a else JSKotlinError(a)
            else -> a
        }
    }

    override fun toKotlin(a: Any?): Any? {
        return when (a) {
            is JsMapWrapper -> a.value.map { toKotlin(it.key) to toKotlin(it.value) }.toMap()
            is JsSetWrapper -> a.value.map(::toKotlin).toSet()
            is JsArrayWrapper -> a.value.fastMap(::toKotlin)
            is Wrapper<*> -> toKotlin(a.value)
            is Int -> a.toLong()
            is Float -> a.toDouble()
            else -> a
        }
    }

    internal tailrec suspend fun Any?.numberOrNull(
        withNaNs : Boolean = true,
        throwForObjects : Boolean = false
    ) : Number? = when(this) {
        null -> 0L
        true -> 1L
        false -> 0L

        is CharSequence -> when {
            isBlank() -> 0L
            withNaNs -> {
                val s = trim().toString()
                    .trimStart('\n', '+')
                    .trimEnd('\n')
                    .lowercase()

                NumberFormat.entries.forEach {
                    val p = "0${it.prefix}"
                    if (it.prefix != null && s.startsWith(p)) {
                        return s.removePrefix(p).toLong(it.radix)
                    }
                }

                s.toLongOrNull() ?: s.toDoubleOrNull()
            }

            else -> null
        }

        is Byte -> toLong()
        is UByte -> toLong()
        is Short -> toLong()
        is UShort -> toLong()
        is Int -> toLong()
        is UInt -> toLong()
        is ULong -> toLong()
        is Float -> toDouble()
        is Long -> this
        is Double -> this
        is List<*> -> {
            if (withNaNs) {
                singleOrNull()?.numberOrNull(withNaNs,throwForObjects)
            } else {
                null
            }
        }

        is Wrapper<*> -> value.numberOrNull(withNaNs,throwForObjects)
        is JSObject ->  if (withNaNs) {
            toPrimitive("number", throwForObjects).numberOrNull(withNaNs, throwForObjects)
        } else {
            null
        }
        else -> null
    }

    private suspend fun Any?.numberOrThis(
        withMagic : Boolean = true,
        throwForObjects : Boolean = false
    ) : Any? = numberOrNull(withMagic, throwForObjects) ?: this

    internal suspend fun JsAny.toNumericOrThis(): Any? {

        if (this is JSSymbol){
            typeError { "Symbol cannot be converted to a number" }
        }

        get("valueOf", this@JSRuntime)?.callableOrNull()
            ?.call(this, emptyList(), this@JSRuntime)
            ?.takeIf { it !is JSObject }
            ?.let { return it }

        JSStringFunction.toString(this, this@JSRuntime)
            .toDoubleOrNull()
            .takeIf { it?.isNaN() != true }
            ?.let { return it  }

        return this
    }

    /**
     * 7.1.4
     *
     * [ToNumber (argument)](https://tc39.es/ecma262/#sec-tonumber)
     *
     * The abstract operation ToNumber takes argument argument (an ECMAScript language value) and
     * returns either a normal completion containing a Number or a throw completion. It converts
     * argument to a value of type Number.
     *
     * It performs the following steps when called:
     *
     * 1. If argument is a Number, return argument.
     * 2. If argument is either a Symbol or a BigInt, throw a TypeError exception.
     * 3. If argument is undefined, return NaN.
     * 4. If argument is either null or false, return +0𝔽.
     * 5. If argument is true, return 1𝔽.
     * 6. If argument is a String, return StringToNumber(argument).
     * 7. Assert: argument is an Object.
     * 8. Let primValue be ? ToPrimitive(argument, number).
     * 9. Assert: primValue is not an Object.
     * 10. Return ? ToNumber(primValue).
     * */
    internal tailrec suspend fun Any?.ToNumber() : Number {
        return when(this) {
            is Wrapper<*> -> value.ToNumber()
            is Byte -> toLong()
            is UByte -> toLong()
            is Short -> toLong()
            is UShort -> toLong()
            is Int -> toLong()
            is UInt -> toLong()
            is ULong -> toLong()
            is Float -> toDouble()
            is Long -> this
            is Double -> this
            is JSSymbol -> typeError { "Symbol cannot be converted to a number" }
            is Unit -> return Double.NaN
            null, false -> +0L
            true -> 1L
            is CharSequence -> StringToNumber()
            is JsAny -> ToPrimitive(S_NUMBER).ToNumber()
            else -> Double.NaN
        }
    }

    /**
     * 7.1.4.1.1
     *
     * [StringToNumber (str)](https://tc39.es/ecma262/#sec-stringtonumber)
     *
     * The abstract operation StringToNumber takes argument str (a String) and returns a Number.
     *
     * It performs the following steps when called:
     *
     * 1. Let literal be ParseText(str, StringNumericLiteral).
     * 2. If literal is a List of errors, return NaN.
     * 3. Return the StringNumericValue of literal.
     *
     * */
    internal fun CharSequence.StringToNumber() : Number {
        return if (isBlank()){
            0L
        } else {
            val s = toString()
                .trim()
                .trimStart('\n', '+')
                .trimEnd('\n')
                .lowercase()

            NumberFormat.entries.forEach {
                val p = "0${it.prefix}"
                if (it.prefix != null && s.startsWith(p)) {
                    return s.removePrefix(p).toLong(it.radix)
                }
            }

            s.toLongOrNull() ?: s.toDoubleOrNull() ?: Double.NaN
        }
    }

    /**
     * 11.1.6
     *
     * [11.1.6 Static Semantics: ParseText ( sourceText, goalSymbol )](https://tc39.es/ecma262/#sec-parsetext)
     *
     * TThe abstract operation ParseText takes arguments sourceText (a String or a sequence of
     * Unicode code points) and goalSymbol (a nonterminal in one of the ECMAScript grammars) and
     * returns a Parse Node or a non-empty List of SyntaxError objects.
     *
     * It performs the following steps when called:
     *
     * 1. If sourceText is a String, set sourceText to StringToCodePoints(sourceText).
     * 2. Attempt to parse sourceText using goalSymbol as the goal symbol, and analyse the parse
     * result for any early error conditions. Parsing and early error detection may be interleaved
     * in an implementation-defined manner.
     * 3. If the parse succeeded and no early errors were found, return the Parse Node (an instance
     * of goalSymbol) at the root of the parse tree resulting from the parse.
     * */
    internal fun CharSequence.ParseText(){

    }

    /**
     * 7.1.17
     *
     * [ToString (argument)](https://tc39.es/ecma262/multipage/abstract-operations.html#sec-tostring)
     *
     * The abstract operation ToString takes argument argument (an ECMAScript language value) and
     * returns either a normal completion containing a String or a throw completion. It converts
     * argument to a value of type String.
     *
     * It performs the following steps when called:
     *
     * 1. If argument is a String, return argument.
     * 2. If argument is a Symbol, throw a TypeError exception.
     * 3. If argument is undefined, return "undefined".
     * 4. If argument is null, return "null".
     * 5. If argument is true, return "true".
     * 6. If argument is false, return "false".
     * 7. If argument is a Number, return Number::toString(argument, 10).
     * 8. If argument is a BigInt, return BigInt::toString(argument, 10).
     * 9. Assert: argument is an Object.
     * 10. Let primValue be ? ToPrimitive(argument, string).
     * 11. Assert: primValue is not an Object.
     * 12. Return ? ToString(primValue).
     * */
    internal tailrec suspend fun Any?.ToString() : String {
        return when(this){
            is Wrapper<*> -> value.ToString()
            is CharSequence -> toString()
            is JSSymbol -> typeError { "Symbol cannot be converted to a string" }
            Unit -> "undefined"
            null, true, false, Number -> toString()
            is JSObject -> ToPrimitive(S_STRING).ToString()
            else -> toString()
        }
    }

    /**
     * 7.1.1
     *
     * [ToPrimitive (input [,preferredType])]((https://tc39.es/ecma262/#sec-toprimitive))
     *
     * The abstract operation ToPrimitive takes argument input (an ECMAScript language value) and
     * optional argument preferredType (string or number) and returns either a normal completion
     * containing an ECMAScript language value or a throw completion. It converts its input argument
     * to a non-Object type. If an object is capable of converting to more than one primitive type,
     * it may use the optional hint preferredType to favour that type.
     *
     * It performs the following steps when called:
     *
     * 1. If input is an Object, then
     *    a. Let exoticToPrim be ? GetMethod(input, %Symbol.toPrimitive%).
     *    b. If exoticToPrim is not undefined, then
     *       i. If preferredType is not present, then
     *          1. Let hint be "default".
     *       ii. Else if preferredType is string, then
     *           1. Let hint be "string".
     *       iii. Else,
     *            1. Assert: preferredType is number.
     *            2. Let hint be "number".
     *       iv. Let result be ? Call(exoticToPrim, input, « hint »).
     *       v. If result is not an Object, return result.
     *       vi. Throw a TypeError exception.
     *    c. If preferredType is not present, let preferredType be number.
     *    d. Return ? OrdinaryToPrimitive(input, preferredType).
     * 2. Return input.
     */
    internal tailrec suspend fun Any?.ToPrimitive(type: String? = S_DEFAULT) : Any? {

        return when (this) {
            is JSObject -> {
                val exoticToPrim = get(JSSymbol.toPrimitive, this@JSRuntime)?.callableOrNull()
                if (exoticToPrim != null){
                    val hint = type ?: S_DEFAULT
                    val result = exoticToPrim.call(this, hint.listOf(), this@JSRuntime)
                    typeCheck(result !is JSObject){ "Cannot convert object to primitive value" }
                    return result
                }

                return OrdinaryToPrimitive(type ?: S_NUMBER)
            }
            is Wrapper<*> -> value.ToPrimitive(type)
            else -> this
        }
    }

    /**
     * 7.1.1.1
     *
     * [OrdinaryToPrimitive(O,hint)](https://tc39.es/ecma262/#sec-ordinarytoprimitive)
     *
     * The abstract operation OrdinaryToPrimitive takes arguments O (an Object) and hint (string or
     * number) and returns either a normal completion containing an ECMAScript language value or a
     * throw completion.
     *
     * It performs the following steps when called:
     *
     * 1. If hint is string, then
     *    a. Let methodNames be « "toString", "valueOf" ».
     * 2. Else,
     *    a. Let methodNames be « "valueOf", "toString" ».
     * 3. For each element name of methodNames, do
     *    a. Let method be ? Get(O, name).
     *    b. If IsCallable(method) is true, then
     *       i. Let result be ? Call(method, O).
     *       ii. If result is not an Object, return result.
     * 4. Throw a TypeError exception.
     * */
    internal suspend fun JsAny.OrdinaryToPrimitive(hint: String?) : Any? {
        val methods = mutableListOf<Any?>(S_VALUEOF, S_TOSTRING)

        if (hint == S_STRING)
            methods.reverse()

        for (m in methods) {
            val callable = get(m, this@JSRuntime)
                ?.callableOrNull() ?: continue
            val res = callable.call(this, emptyList(), this@JSRuntime)
            if (res !is JSObject){
                return res
            }
        }

        typeError {
            "Cannot convert object to primitive value"
        }
    }

    internal suspend fun JsAny.toPrimitiveSymbolOrThis(
        type : Any? = "default",
    ) : Any? {

        get(JSSymbol.toPrimitive, this@JSRuntime)
            ?.callableOrNull()
            ?.call(this, type?.listOf() ?: emptyList(), this@JSRuntime)
            ?.let {
                typeCheck(it !is JSObject){
                    "Cannot convert object to primitive value"
                }
                return it
            }

        return this
    }
    internal suspend fun JsAny.toPrimitive(
        type : Any? = "default",
        force : Boolean = false,
        symbolOnly : Boolean = false,
    ) : Any? {

        val prim = toPrimitiveSymbolOrThis(type)
        if (prim !== this){
            return prim
        }

        return if (!symbolOnly) {
            toNumericOrThis().takeIf { it !== this }
        } else {
            null
        }
    }

    private suspend fun registerJob(
        args: List<Any?>,
        block: suspend CoroutineScope.(Callable, Long) -> Unit
    ): Long {
        val jobId = jobsCounter++

        val handler = args[0].callableOrThrow(this)
        val timeout = toNumber(args.getOrNull(1)).toLong()

        jobsMap[jobId] = launch(
            coroutineContext + CoroutineName("JS interval/timeout job $jobId")
        ) {
            block(handler, timeout)
        }.apply {
            invokeOnCompletion { jobsMap.remove(jobId) }
        }

        return jobId
    }

    private fun regSetTimeout() {
        variables["setTimeout"] = null to "setTimeout".func("handler", "timeout") {
            (findRoot() as JSRuntime).registerJob(it) { handler, timeout ->
                delay(timeout)
                handler.invoke(emptyList(), this@func)
            }
        }
    }

    private fun regSetInterval() {
        variables["setInterval"] = null to "setInterval".func("handler", "interval") {
            (findRoot() as JSRuntime).registerJob(it) { handler, timeout ->
                while (isActive) {
                    handler.invoke(emptyList(), this@func)
                    delay(timeout)
                }
            }
        }
    }

    private fun regClearJob(what: String) {
        variables[ "clear$what"] =  null to  "clear$what".func("id") {
            val id = toNumber(it.getOrNull(0)).toLong()
            jobsMap[id]?.cancel()
        }
    }
}

private class RuntimeObject(val thisRef: () -> ScriptRuntime) : JSObject {

    override suspend fun get(property: Any?, runtime: ScriptRuntime): Any? {
        return if (thisRef().contains(property)){
            thisRef().get(property)?.get(runtime)
        } else {
            super.get(property, thisRef())
        }
    }

    override suspend fun set(
        property: Any?,
        value: Any?,
        runtime: ScriptRuntime
    ) {
        thisRef().set(property, value, null)
    }

    override suspend fun defineOwnProperty(
        property: Any?,
        value: JSPropertyAccessor,
        runtime: ScriptRuntime,
        enumerable: Boolean?,
        configurable: Boolean?,
        writable: Boolean?
    ) {
        set(property, value.get(runtime), runtime)
    }

    override suspend fun hasOwnProperty(name: Any?, runtime: ScriptRuntime): Boolean {
        return thisRef().contains(name)
    }

    override suspend fun contains(property: Any?, runtime: ScriptRuntime): Boolean {
        return thisRef().contains(property)
    }


    override suspend fun delete(property: Any?, runtime: ScriptRuntime): Boolean {

        thisRef().delete(property, true)
        return true
    }


}

internal inline fun <reified T> ScriptRuntime.thisRef() : T {
    return thisRef as T
}

private fun jssub(a : Any?, b : Any?) : Any? {
    return when {
        a is Unit || b is Unit -> Double.NaN
        a is Number && b is Unit || a is Unit && b is Number -> Double.NaN
        a is Long? && b is Long? -> (a ?: 0L) - (b ?: 0L)
        a is Double? && b is Double? ->(a ?: 0.0) - (b ?: 0.0)
        a is Number? && b is Number? ->(a?.toDouble() ?: 0.0) - (b?.toDouble() ?: 0.0)
        else -> Double.NaN
    }
}

private fun jsmul(a : Any?, b : Any?) : Any? {
    return when {
        a == Unit || b == Unit -> Double.NaN
        a == null || b == null -> 0L
        a is Long && b is Long -> a*b
        a is Double && b is Double -> a*b
        a is Long && b is Long -> a * b
        a is Number && b is Number -> a.toDouble() * b.toDouble()
        a is List<*> && b is Number -> {
            a as List<Number>
            val bf = b.toDouble()
            a.fastMap { it.toDouble() * bf }
        }
        a is Number && b is List<*> -> {
            b as List<Number>
            val af = a.toDouble()
            b.fastMap { it.toDouble() * af }
        }
        else -> Double.NaN
    }
}

private fun jsdiv(a : Any?, b : Any?) : Any {
    return when {
        a is Unit || b is Unit
                || (a == null && b == null)
                || ((a as? Number)?.toDouble() == 0.0 && b == null)
                || ((b as? Number)?.toDouble() == 0.0 && a == null)
                || ((a as? CharSequence)?.toString()?.toDoubleOrNull() == 0.0 && b == null)
                || ((b as? CharSequence)?.toString()?.toDoubleOrNull() == 0.0 && a == null) -> Double.NaN
        a is Long && b is Long -> when {
            b == 0 -> a / b.toDouble()
            a % b == 0L -> a / b
            else -> a.toDouble() / b
        }
        a is Number && b is Number -> a.toDouble() / b.toDouble()
        a == null -> 0L
        b == null -> Double.POSITIVE_INFINITY

        a is List<*> && b is Number -> {
            a as List<Number>
            val bf = b.toDouble()
            a.fastMap { it.toDouble() / bf }
        }

        else -> Double.NaN
    }
}

private fun jsmod(a : Any?, b : Any?) : Any {
    return when {
        b == null || a == Unit || b == Unit -> Double.NaN
        (b as? Number)?.toDouble()?.absoluteValue?.let { it < Double.MIN_VALUE } == true -> Double.NaN
        a == null -> 0.0
//        a is Long && b is Long -> a % b
        a is Number && b is Number -> a.toDouble() % b.toDouble()
        else -> Double.NaN
    }
}


private fun jsinc(v : Any?) : Any {
    return when (v) {
        null -> 1L
        is Long -> v + 1
        is Double -> v + 1
        is Number -> v.toDouble() + 1
        else -> Double.NaN
    }
}

private fun jsdec(v : Any?) : Any {
    return when (v) {
        null -> -1L
        is Long -> v - 1
        is Double -> v - 1
        is Number -> v.toDouble() - 1
        else -> Double.NaN
    }
}

private fun jsneg(v : Any?) : Any {
    return when (v) {
        null -> -0
        0, 0L -> -0.0
        is Long -> -v
        is Number -> -v.toDouble()
        is List<*> -> {
            v as List<Number>
            v.fastMap { -it.toDouble() }
        }
        else -> Double.NaN
    }
}

private fun jspos(v : Any?) : Any {
    return when (v) {
        null -> 0
        is Long -> +v
        is Number -> +v.toDouble()
        else -> Double.NaN
    }
}

private const val S_NUMBER = "number"
private const val S_STRING = "string"
private const val S_DEFAULT = "default"
private const val S_VALUEOF = "valueOf"
private const val S_TOSTRING = "toString"