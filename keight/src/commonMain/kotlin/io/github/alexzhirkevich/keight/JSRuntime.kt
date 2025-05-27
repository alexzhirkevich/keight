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
import io.github.alexzhirkevich.keight.js.Undefined
import io.github.alexzhirkevich.keight.js.argOrElse
import io.github.alexzhirkevich.keight.js.defaults
import io.github.alexzhirkevich.keight.js.func
import io.github.alexzhirkevich.keight.js.interpreter.NumberFormat
import io.github.alexzhirkevich.keight.js.interpreter.typeCheck
import io.github.alexzhirkevich.keight.js.interpreter.typeError
import io.github.alexzhirkevich.keight.js.js
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
            variables[it.name.js()] = VariableType.Global to it
        }

        variables["globalThis".js()] = null to thisRef

        variables["Infinity".js()] = null to JsNumberWrapper(Double.POSITIVE_INFINITY)
        variables["NaN".js()] = null to JsNumberWrapper(Double.NaN)
        variables["undefined".js()] = null to Undefined
        variables["console".js()] = null to JsConsole(::console)
        variables["Math".js()] = null to JSMath()
        variables["JSON".js()] = null to JSON()

        numberMethods().forEach {
            variables[it.name.js()] = null to it
        }

        regSetTimeout()
        regSetInterval()
        regClearJob("Timeout")
        regClearJob("Interval")

        variables["eval".js()] = null to "eval".func("script" defaults OpArgOmitted) {
            val script = it.argOrElse(0) { return@func Undefined }?.toKotlin(this).toString()
            JavaScriptEngine(this).compile(script).invoke(this)
        }
    }

    override suspend fun get(property: JsAny?): JsAny? {
        return when (property) {
            "Infinity".js() -> Double.POSITIVE_INFINITY.js()
            "NaN".js() -> Double.NaN.js()
            "undefined".js() -> Undefined
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
                || a is Undefined
                || a is Unit
                || a is CharSequence && a.isEmpty()
                || a is Number && a.toDouble().let { it == 0.0 || it.isNaN() }
                || (a as? Wrapper<*>)?.value?.let { isFalse(it) } == true
    }

    override suspend fun isComparable(a: JsAny?, b: JsAny?): Boolean {

        val ka = a?.toKotlin(this)
        val kb = b?.toKotlin(this)

        if (ka is Number && kb is CharSequence || kb is Number && ka is CharSequence){
            return isComparable(toNumber(a).js(), toNumber(b).js())
        }

        if (ka is Double && ka.isNaN() || kb is Double && kb.isNaN()) {
            return false
        }
        if (ka is Number && kb is Number || ka is CharSequence && kb is CharSequence) {
            return true
        }
        return false
    }

    override suspend fun compare(a: JsAny?, b: JsAny?): Int {

        val ka = a?.toKotlin(this)
        val kb = b?.toKotlin(this)

        return if (ka is Number || kb is Number) {
            toNumber(a).toDouble().compareTo(toNumber(b).toDouble())
        } else {
            ka.toString().compareTo(kb.toString())
        }
    }

    override suspend fun sum(a: JsAny?, b: JsAny?): JsAny? {
        var ta: Any? = a
        var tb: Any? = b

        if (ta is JSObject) {
            ta = a.ToPrimitive()
        }
        if (tb is JSObject) {
            tb = b.ToPrimitive()
        }

        if (ta is CharSequence || tb is CharSequence) {
            return (ta.ToString() + tb.ToString()).js()
        }

        val na = ta.ToNumber()
        val nb = tb.ToNumber()

        return when {
            na is Long && nb is Long -> (na + nb).js()
            else -> (na.toDouble() + nb.toDouble()).js()
        }
    }

    override suspend fun sub(a: JsAny?, b: JsAny?): JsAny? {
        return jssub(a?.numberOrThis(), b?.numberOrThis())
    }

    override suspend fun mul(a: JsAny?, b: JsAny?): JsAny? {
        return jsmul(a?.numberOrThis(), b?.numberOrThis())
    }

    override suspend fun div(a: JsAny?, b: JsAny?): JsAny? {
        return jsdiv(a?.numberOrThis(), b?.numberOrThis())
    }

    override suspend fun mod(a: JsAny?, b: JsAny?): JsAny? {
        return jsmod(a?.numberOrThis(), b?.numberOrThis(throwForObjects = true))
    }

    override suspend fun inc(a: JsAny?): JsAny? {
        return jsinc(a?.numberOrThis())
    }

    override suspend fun dec(a: JsAny?): JsAny? {
        return jsdec(a?.numberOrThis())
    }

    override suspend fun neg(a: JsAny?): JsAny? {
        return jsneg(a?.numberOrThis())
    }

    override suspend fun pos(a: JsAny?): JsAny? {
        return jspos(a?.numberOrThis())
    }

    override suspend fun toNumber(a: JsAny?): Number {
        return a.ToNumber()
    }

    private suspend fun JsAny.numberOrThis(
        withMagic : Boolean = true,
        throwForObjects : Boolean = false
    ) : Any = numberOrNull(withMagic, throwForObjects) ?: this

    private tailrec suspend fun Any?.numberOrNull(
        withNaNs : Boolean = true,
        throwForObjects : Boolean = false
    ) : Number? = when(this) {
        is Undefined -> null
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
            toPrimitive("number".js(), throwForObjects)?.numberOrNull(withNaNs, throwForObjects)
        } else {
            null
        }
        else -> null
    }



    private suspend fun JsAny.toNumericOrThis(): JsAny? {

        if (this is JSSymbol){
            typeError { "Symbol cannot be converted to a number".js() }
        }

        get("valueOf".js(), this@JSRuntime)?.callableOrNull()
            ?.call(this, emptyList(), this@JSRuntime)
            ?.takeIf { it !is JSObject }
            ?.let { return it }

        JSStringFunction.toString(this, this@JSRuntime)
            .toDoubleOrNull()
            .takeIf { it?.isNaN() != true }
            ?.let { return it.js()  }

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
            is JSSymbol -> typeError { "Symbol cannot be converted to a number".js() }
            is Unit, is Undefined -> return Double.NaN
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
            is JSSymbol -> typeError { "Symbol cannot be converted to a string".js() }
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
    internal tailrec suspend fun JsAny?.ToPrimitive(type: String? = S_DEFAULT) : JsAny? {

        return when  {
            this is JSObject -> {
                val exoticToPrim = get(JSSymbol.toPrimitive, this@JSRuntime)?.callableOrNull()
                if (exoticToPrim != null){
                    val hint = type ?: S_DEFAULT
                    val result = exoticToPrim.call(this, hint.js().listOf(), this@JSRuntime)
                    typeCheck(result !is JSObject){ "Cannot convert object to primitive value".js() }
                    return result
                }

                return OrdinaryToPrimitive(type ?: S_NUMBER)
            }
             this is Wrapper<*> && value is JsAny -> (value as JsAny).ToPrimitive(type)
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
    private suspend fun JsAny.OrdinaryToPrimitive(hint: String?) : JsAny? {
        val methods = mutableListOf<JsAny?>(S_VALUEOF.js(), S_TOSTRING.js())

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
            "Cannot convert object to primitive value".js()
        }
    }

//    private suspend fun JsAny.toPrimitiveSymbolOrThis(
//        type : JsAny? = "default".js(),
//    ) : JsAny {
//
//        get(JSSymbol.toPrimitive, this@JSRuntime)
//            ?.callableOrNull()
//            ?.call(this, listOfNotNull(type), this@JSRuntime)
//            ?.let {
//                typeCheck(it !is JSObject){
//                    "Cannot convert object to primitive value"
//                }
//                return it
//            }
//
//        return this
//    }

    internal suspend fun JsAny.toPrimitive(
        type : JsAny? = "default".js(),
        force : Boolean = false,
        symbolOnly : Boolean = false,
    ) : JsAny? {

        get(JSSymbol.toPrimitive, this@JSRuntime)
            ?.callableOrNull()
            ?.call(this, listOfNotNull(type), this@JSRuntime)
            ?.let {
                typeCheck(it !is JSObject){
                    "Cannot convert object to primitive value".js()
                }
                return it
            }

        return if (!symbolOnly) {
            if (this is JSSymbol){
                typeError { "Symbol cannot be converted to a number".js() }
            }

            get("valueOf".js(), this@JSRuntime)?.callableOrNull()
                ?.call(this, emptyList(), this@JSRuntime)
                ?.takeIf { it !is JSObject }
                ?.let { return it }

            JSStringFunction.toString(this, this@JSRuntime)
                .toDoubleOrNull()
                ?.takeUnless(Double::isNaN)
                ?.let { return it.js()  }

            return null
        } else {
            null
        }
    }

    private suspend fun registerJob(
        args: List<JsAny?>,
        block: suspend CoroutineScope.(Callable, Long) -> Unit
    ): JsAny {
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

        return jobId.js()
    }

    private fun regSetTimeout() {
        variables["setTimeout".js()] = null to "setTimeout".func("handler", "timeout") {
            (findRoot() as JSRuntime).registerJob(it) { handler, timeout ->
                delay(timeout)
                handler.invoke(emptyList(), this@func)
            }
        }
    }

    private fun regSetInterval() {
        variables["setInterval".js()] = null to "setInterval".func("handler", "interval") {
            (findRoot() as JSRuntime).registerJob(it) { handler, timeout ->
                while (isActive) {
                    handler.invoke(emptyList(), this@func)
                    delay(timeout)
                }
            }
        }
    }

    private fun regClearJob(what: String) {
        variables[ "clear$what".js()] =  null to  "clear$what".func("id") {
            val id = toNumber(it.getOrNull(0)).toLong()
            jobsMap[id]?.cancel()
            Undefined
        }
    }
}

private class RuntimeObject(val thisRef: () -> ScriptRuntime) : JSObject {

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {
        return if (thisRef().contains(property)){
            thisRef().get(property)?.get(runtime)
        } else {
            super.get(property, thisRef())
        }
    }

    override suspend fun set(
        property: JsAny?,
        value: JsAny?,
        runtime: ScriptRuntime
    ) {
        thisRef().set(property, value, null)
    }

    override suspend fun defineOwnProperty(
        property: JsAny?,
        value: JSPropertyAccessor,
        runtime: ScriptRuntime,
        enumerable: Boolean?,
        configurable: Boolean?,
        writable: Boolean?
    ) {
        set(property, value.get(runtime), runtime)
    }

    override suspend fun hasOwnProperty(name: JsAny?, runtime: ScriptRuntime): Boolean {
        return thisRef().contains(name)
    }

    override suspend fun contains(property: JsAny?, runtime: ScriptRuntime): Boolean {
        return thisRef().contains(property)
    }


    override suspend fun delete(property: JsAny?, runtime: ScriptRuntime): Boolean {

        thisRef().delete(property, true)
        return true
    }


}

internal inline fun <reified T > ScriptRuntime.thisRef() : T {
    return thisRef as T
}

private fun jssub(a : Any?, b : Any?) : JsAny? {
    return when {
        a is Unit || b is Unit -> Double.NaN
        a is Number && b is Unit || a is Unit && b is Number -> Double.NaN
        a is Long? && b is Long? -> (a ?: 0L) - (b ?: 0L)
        a is Double? && b is Double? ->(a ?: 0.0) - (b ?: 0.0)
        a is Number? && b is Number? ->(a?.toDouble() ?: 0.0) - (b?.toDouble() ?: 0.0)
        else -> Double.NaN
    }.js()
}

private fun jsmul(a : Any?, b : Any?) : JsAny {
    return when {
        a == Undefined || b == Undefined -> Double.NaN.js()
        a == null || b == null -> 0L.js()
        a is Long && b is Long -> (a*b).js()
        a is Double && b is Double -> (a*b).js()
        a is Long && b is Long -> (a * b).js()
        a is Number && b is Number -> (a.toDouble() * b.toDouble()).js()
        a is List<*> && b is Number -> {
            a as List<Number>
            val bf = b.toDouble()
            a.fastMap { (it.toDouble() * bf).js() }.js()
        }
        a is Number && b is List<*> -> {
            b as List<Number>
            val af = a.toDouble()
            b.fastMap { (it.toDouble() * af).js() }.js()
        }
        else -> Double.NaN.js()
    }
}

private fun jsdiv(a : Any?, b : Any?) : JsAny {
    return when {
        a is Undefined || b is Undefined ||
        (a == null && b == null)
                || ((a as? Number)?.toDouble() == 0.0 && b == null)
                || ((b as? Number)?.toDouble() == 0.0 && a == null)
                || ((a as? CharSequence)?.toString()?.toDoubleOrNull() == 0.0 && b == null)
                || ((b as? CharSequence)?.toString()?.toDoubleOrNull() == 0.0 && a == null) -> Double.NaN.js()

        a is Long && b is Long -> when {
            b == 0 -> a / b.toDouble()
            a % b == 0L -> a / b
            else -> a.toDouble() / b
        }.js()
        a is Number && b is Number -> (a.toDouble() / b.toDouble()).js()
        a == null -> 0L.js()
        b == null -> Double.POSITIVE_INFINITY.js()

        a is List<*> && b is Number -> {
            a as List<Number>
            val bf = b.toDouble()
            a.fastMap { (it.toDouble() / bf).js() }.js()
        }

        else -> Double.NaN.js()
    }
}

private fun jsmod(a : Any?, b : Any?) : JsAny {
    return when {
        b == null || a == Unit || b == Unit
                || (b as? Number)?.toDouble()?.absoluteValue?.let { it < Double.MIN_VALUE } == true -> Double.NaN.js()
        a == null -> 0.0.js()
//        a is Long && b is Long -> a % b
        a is Number && b is Number -> (a.toDouble() % b.toDouble()).js()
        else -> Double.NaN.js()
    }
}


private fun jsinc(v : Any?) : JsAny {
    return when (v) {
        null -> 1L
        is Long -> v + 1
        is Double -> v + 1
        is Number -> v.toDouble() + 1
        else -> Double.NaN
    }.js()
}

private fun jsdec(v : Any?) : JsAny {
    return when (v) {
        null -> -1L
        is Long -> v - 1
        is Double -> v - 1
        is Number -> v.toDouble() - 1
        else -> Double.NaN
    }.js()
}

private fun jsneg(v : Any?) : JsAny {
    return when (v) {
        null -> (-0).js()
        0, 0L -> (-0.0).js()
        is Long -> (-v).js()
        is Number -> (-v.toDouble()).js()
        is List<*> -> {
            v as List<Number>
            v.fastMap { (-it.toDouble()).js() }.js()
        }
        else -> Double.NaN.js()
    }
}

private fun jspos(v : Any?) : JsAny {
    return when (v) {
        null -> 0
        is Long -> +v
        is Number -> +v.toDouble()
        else -> Double.NaN
    }.js()
}

private const val S_NUMBER = "number"
private const val S_STRING = "string"
private const val S_DEFAULT = "default"
private const val S_VALUEOF = "valueOf"
private const val S_TOSTRING = "toString"